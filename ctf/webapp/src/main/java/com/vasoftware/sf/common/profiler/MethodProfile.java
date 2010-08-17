/*
 * CollabNet TeamForge
 * Copyright 2010 CollabNet, Inc.  All rights reserved.
 * http://www.collab.net
 */

package com.vasoftware.sf.common.profiler;

/**
 * Object used for profiling method executions.
 */
@SuppressWarnings("unchecked")
public class MethodProfile extends ProfileObject {
    /** The type string for the profile object. */
    public static final String TYPE = "method";

    private final Class mMethodClass;
    private final String mMethodName;
    private final Class[] mSignature;
    private final String mDescription;

    /**
     * Create an object to represent a method call.
     * 
     * @param clazz
     *            The class being called.
     * @param methodName
     *            The name of the method that was called.
     * @param signature
     *            The signature of the method.
     * @param description
     *            A description of the method being profiled.
     */
    public MethodProfile(final Class clazz, final String methodName, final Class[] signature, final String description) {
        super();

        mMethodClass = clazz;
        mMethodName = methodName;
        mSignature = signature;
        mDescription = description;
    }

    /**
     * Get the class where the method is located.
     * 
     * @return The class that contains the called method.
     */
    public Class getMethodClass() {
        return mMethodClass;
    }

    /**
     * Get the name of the method that was called.
     * 
     * @return The name of the method that was called.
     */
    public String getMethodName() {
        return mMethodName;
    }

    /**
     * Get the method signature.
     * 
     * @return The method's signature.
     */
    public Class[] getSignature() {
        return mSignature;
    }

    /**
     * Get a description of the method being profiled.
     * 
     * @return The method description.
     */
    public String getDescription() {
        return mDescription;
    }

    /**
     * @see ProfileObject#getType()
     */
    @Override
    public String getType() {
        return TYPE;
    }

    /**
     * @see ProfileObject#appendCustomDataXml(StringBuffer)
     */
    @Override
    protected void appendCustomDataXml(final StringBuffer buffer) {
        buffer.append("  <method_class>").append(getMethodClass().getName()).append("</method_class>\n");
        buffer.append("  <method_name>").append(getMethodName()).append("</method_name>\n");
        buffer.append("  <description>").append(getDescription()).append("</description>\n");

        buffer.append("  <signature>\n");
        if (mSignature != null) {
            for (int i = 0; i < mSignature.length; i++) {
                buffer.append("    <method-parameter>").append(mSignature[i].getName()).append("</method-parameter>\n");
            }
        }
        buffer.append("  </signature>\n");
    }

    /**
     * @see ProfileObject#appendCustomDataHtml(StringBuffer)
     */
    @Override
    protected void appendCustomDataHtml(final StringBuffer buffer) {
        buffer.append("<strong>Method Execution</strong>");

        buffer.append("</td>\n<td style=\"").append(QueryProfile.STYLE).append("\">");
        buffer.append("<strong>Method Class:</strong>&nbsp;").append(getMethodClass().getName()).append("<br/>\n");
        buffer.append("<strong>Method Name:</strong>&nbsp;").append(getMethodName()).append("(");
        if (mSignature != null) {
            for (int i = 0; i < mSignature.length; i++) {
                buffer.append(mSignature[i].getName());
                if (i + 1 < mSignature.length) {
                    buffer.append(", ");
                }
            }
        }
        buffer.append(")<br/>\n");
        buffer.append("<strong>Description:</strong>&nbsp;").append(getDescription()).append("<br/>\n");
    }
}
