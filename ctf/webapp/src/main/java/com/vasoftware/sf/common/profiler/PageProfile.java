/*
 * CollabNet TeamForge
 * Copyright 2010 CollabNet, Inc.  All rights reserved.
 * http://www.collab.net
 */
package com.vasoftware.sf.common.profiler;

import com.vasoftware.sf.common.SfSystemException;

/**
 * The <code>PageProfile</code> class is used to profile page requests.
 */
public class PageProfile extends ProfileObject {
    /** The type string for the profile object. */
    public static final String TYPE = "page";

    private static final String[] STATUS_NAMES = new String[] { "active", "finished", "error" };

    /** Request still active. */
    public static final int ACTIVE = 0;
    /** Request finished. */
    public static final int FINISHED = 1;
    /** Error processing request. */
    public static final int ERROR = 2;

    private int mStatus;
    private String mUri;
    private String mSessionId;
    private String mUsername;
    private final String mUserAgent;
    private long mDownloadDuration = 0;
    private long mUploadDuration = 0;
    private int mStartingActivePageRequests = 0;
    private int mEndingActivePageRequests = 0;

    /**
     * Profile object for a web page hit by a user.
     * 
     * @param uri
     *            the URI for this page
     * @param sessionId
     *            Session ID.
     * @param username
     *            User name.
     * @param userAgent
     *            The user agent that hit the page.
     */
    public PageProfile(final String uri, final String sessionId, final String username, final String userAgent) {
        super();

        mStatus = ACTIVE;
        mUri = uri;
        mSessionId = sessionId;
        mUsername = username;
        mUserAgent = userAgent;
    }

    /**
     * Mark the end of the page hit.
     * 
     * @param sessionId
     *            The session id.
     * @param username
     *            The username.
     * @param exception
     *            true if the page ended due to an exception.
     * @return true if the page has not already had the end() method called.
     */
    public boolean end(final String sessionId, final String username, final boolean exception) {
        if (mStatus != ACTIVE) {
            mUri += ":!";
            return false;
        }

        if (mSessionId == null) {
            mSessionId = sessionId;
        }

        if (mUsername == null) {
            mUsername = username;
        }

        mStatus = exception ? ERROR : FINISHED;
        end();

        return true;
    }

    /**
     * Make sure that the page was cleaned up the correct way.
     */
    @Override
    protected void cleanup() {
        super.cleanup();

        if (mStatus == ACTIVE) {
            throw new SfSystemException("Use end(String, String, boolean) method on PageProfile to end.");
        }
    }

    /**
     * Append a string to the uri. This is used in PageRecord when a page is listed as closing more than once.
     * 
     * @param string
     *            The string to append to the URI as a warning.
     */
    public void appendToUri(final String string) {
        mUri += string;
    }

    /**
     * Get the uri of the page that was hit.
     * 
     * @return The uri of the profiled page.
     */
    public String getUri() {
        return mUri;
    }

    /**
     * Get the session id of the page hit.
     * 
     * @return The session id of the page hit.
     */
    public String getSessionId() {
        return mSessionId;
    }

    /**
     * Set the session id of the page hit.
     * 
     * @param sessionId
     *            The session id of the page hit.
     */
    public void setSessionId(final String sessionId) {
        mSessionId = sessionId;
    }

    /**
     * Get the name of the user that hit the page.
     * 
     * @return The name of the user that hit the page.
     */
    public String getUsername() {
        return mUsername;
    }

    /**
     * Set the username that was hitting the page.
     * 
     * @param username
     *            The username of the user that hit the page.
     */
    public void setUsername(final String username) {
        mUsername = username;
    }

    /**
     * Get the user agent that was responsible for the page hit.
     * 
     * @return The user agent that requested the page.
     */
    public String getUserAgent() {
        return mUserAgent;
    }

    /**
     * Get the status of the page.
     * 
     * @return The status of the page.
     */
    public int getStatus() {
        return mStatus;
    }

