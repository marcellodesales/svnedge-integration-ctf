/*
 * CollabNet TeamForge
 * Copyright 2010 CollabNet, Inc.  All rights reserved.
 * http://www.collab.net
 */

package com.vasoftware.sf.common.profiler;

import java.util.Iterator;
import java.util.Map;

/**
 * The <code>QueryProfile</code> class is used for profiling query requests.
 */
@SuppressWarnings("unchecked")
public class QueryProfile extends ProfileObject {
    /** The type string for the profile object. */
    public static final String TYPE = "query";

    public static final String STYLE = "white-space: nowrap; vertical-align: top; text-align: left; "
            + "border-left: 1px solid #999; padding: 3px;";

    private final String mQueryId;
    private final String mQuery;
    private final Map mBindVariables;

    /**
     * Constructor for a query profile object.
     * 
     * @param queryId
     *            The id of the query that was executed. Prepared queries can be used multiple times and have the same
     *            id.
     * @param query
     *            The query that that is being executed.
     * @param bindVariables
     *            The bind variables associated with the query.
     */
    public QueryProfile(final String queryId, final String query, final Map bindVariables) {
        super();

        mQueryId = queryId;
        mQuery = query;
        mBindVariables = bindVariables;
    }

    /**
     * Get the query id.
     * 
     * @return The query id.
     */
    public String getQueryId() {
        return mQueryId;
    }

    /**
     * Get the query that was executed.
     * 
     * @return The query that was executed.
     */
    public String getQuery() {
        return mQuery;
    }

    /**
     * Get the bind variables that were used in the query.
     * 
     * @return The map of bind variables.
     */
    public Map getBindVariables() {
        return mBindVariables;
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
        buffer.append("  <query_id>").append(getQueryId()).append("</query_id>\n");
        buffer.append("  <query><![CDATA[").append(getQuery()).append("]]></query>\n");

        // Append the bind variables
        buffer.append("  <bind_variables>\n");
        for (final Iterator iterator = mBindVariables.entrySet().iterator(); iterator.hasNext();) {
            final Map.Entry bindEntry = (Map.Entry) iterator.next();
            buffer.append("    <variable id=\"").append(bindEntry.getKey()).append("\"><![CDATA[");
            buffer.append(bindEntry.getValue());
            buffer.append("]]></variable>\n");
        }
        buffer.append("  </bind_variables>\n");
    }

    /**
     * @see ProfileObject#appendCustomDataHtml(StringBuffer)
     */
    @Override
    protected void appendCustomDataHtml(final StringBuffer buffer) {
        buffer.append("<strong>Query Id:</strong>&nbsp;").append(getQueryId()).append("<br/>\n");
        buffer.append("<strong>Bind Variables:</strong><br/><div style=\"padding-left: 10px;\">");
        for (final Iterator iterator = mBindVariables.entrySet().iterator(); iterator.hasNext();) {
            final Map.Entry bindEntry = (Map.Entry) iterator.next();
            buffer.append(bindEntry.getKey()).append(": ").append(bindEntry.getValue()).append("<br/>");
        }

        buffer.append("</div></td>\n<td style=\"").append(STYLE).append("\">");
        buffer.append("<div style=\"overflow: auto; white-space: pre; min-width: 400px\">");

        if (getPageId() >= 0) {
            if (PageRecord.isDevelopementEnvironment()) {
                buffer.append("<a href=\"/sf/devtools/do/viewQuery?pageId=");
                buffer.append(getPageId());
                buffer.append("&queryId=").append(getId());
                buffer.append("\">Run</a><br/>\n");
            }
        }

        buffer.append(getQuery());
        buffer.append("</div>");
    }
}
