/*
 * CollabNet TeamForge
 * Copyright 2010 CollabNet, Inc.  All rights reserved.
 * http://www.collab.net
 */
package com.vasoftware.sf.common.profiler;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.soap.SOAPException;

import org.apache.axis.MessageContext;
import org.apache.axis.description.JavaServiceDesc;
import org.apache.axis.description.OperationDesc;
import org.apache.axis.description.ParameterDesc;
import org.apache.axis.description.ServiceDesc;
import org.apache.axis.handlers.soap.SOAPService;
import org.apache.axis.message.RPCElement;
import org.w3c.dom.NodeList;

import com.vasoftware.sf.common.logger.Logger;
import com.vasoftware.sf.common.util.StringUtil;

/**
 * The <code>SoapRecord</code> is used to record SOAP profiler information.
 */
@SuppressWarnings("unchecked")
public class SoapRecord {

    private static final Logger smLogger = Logger.getLogger(SoapRecord.class);
    /** a count of how many pages are currently being actively processed */
    private static int smCurrentActivePageCount = 0;
    /** Synchronization lock for the counter. */
    private static Object smCounterLock = new Object();
    /** How many milliseconds before a call is considered "long" */
    private static final int LONG_CALL_DURATION = 4000;
    /** Maximum number of soap request records to keep around */
    private static final int MAX_ACTVITY_LOG_COUNT = 1000;
    /** Session activity records. */
    private static LinkedList smActivityRecords = new LinkedList();
    /** This stores the current soap request record being tracked */
    private static final ThreadLocal SOAP_RECORD = new ThreadLocal();
    /** The soap request profile object. */
    private final SoapRequestProfile mSoapRequest;
    private static final String UNKNOWN_OPERATION_NAME = "Unknown";

    /**
     * Log the start of a new soap request only if debugging is enabled.
     * 
     * @param messageContext
     *            The soap message context.
     */
    public static void start(final MessageContext messageContext) {
        if (smLogger.isInfoEnabled()) {
            Class soapClass = null;
            String operationName = null;
            String sessionId = null;
            String userName = null;

            if (messageContext != null) {
                final SOAPService service = messageContext.getService();
                if (service != null) {
                    final ServiceDesc serviceDescription = service.getServiceDescription();
                    if (serviceDescription instanceof JavaServiceDesc) {
                        soapClass = ((JavaServiceDesc) serviceDescription).getImplClass();
                    }
                }

                operationName = getOperationNameFromMessageContext(messageContext);
                if (operationName != null) {
                    sessionId = getParameterValue(messageContext, "sessionId");
                }
            }
            if (operationName != null && operationName.startsWith("login")) {
                userName = getParameterValue(messageContext, "userName");
            }

            new SoapRecord(soapClass, operationName, sessionId, userName);
        }
    }

    /**
     * Given a message context, extract the operation name (if one can be found) of the method being invoked.
     * 
     * @param messageContext
     *            The message context sent to the request handler's invoke method.
     * @return The operation name to use for profiling.
     */
    private static String getOperationNameFromMessageContext(final MessageContext messageContext) {
        String operationName = UNKNOWN_OPERATION_NAME;
        final OperationDesc operation = messageContext.getOperation();

        if (operation == null) {
            // This only happens for overloaded methods or if the method does not exist
            String possibleOperationName = null;
            try {
                final Iterator i = messageContext.getCurrentMessage().getSOAPEnvelope().getBody().getChildElements();
                while (i.hasNext()) {
                    final Object nextElement = i.next();
                    if (nextElement instanceof RPCElement) {
                        final RPCElement operationElement = (RPCElement) nextElement;
                        possibleOperationName = operationElement.getMethodName();
                        break;
                    }
                }
            } catch (final Throwable e) {
                ; // do nothing. We will use the unknown operation name
            }

            if (!StringUtil.isEmpty(possibleOperationName)) {
                operationName = possibleOperationName;
            }
        } else {
            final List parameterValueList = new ArrayList();
            for (int i = 1; i < operation.getNumInParams(); i++) {
                try {
                    final NodeList nodes = messageContext.getCurrentMessage().getSOAPBody()
                                                         .getElementsByTagName("arg" + i);
                    final String parameterValue = nodes.item(0).getChildNodes().item(0).getNodeValue();
                    if (!StringUtil.isEmpty(parameterValue)) {
                        parameterValueList.add(parameterValue.substring(0, 10));
                    }
                } catch (final Exception ex) {
                    // do nothing
                }
            }
            operationName = operation.getName() + "("
                    + StringUtil.join((String[]) parameterValueList.toArray(new String[] {}), ",") + ")";
        }

        return operationName;
    }