    /**
     * Set the ending page status.
     * 
     * @param status
     *            The ending page status.
     */
    public void setStatus(final int status) {
        mStatus = status;
    }

    /**
     * Get the amount of time a page spent on downloads.
     * 
     * @return The amount of time a page spent on a download.
     */
    public long getDownloadDuration() {
        return mDownloadDuration;
    }

    /**
     * Set the amount of time a page spent in a file download.
     * 
     * @param duration
     *            The amount of time (milliseconds) that a file download took.
     */
    public void setDownloadDuration(final long duration) {
        mDownloadDuration = duration;
    }

    /**
     * Get the amount of time the request took to upload.
     * 
     * @return The total request upload time.
     */
    public long getUploadDuration() {
        return mUploadDuration;
    }

    /**
     * Set the amount of time the request took to upload.
     * 
     * @param duration
     *            The total request upload time.
     */
    public void setUploadDuration(final long duration) {
        mUploadDuration = duration;
    }

    /**
     * Get the number of active page requests when the page started.
     * 
     * @return The number of active page requests when the page started.
     */
    public int getStartingActivePageRequests() {
        return mStartingActivePageRequests;
    }

    /**
     * Set the number of active page requests when the page started.
     * 
     * @param startingActivePageRequests
     *            The number of active page requests when the page started.
     */
    public void setStartingActivePageRequests(final int startingActivePageRequests) {
        mStartingActivePageRequests = startingActivePageRequests;
    }

    /**
     * Get the number of active page requests when the page ended.
     * 
     * @return The number of active page requests when the page ended.
     */
    public int getEndingActivePageRequests() {
        return mEndingActivePageRequests;
    }

    /**
     * Set the number of active page requests when the page ended.
     * 
     * @param endingActivePageRequests
     *            The number of active page requests when the page ended.
     */
    public void setEndingActivePageRequests(final int endingActivePageRequests) {
        mEndingActivePageRequests = endingActivePageRequests;
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
        buffer.append("  <status>").append(STATUS_NAMES[mStatus]).append("</status>\n");
        buffer.append("  <uri>").append(getUri()).append("</uri>\n");
        buffer.append("  <username>").append(getUsername()).append("</username>\n");
        buffer.append("  <session_id>").append(getSessionId()).append("</session_id>\n");
        buffer.append("  <user_agent>").append(getUserAgent()).append("</user_agent>\n");
        buffer.append("  <upload_duration>").append(getUploadDuration()).append("</upload_duration>\n");
        buffer.append("  <download_duration>").append(getDownloadDuration()).append("</download_duration>\n");

        buffer.append("  <starting_active_page_requests>");
        buffer.append(getStartingActivePageRequests());
        buffer.append("</starting_active_page_requests>\n");

        buffer.append("  <ending_active_page_requests>");
        buffer.append(getEndingActivePageRequests());
        buffer.append("</ending_active_page_requests>\n");
    }

    /**
     * @see ProfileObject#appendCustomDataHtml(StringBuffer)
     */
    @Override
    protected void appendCustomDataHtml(final StringBuffer buffer) {
        buffer.append("<strong>Status:</strong>&nbsp;").append(STATUS_NAMES[mStatus]).append("<br/>\n");

        buffer.append("</td>\n<td style=\"").append(QueryProfile.STYLE).append("\">");
        buffer.append("<strong>Username:</strong>&nbsp;").append(getUsername()).append("<br/>\n");
        buffer.append("<strong>URI:</strong>&nbsp;").append(getUri()).append("<br/>\n");
        buffer.append("<strong>Session Id:</strong>&nbsp;").append(getSessionId()).append("<br/>\n");
        buffer.append("<strong>User Agent:</strong>&nbsp;").append(getUserAgent()).append("<br/>\n");

        buffer.append(getStartingActivePageRequests());
        buffer.append("/");
        buffer.append(getEndingActivePageRequests());

        buffer.append("<br/>\n");
    }
}
