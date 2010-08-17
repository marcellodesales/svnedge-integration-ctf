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
public class ProfilingResponseHandler extends BasicHandler {
    /**
     * @see BasicHandler#invoke(MessageContext)
     */
    public void invoke(final MessageContext messageContext) throws AxisFault {
        SoapRecord.end(false, messageContext);
    }
}