    /**
     * Get operation parameter value
     * 
     * @param messageContext
     *            message context
     * @param parameterName
     *            parameter name to get value
     * @return parameter value, null if failed
     */
    private static String getParameterValue(final MessageContext messageContext, final String parameterName) {
        if (messageContext == null || messageContext.getOperation() == null) {
            return null;
        }
        final ParameterDesc parameterDesc = messageContext.getOperation().getParamByQName(new QName(parameterName));
        if (parameterDesc != null) {
            final int i = parameterDesc.getOrder();
            try {
                final NodeList nodes = messageContext.getCurrentMessage().getSOAPBody().getElementsByTagName("arg" + i);
                return nodes.item(0).getChildNodes().item(0).getNodeValue();
            } catch (final Exception ex) {
                // do nothing
            }
        }
        return null;
    }

    /**
     * End the current soap request only if logging is enabled.
     * 
     * @param fault
     *            true if the soap request ended due to a fault.
     * @param messageContext
     *            the message context.
     */
    public static void end(final boolean fault, final MessageContext messageContext) {
        if (smLogger.isInfoEnabled()) {
            final SoapRecord record = (SoapRecord) SOAP_RECORD.get();

            if (record == null) {
                smLogger.debug("Ending unknown soap call.");
            } else {
                record.endRequest(fault, messageContext);
            }
        }
    }

    /**
     * Get the current soap record if one is present (null if there is none).
     * 
     * @return The current soap record on the thread.
     */
    public static SoapRecord getSoapRecord() {
        return (SoapRecord) SOAP_RECORD.get();
    }

    /**
     * Get the id of the current soap record or -1 if none is present.
     * 
     * @return The id of the current soap record or -1 if none is present.
     */
    public static long getCurrentId() {
        final SoapRecord record = (SoapRecord) SOAP_RECORD.get();
        if (record == null) {
            return -1;
        } else {
            return record.getId();
        }
    }

    /**
     * Get the list of soap requests.
     * 
     * @return The list of recent soap requests.
     */
    public static List getSoapActivityList() {
        synchronized (smCounterLock) {
            return new ArrayList(smActivityRecords);
        }
    }

    /**
     * Clear out all existing soap records.
     */
    public static void clearSoapRecords() {
        synchronized (smCounterLock) {
            smActivityRecords.clear();
        }
    }

    /**
     * public constructor for a PageRecord, which encapsulates simple historical information about the production of a
     * particular web page.
     * 
     * @param serviceClass
     *            The class of the service being invoked
     * @param methodName
     *            The name of the method being invoked.
     * @param sessionId
     *            The id of the session.
     * @param userName
     *            the username if available.
     */
    private SoapRecord(final Class serviceClass, final String methodName, final String sessionId, final String userName) {
        mSoapRequest = new SoapRequestProfile(serviceClass, methodName);
        mSoapRequest.setSessionId(sessionId);
        if (!StringUtil.isEmpty(userName)) {
            mSoapRequest.setUserName(userName);
        }
        SOAP_RECORD.set(this);
        synchronized (smCounterLock) {
            mSoapRequest.setStartingActiveSoapRequests(++smCurrentActivePageCount);
            smActivityRecords.add(this);
            if (smActivityRecords.size() > MAX_ACTVITY_LOG_COUNT) {
                smActivityRecords.removeFirst();
            }
        }

        logRequestStart(mSoapRequest);
    }

    /**
     * Marks the end of page request.
     * 
     * @param fault
     *            Is error encountered in processing request?
     * @param messageContext
     *            The message context.
     */
    public void endRequest(final boolean fault, final MessageContext messageContext) {
        if (mSoapRequest.getIsComplete()) {
            return;
        }

        try {
            final NodeList loginResult = messageContext.getCurrentMessage().getSOAPBody()
                                                       .getElementsByTagName("loginReturn");
            if (loginResult != null && loginResult.getLength() > 0) {
                mSoapRequest.setSessionId(loginResult.item(0).getChildNodes().item(0).getNodeValue());
            }
        } catch (final SOAPException ex) {
            // never mind
        }
        mSoapRequest.setFaultCondition(fault);

        synchronized (smCounterLock) {
            mSoapRequest.setEndingActiveSoapRequests(smCurrentActivePageCount--);
        }

        // End the profile
        mSoapRequest.end();
        logRequestCompletion(mSoapRequest);

        SOAP_RECORD.set(null);
    }

