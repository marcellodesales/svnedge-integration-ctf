/*
 * CollabNet TeamForge
 * Copyright 2010 CollabNet, Inc.  All rights reserved.
 * http://www.collab.net
 */

package com.vasoftware.sf.common.profiler;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Simple container that records interesting information about each page hit.
 */
@SuppressWarnings("unchecked")
public class PageRecord {
    /**
     * Thread local variable.
     */
    private static final ThreadLocal PAGE_RECORD = new ThreadLocal();

    /**
     * a count of how many pages are currently being actively processed
     */
    private static int smCurrentActivePageCount = 0;
    /**
     * Synchronization lock for the counter.
     */
    private static Object smCounterLock = new Object();

    /**
     * Date format.
     */
    protected static SimpleDateFormat smDateFormat = new SimpleDateFormat("yyyy/MM/dd hh:mm:ss,S");

    /***
     * Session activity records.
     */
    private static final int MAX_ACTVITY_LOG_COUNT = 1000;
    private static LinkedList smActivityRecords = new LinkedList();
    private static LinkedList smActivePageProfiles = new LinkedList();
    private static final boolean smIsDevelopmentEnvironment = initIsDevelopmentEnvironment();

    private int mStartPageCount;
    private int mEndPageCount;

    PageProfile mPageProfile;

    /**
     * public constructor for a PageRecord, which encapsulates simple historical information about the production of a
     * particular web page.
     * 
     * @param uri
     *            the URI for this page
     * @param sessionId
     *            Session ID.
     * @param username
     *            User name.
     * @param userAgent
     *            The user agent that is accessing the page.
     */
    public PageRecord(final String uri, final String sessionId, final String username, final String userAgent) {
        mPageProfile = new PageProfile(uri, sessionId, username, userAgent);

        synchronized (smCounterLock) {
            smActivePageProfiles.add(mPageProfile);
            if (PAGE_RECORD.get() == null) {
                PAGE_RECORD.set(this);

                smActivityRecords.add(this);

                if (smActivityRecords.size() > MAX_ACTVITY_LOG_COUNT) {
                    smActivityRecords.removeFirst();
                }

                ++smCurrentActivePageCount;
            }

            mStartPageCount = smCurrentActivePageCount;
        }

        mPageProfile.setStartingActivePageRequests(mStartPageCount);
    }

    /**
     * Marks the end of page request.
     * 
     * @param sessionId
     *            Session id.
     * @param username
     *            User name.
     * @param exception
     *            Is error encountered in processing request?
     */
    public void endPage(final String sessionId, final String username, final boolean exception) {
        synchronized (smCounterLock) {
            smActivePageProfiles.remove(mPageProfile);
        }

        if (!mPageProfile.end(sessionId, username, exception)) {
            return;
        }

        final PageRecord currentPage = (PageRecord) PAGE_RECORD.get();
        synchronized (smCounterLock) {
            mEndPageCount = smCurrentActivePageCount;

            if (currentPage != null && currentPage.getId() == getId()) {
                PAGE_RECORD.set(null);
                smCurrentActivePageCount--;
            }
        }

        mPageProfile.setEndingActivePageRequests(mEndPageCount);
    }

    /**
     * Returns the ID of the request.
     * 
     * @return Request id.
     */
    public long getId() {
        return mPageProfile.getId();
    }

    /**
     * Get the page profile for this page record.
     * 
     * @return The page profile for this record.
     */
    public PageProfile getPageProfile() {
        return mPageProfile;
    }

