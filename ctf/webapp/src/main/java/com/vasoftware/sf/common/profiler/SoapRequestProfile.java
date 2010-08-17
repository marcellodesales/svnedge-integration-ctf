/*
 * CollabNet TeamForge
 * Copyright 2010 CollabNet, Inc.  All rights reserved.
 * http://www.collab.net
 */
package com.vasoftware.sf.common.profiler;

/**
 * The <code>SoapRequestProfile</code> is used to profile SOAP requests.
 */
@SuppressWarnings("unchecked")
public class SoapRequestProfile extends ProfileObject {
    /** The type string for the profile object. */
    public static final String TYPE = "soap-request";

    private boolean mFaultCondition;
    private Class mServiceImplClass;
    private String mOperationName;
    private int mStartingActiveSoapRequests;
    private int mEndingActiveSoapRequests;
    private String mSessionId;
    private String mUserName;

    /**
     * Basic constructor. Just calls through to super().
     * 
     * @param serviceClass
     *            The service class being invoked.
     * @param operationName
     *            The name of the operation being called.
     */
    public SoapRequestProfile(Class serviceClass, final String operationName) {
        super();

        if (serviceClass == null) {
            serviceClass = UnknownSoapService.class;
        }

        mServiceImplClass = serviceClass;
        mOperationName = operationName;
    }

    /**
     * @see ProfileObject#getType()
     */
    @Override
    public String getType() {
        return TYPE;
    }

    /**
     * Get the name of the service implementation class.
     * 
     * @return The name of the service implementation class.
     */
    public Class getServiceImplClass() {
        return mServiceImplClass;
    }

    /**
     * Set the class that is being invoked via soap.
     * 
     * @param serviceImplClass
     *            The class that is being invoked via soap.
     */
    public void setServiceImplClass(final Class serviceImplClass) {
        mServiceImplClass = serviceImplClass;
    }

    /**
     * Get the name of the operation being invoked over soap.
     * 
     * @return The name of the operation that was invoked over soap.
     */
    public String getOperationName() {
        return mOperationName;
    }

    /**
     * Set the name of the operation being invoked over soap.
     * 
     * @param operationName
     *            The name of the operation being invoked over soap.
     */
    public void setOperationName(final String operationName) {
        mOperationName = operationName;
    }

    /**
     * Get a flag indicating whether or not the soap request ended in a fault.
     * 
     * @return true if the soap request ended in a fault.
     */
    public boolean isFaultCondition() {
        return mFaultCondition;
    }

    /**
     * Set a flag indicating whether or not the soap request ended in a fault.
     * 
     * @param faultCondition
     *            true if the soap request ended in a fault.
     */
    public void setFaultCondition(final boolean faultCondition) {
        mFaultCondition = faultCondition;
    }

    /**
     * Get the number of active soap requests being processed when this call started.
     * 
     * @return The number of active soap requests being processed at the time this call was started.
     */
    public int getStartingActiveSoapRequests() {
        return mStartingActiveSoapRequests;
    }

    /**
     * Set the number of active soap requests being processed when this call started.
     * 
     * @param startingActiveSoapRequests
     *            The number of active soap requests being processed at the time this call was started.
     */
    public void setStartingActiveSoapRequests(final int startingActiveSoapRequests) {
        mStartingActiveSoapRequests = startingActiveSoapRequests;
    }

    /**
     * Get the number of active soap requests being processed when this call ended.
     * 
     * @return The number of active soap requests being processed at the time this call was ended.
     */
    public int getEndingActiveSoapRequests() {
        return mEndingActiveSoapRequests;
    }

    /**
     * Set the number of active soap requests being processed when this call ended.
     * 
     * @param endingActiveSoapRequests
     *            The number of active soap requests being processed at the time this call was ended.
     */
    public void setEndingActiveSoapRequests(final int endingActiveSoapRequests) {
        mEndingActiveSoapRequests = endingActiveSoapRequests;
    }

    /**
     * Get session id.
     * 
     * @return session id.
     */
    public String getSessionId() {
        return mSessionId;
    }

    /**
     * Set session id.
     * 
     * @param sessionId
     *            session id.
     */
    public void setSessionId(final String sessionId) {
        mSessionId = sessionId;
    }

    /**
     * Get username.
     * 
     * @return username.
     */
    public String getUserName() {
        return mUserName;
    }

    /**
     * Set username.
     * 
     * @param userName
     *            username.
     */
    public void setUserName(final String userName) {
        mUserName = userName;
    }

    /**
     * @see ProfileObject#appendCustomDataXml(StringBuffer)
     */
    @Override
    protected void appendCustomDataXml(final StringBuffer buffer) {
        buffer.append("  <service_impl>").append(mServiceImplClass.getName()).append("</serviceImpl>\n");
        buffer.append("  <operation_name>").append(mOperationName).append("</operation_name>\n");
        buffer.append("  <fault_condition>").append(mFaultCondition).append("</fault_condition>\n");

        buffer.append("  <starting_active_soap_requests>");
        buffer.append(getStartingActiveSoapRequests());
        buffer.append("</starting_active_soap_requests>\n");

        buffer.append("  <ending_active_soap_requests>");
        buffer.append(getEndingActiveSoapRequests());
        buffer.append("</ending_active_soap_requests>\n");
    }

    /**
     * @see ProfileObject#appendCustomDataHtml(StringBuffer)
     */
    @Override
    protected void appendCustomDataHtml(final StringBuffer buffer) {
        buffer.append("<strong>Ended with Fault:</strong>&nbsp;").append(mFaultCondition).append("<br/>\n");

        buffer.append("</td>\n<td style=\"").append(QueryProfile.STYLE).append("\">");
        buffer.append("<strong>Service Class:</strong>&nbsp;").append(mServiceImplClass.getName()).append("<br/>\n");
        buffer.append("<strong>Operation Name:</strong>&nbsp;").append(mOperationName).append("<br/>\n");
        buffer.append("<strong>Active Soap Requests (start/end):</strong>&nbsp;");

        buffer.append(getStartingActiveSoapRequests());
        buffer.append("/");
        buffer.append(getEndingActiveSoapRequests()).append("<br/>\n");
    }

    /**
     * This class is used if no service class was passed in.
     */
    public class UnknownSoapService {
        /**
         * Private constructor for dummy unknown soap service class.
         */
        private UnknownSoapService() {
            ; // do nothing but don't let people use this
        }
    }
}