    /**
     * Log the fact that a soap request was made.
     * 
     * @param requestProfile
     *            The request profile.
     */
    private void logRequestStart(final SoapRequestProfile requestProfile) {
        if (requestProfile == null || !smLogger.isDebugEnabled()) {
            return;
        }
        if (smLogger.isDebugEnabled()) {
            final StringBuilder callStart = new StringBuilder();
            callStart.append("SOAP CALL START: ID[" + requestProfile.getId() + "] " + "service["
                    + requestProfile.getServiceImplClass().getName() + "] " + "operation["
                    + requestProfile.getOperationName() + "] ");
            if (requestProfile.getUserName() != null) {
                callStart.append("username[" + requestProfile.getUserName() + "] ");
            }
            callStart.append("sessionId[" + requestProfile.getSessionId() + "] " + "active["
                    + requestProfile.getStartingActiveSoapRequests() + "] " + "memoryAllocated["
                    + requestProfile.getStartingAllocatedMemory() + "] " + "memoryUsed["
                    + requestProfile.getStartingMemoryUse() + "]");
            smLogger.debug(callStart.toString());
        }
    }

    /**
     * Mark the end of a soap request on the current requestProfile object.
     * 
     * @param requestProfile
     *            The request profile being used.
     */
    private void logRequestCompletion(final SoapRequestProfile requestProfile) {
        if (requestProfile == null || !smLogger.isInfoEnabled()) {
            return;
        }

        final long duration = requestProfile.getDuration();

        final String faultString = requestProfile.isFaultCondition() ? " (FAULT)" : "";
        final String longString = duration >= LONG_CALL_DURATION ? " (LONG)" : "";
        final boolean logInfo = duration > SoapRequestProfile.getSoapThreshold();
        if (smLogger.isDebugEnabled() || (smLogger.isInfoEnabled() && logInfo)) {
            final StringBuilder callEnd = new StringBuilder();
            callEnd.append("SOAP CALL END: ID[" + requestProfile.getId() + "] " + "service["
                    + requestProfile.getServiceImplClass().getName() + "] " + "operation["
                    + requestProfile.getOperationName() + "] ");
            if (requestProfile.getUserName() != null) {
                callEnd.append("username[" + requestProfile.getUserName() + "] ");
            }
            callEnd.append("sessionId[" + requestProfile.getSessionId() + "] " + "duration[" + duration + "] "
                    + "active[" + requestProfile.getEndingActiveSoapRequests() + "] " + "memoryAllocated["
                    + requestProfile.getEndingAllocatedMemory() + "] " + "memoryUsed["
                    + requestProfile.getEndingMemoryUse() + "]" + faultString + longString);
            if (logInfo) {
                // if duration is longer than threshold, log this as info regardless of default log level
                smLogger.info(callEnd.toString());
            } else if (smLogger.isDebugEnabled()) {
                smLogger.debug(callEnd.toString());
            }
        }
    }

    /**
     * Get the soap request profile object that was stored in this record.
     * 
     * @return The soap request profile object.
     */
    public SoapRequestProfile getSoapRequestProfile() {
        return mSoapRequest;
    }

    /**
     * @see SoapRequestProfile#getId()
     */
    public long getId() {
        return mSoapRequest.getId();
    }

    /**
     * @see SoapRequestProfile#getServiceImplClass()
     */
    public Class getServiceImpl() {
        return mSoapRequest.getServiceImplClass();
    }

    /**
     * @see SoapRequestProfile#getOperationName()
     */
    public String getOperationName() {
        return mSoapRequest.getOperationName();
    }

    /**
     * @see SoapRequestProfile#getDuration()
     */
    public long getDuration() {
        return mSoapRequest.getDuration();
    }

    /**
     * @see SoapRequestProfile#getStartingActiveSoapRequests()
     */
    public int getStartingActiveSoapRequests() {
        return mSoapRequest.getStartingActiveSoapRequests();
    }

    /**
     * @see SoapRequestProfile#getEndingActiveSoapRequests()
     */
    public int getEndingActiveSoapRequests() {
        return mSoapRequest.getEndingActiveSoapRequests();
    }

    /**
     * Get the memory use when the request was started.
     * 
     * @return The memory use when the request was started.
     */
    public long getStartingMemory() {
        return mSoapRequest.getStartingMemoryUse();
    }

    /**
     * Get the memory use when the request completed.
     * 
     * @return The memory use when the request completed.
     */
    public long getEndingMemory() {
        return mSoapRequest.getEndingMemoryUse();
    }

    /**
     * Returns formatted start time.
     * 
     * @return Formatted start time string.
     */
    public String getFormattedStartTime() {
        return PageRecord.smDateFormat.format(new Date(mSoapRequest.getStartTime()));
    }
}
