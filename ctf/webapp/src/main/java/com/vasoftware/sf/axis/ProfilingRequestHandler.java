/*
 * CollabNet TeamForge
 * Copyright 2010 CollabNet, Inc.  All rights reserved.
 * http://www.collab.net
 */
package com.vasoftware.sf.axis;

import org.apache.axis.AxisFault;
import org.apache.axis.MessageContext;
import org.apache.axis.handlers.BasicHandler;

import com.vasoftware.sf.common.profiler.SoapRecord;

/**
 * The <code>ProfilingResponseHandler</code> adds support for profiling Axis responses.
 */
@SuppressWarnings("serial")
public class ProfilingRequestHandler extends BasicHandler {
    /**
     * Mark the beginning of a soap request.
     * 
     * @param messageContext
     *            The message context being invoked.
     * @throws AxisFault
     *             Never thrown.
     */
    public void invoke(final MessageContext messageContext) throws AxisFault {
        SoapRecord.start(messageContext);
    }

    /**
     * @see ProfilingResponseHandler#onFault(MessageContext)
     */
    @Override
    public void onFault(final MessageContext messageContext) {
        SoapRecord.end(true, messageContext);
    }
}