    /**
     * Initialize the flag indicating whether or not we are in development mode.
     * 
     * @return true if the current environment is a development environment.
     */
    private static boolean initIsDevelopmentEnvironment() {
        try {
            Class.forName("com.vasoftware.sf.client.apps.devtools.RunQueryAction");
            return true;
        } catch (final ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Check to see if this is a development environment.
     * 
     * @return true if this is a development environment.
     */
    public static boolean isDevelopementEnvironment() {
        return smIsDevelopmentEnvironment;
    }

    /**
     * Returns a list of profile objects associated with this page.
     * 
     * @return List of profile objects.
     */
    public List getProfileObjects() {
        return mPageProfile.getChildren();
    }

    /**
     * Returns the thread specific page record.
     * 
     * @return Page record for the thread.
     */
    public static PageRecord getPageRecord() {
        return (PageRecord) PAGE_RECORD.get();
    }

    /**
     * Get the URI for this page
     * 
     * @return the URI (as returned by HttpServletRequest.getRequestURI()).
     */
    public String getUri() {
        return mPageProfile.getUri();
    }

    /**
     * Returns the session Id.
     * 
     * @return session Id.
     */
    public String getSessionId() {
        return mPageProfile.getSessionId();
    }

    /**
     * Returns the user name.
     * 
     * @return user name.
     */
    public String getUsername() {
        return mPageProfile.getUsername();
    }

    /**
     * Get the time, in milliseconds, that elapsed during processing of this page. NOTE: This does not include download
     * time or request upload time.
     * 
     * @return duration of time it took to process this page request without counting any upload/download time.
     */
    public long getDuration() {
        return mPageProfile.getDuration() - mPageProfile.getDownloadDuration() - mPageProfile.getUploadDuration();
    }

    /**
     * Request status (ACTIVE, FINISHED, ERROR)
     * 
     * @return Request status.
     */
    public int getStatus() {
        return mPageProfile.getStatus();
    }

    /**
     * Start page count.
     * 
     * @return Start page count.
     */
    public int getStartPageCount() {
        return mStartPageCount;
    }

    /**
     * End Page count.
     * 
     * @return End page count.
     */
    public int getEndPageCount() {
        return mEndPageCount;
    }

    /**
     * Returns the amount of memory used at start.
     * 
     * @return Starting memory.
     */
    public long getStartingMemory() {
        return mPageProfile.getStartingMemoryUse();
    }

    /**
     * Returns the amount of memory used at end.
     * 
     * @return Ending memory.
     */
    public long getEndingMemory() {
        return mPageProfile.getEndingMemoryUse();
    }

    /**
     * Get the amount of time a page spent on a file download.
     * 
     * @return The amount of time a page spent on a file download.
     */
    public long getDownloadDuration() {
        return mPageProfile.getDownloadDuration();
    }

    /**
     * Set the amount of time a download took.
     * 
     * @param duration
     *            The duration of any downloads that occurred.
     */
    public void setDownloadDuration(final long duration) {
        mPageProfile.setDownloadDuration(duration);
    }

    /**
     * Set the object to fix the time of when processing actually began.
     */
    public void startPageProcessing() {
        if (mPageProfile.getUploadDuration() == 0) {
            mPageProfile.setUploadDuration(System.currentTimeMillis() - mPageProfile.getStartTime());
        }
    }

    /**
     * Get the duration of the request upload time.
     * 
     * @return The request upload duration.
     */
    public long getUploadDuration() {
        return mPageProfile.getUploadDuration();
    }

    /**
     * Returns formatted start time.
     * 
     * @return Formatted start time string.
     */
    public String getFormattedStartTime() {
        return smDateFormat.format(new Date(mPageProfile.getStartTime()));
    }

    /**
     * Get a dump of all XML pages in record.
     * 
     * @return The xml representation of all pages currently stored.
     */
    public static String getPageActivityXmlRepresentation() {
        final StringBuffer xmlBuffer = new StringBuffer(ProfileObject.getXmlRepresentationStartTag()).append("\n");
        synchronized (smCounterLock) {
            for (final Iterator iterator = smActivityRecords.iterator(); iterator.hasNext();) {
                final PageRecord pageRecord = (PageRecord) iterator.next();
                pageRecord.getPageProfile().appendXmlRepresentation(xmlBuffer);
            }
        }
        xmlBuffer.append(ProfileObject.getXmlRepresentationEndTag()).append("\n");

        return xmlBuffer.toString();
    }

    /**
     * Returns the list of page activity records.
     * 
     * @return Page activity record list.
     */
    public static List getPageActivityList() {
        synchronized (smCounterLock) {
            return new ArrayList(smActivityRecords);
        }
    }

    /**
     * Returns the active page count.
     * 
     * @return active page count.
     */
    public static int getActivePageCount() {
        synchronized (smCounterLock) {
            return smCurrentActivePageCount;
        }
    }

    /**
     * Clears all activity logs.
     */
    public static void clearPageRecords() {
        synchronized (smCounterLock) {
            smActivityRecords.clear();
        }
    }

    /**
     * Get the current page record id.
     * 
     * @return The id of the current page record or -1 if no page is active.
     */
    public static long getCurrentId() {
        final PageRecord pageRecord = getPageRecord();
        if (pageRecord == null) {
            return -1;
        } else {
            return pageRecord.getId();
        }
    }

    /**
     * Returns the currently active page profiles.
     * 
     * @return List of currently active page profiles.
     */
    public static List getActivePageProfiles() {
        synchronized (smCounterLock) {
            return new ArrayList(smActivePageProfiles);
        }
    }
}
