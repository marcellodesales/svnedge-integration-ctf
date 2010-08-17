/*
 * CollabNet TeamForge
 * Copyright 2010 CollabNet, Inc.  All rights reserved.
 * http://www.collab.net
 */

package com.vasoftware.sf.common;

import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.StringTokenizer;

import javax.xml.namespace.QName;
import javax.xml.rpc.ServiceException;

import org.apache.axis.EngineConfiguration;
import org.apache.axis.client.Call;
import org.apache.axis.client.Service;
import org.apache.axis.configuration.BasicClientConfig;

/**
 * The <code>SoapClientHelper</code> class provides helper methods for SOAP.
 */
public class SoapClientHelper {
    /* The SOAP service url */
    private final URL mServiceUrl;

    /* The SOAP service name */
    private final String mServiceName;

    /* The SOAP service handle */
    private final Service mService;

    /* The timeout duration */
    private static final Integer DEFAULT_TIMEOUT = new Integer(Integer.MAX_VALUE);

    /**
     * Constructor with information on the remote SOAP service URL.
     * 
     * @param serviceUrl
     *            SOAP service URL.
     * @throws MalformedURLException
     *             Thrown when the specified URL is malfored.
     */
    public SoapClientHelper(final String serviceUrl) throws MalformedURLException {
        mServiceUrl = new URL(serviceUrl);
        final StringTokenizer urlTokens = new StringTokenizer(serviceUrl, "/");
        String urlToken = null;
        while (urlTokens.hasMoreTokens()) {
            urlToken = urlTokens.nextToken();
        }
        mServiceName = urlToken;
        if (mServiceName == null) {
            throw new MalformedURLException(serviceUrl);
        }

        final EngineConfiguration config = new BasicClientConfig();

        /**
         * Uncomment below to allow SSL connections to untrusted servers
         * 
         * AxisProperties.setProperty("org.apache.axis.components.net.SecureSocketFactory",
         * "org.apache.axis.components.net.SunFakeTrustSocketFactory");
         */

        mService = new Service(config);
    }

    /**
     * Invokes a service method with the specified parameters.
     * 
     * @param methodName
     *            Service method name.
     * @param params
     *            Service method parameters.
     * @return Return value from the SOAP service call.
     * @throws ServiceException
     *             See org.apache.axis.client.Service#createCall.
     * @throws RemoteException
     *             See org.apache.axis.client.Call#invoke
     */
    public Object invoke(final String methodName, final Object params[]) throws ServiceException, RemoteException {
        return invoke(methodName, params, DEFAULT_TIMEOUT);
    }

    /**
     * Invokes a service method with the specified parameters.
     * 
     * @param methodName
     *            Service method name.
     * @param params
     *            Service method parameters.
     * @param timeout
     *            how long before we timeout this connection.
     * @return Return value from the SOAP service call.
     * @throws ServiceException
     *             See org.apache.axis.client.Service#createCall.
     * @throws RemoteException
     *             See org.apache.axis.client.Call#invoke
     */
    public Object invoke(final String methodName, final Object params[], final Integer timeout)
                                                                                               throws ServiceException,
                                                                                               RemoteException {
        final Call call = (Call) mService.createCall();

        call.setTimeout(timeout);
        call.setTargetEndpointAddress(mServiceUrl);
        call.setOperationName(new QName(mServiceName, methodName));

        // Uncomment code below to get full stacktrace of what happens during Axis call
        // try {
        return call.invoke(params);
        // } catch (RemoteException e) {
        // System. out.println("======== Exception in Axis call ========");
        // e.printStackTrace();
        // throw e;
        // }
    }
}
