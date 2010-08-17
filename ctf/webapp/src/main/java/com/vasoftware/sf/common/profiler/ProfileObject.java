/*
 * CollabNet TeamForge
 * Copyright 2010 CollabNet, Inc.  All rights reserved.
 * http://www.collab.net
 */

package com.vasoftware.sf.common.profiler;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.MDC;

import com.vasoftware.sf.common.logger.Logger;
import com.vasoftware.sf.common.util.StringUtil;

/**
 * This is the base object that is used for profiling various system events like method calls and query executions.
 */
@SuppressWarnings("unchecked")
public abstract class ProfileObject {
    private static final ThreadLocal CURRENT_PARENT = new ThreadLocal();
    private static final Object LOCK = new Object();

    private static long smCounter = 0;

    private final long mPageId = PageRecord.getPageRecord() != null ? PageRecord.getPageRecord().getId() : -1;
    private final ProfileObject mParent;
    private List mChildren;

    private long mId;
    private final String mThreadName;
    private final long mStartTime;
    private long mEndTime;
    private ProfileSummary mQuerySummary;
    private ProfileSummary mApiTransitionSummary;
    private String mErrorMessage;

    private final long mStartingAllocatedMemory;
    private final long mStartingMemoryUse;
    private long mEndingAllocatedMemory;
    private long mEndingMemoryUse;

    private boolean mIsComplete = false;

    /** These members are used to track whether a profile object should be written out upon completion. */
    private boolean mWriteOnCompletion = false;
    private String mOutputPath;

    /** This flag is used to determine whether or not query profiling is enabled. */
    private static boolean smQueryProfilingEnabled = false;
    /** This flag is used to determine whether or not API entry/exit should be profiled. */
    private static boolean smApiProfilingEnabled = false;
    /** This flag is used to determine whether or not event entry/exit should be profiled. */
    private static boolean smEventProfilingEnabled = false;

    private static Long smSoapThreshold = new Long(30000);

    /** Strings for the logging parent id. */
    private static final String LOG_ID_MIDDLE = " id: ";

    /**
     * Constructor for a profile object that initializes a unique id and the start time.
     */
    public ProfileObject() {
        synchronized (LOCK) {
            mId = ++smCounter;
        }

        mThreadName = Thread.currentThread().getName();
        mParent = (ProfileObject) CURRENT_PARENT.get();
        CURRENT_PARENT.set(this);

        if (mParent != null) {
            if (mParent.mChildren == null) {
                mParent.mChildren = new ArrayList();
            }

            mParent.mChildren.add(this);
        }

        mStartingAllocatedMemory = Runtime.getRuntime().totalMemory();
        mStartingMemoryUse = mStartingAllocatedMemory - Runtime.getRuntime().freeMemory();
        mStartTime = System.currentTimeMillis();

        // See if the parent is null, if so, make this the root for all log messages
        if (mParent == null) {
            MDC.put(Logger.PROFILE_ROOT_ANCESTOR_KEY, getLoggingIdString());
        }
    }

    /**
     * Get the current profile object. This may return null if there is no active profile object.
     * 
     * @return The current profile object. This is allowed to be null.
     */
    public static ProfileObject getCurrentProfileObject() {
        return (ProfileObject) CURRENT_PARENT.get();
    }

    /**
     * If this method is called, when the 'end()' method is executed, the xml representation for the profile object will
     * be dumped to a file with the supplied path.
     * 
     * @param outputPath
     *            The path to the file that should contain the profile information.
     */
    public void enableWriteOnCompletion(final String outputPath) {
        mWriteOnCompletion = true;
        mOutputPath = outputPath;
    }

    /**
     * Mark the end of execution for the event being profiled.
     */
    public final void end() {
        mEndTime = System.currentTimeMillis();
        mEndingAllocatedMemory = Runtime.getRuntime().totalMemory();
        mEndingMemoryUse = mEndingAllocatedMemory - Runtime.getRuntime().freeMemory();
        CURRENT_PARENT.set(mParent);

        mIsComplete = true;

        cleanup();

        if (mParent == null) {
            MDC.remove(Logger.PROFILE_ROOT_ANCESTOR_KEY);
        }

        if (mWriteOnCompletion && !StringUtil.isEmpty(mOutputPath)) {
            final File file = new File(mOutputPath);
            FileWriter writer = null;
            try {
                writer = new FileWriter(file);
                writer.write(getXmlRepresentation());
            } catch (final IOException e) {
                ;
            } finally {
                if (writer != null) {
                    try {
                        writer.close();
                    } catch (final IOException e) {
                        ; // do nothing
                    }
                }
            }
        }
    }

    /**
     * Provides a method where profile objects can execute extra logic after a profile end has been called.
     */
    protected void cleanup() {
        ; // do nothing by default
    }

    /**
     * Get the page id for this profile object.
     * 
     * @return The page id that was active for this profile object.
     */
    public long getPageId() {
        return mPageId;
    }

    /**
     * Get the id of the profiled event.
     * 
     * @return The id of the profiled event.
     */
    public long getId() {
        return mId;
    }

    /**
     * Returns the thread name associated with this profile object.
     * 
     * @return Thread name.
     */
    public String getThreadName() {
        return mThreadName;
    }

    /**
     * Get the start time of the profiled event.
     * 
     * @return The start time of the profiled event.
     */
    public long getStartTime() {
        return mStartTime;
    }

    /**
     * Get the end time of the profiled event.
     * 
     * @return The end time of the profiled event.
     */
    public long getEndTime() {
        return mEndTime;
    }

    /**
     * Get the amount of memory that was allocated to the JVM at the time when the profile was started.
     * 
     * @return The amount of memory that was allocated to the JVM at the time when the profile was started.
     */
    public long getStartingAllocatedMemory() {
        return mStartingAllocatedMemory;
    }

    /**
     * Get the amount of memory being used when the event started.
     * 
     * @return The amount of memory in use when the event started.
     */
    public long getStartingMemoryUse() {
        return mStartingMemoryUse;
    }

    /**
     * Get the amount of memory that was allocated to the JVM at the time when the profile was ended.
     * 
     * @return The amount of memory that was allocated to the JVM at the time when the profile was ended.
     */
    public long getEndingAllocatedMemory() {
        return mEndingAllocatedMemory;
    }

    /**
     * Get the amount of memory being used when the event ended.
     * 
     * @return The amount of memory in use when the event ended.
     */
    public long getEndingMemoryUse() {
        return mEndingMemoryUse;
    }

    /**
     * Get the change in memory use from the start to end of the profiled event.
     * 
     * @return The memory change in kilobytes.
     */
    public double getMemoryChange() {
        return Math.round(((double) (mEndingMemoryUse - mStartingMemoryUse)) / 10) / (double) 100;
    }

    /**
     * Get the duration of the profiled event.
     * 
     * @return The duration of the profiled event.
     */
    public long getDuration() {
        return mEndTime - mStartTime;
    }

    /**
     * Get the id string for this object.
     * 
     * @return The id string for the object.
     */
    public String getLoggingIdString() {
        return getType() + LOG_ID_MIDDLE + getId();
    }

    /**
     * Get the error message that if one was passed in for the profile object.
     * 
     * @return The error message.
     */
    public String getErrorMessage() {
        return mErrorMessage;
    }

    /**
     * If the profile ended due to an e, you should set the error message here.
     * 
     * @param e
     *            The error message that caused the premature end to the profile.
     */
    public void setException(final Throwable e) {
        mErrorMessage = e.getClass().getName() + ": " + e.getMessage();
    }

    /**
     * @see Object#equals(Object)
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }

        if (obj instanceof ProfileObject) {
            final ProfileObject profObj = (ProfileObject) obj;
            return profObj.getId() == getId();
        }

        return false;
    }

    /**
     * @see Object#hashCode()
     */
    @Override
    public int hashCode() {
        return new Long(getId()).hashCode();
    }

    /**
     * Get the parent of this profiled object.
     * 
     * @return The parent of the profile object.
     */
    public ProfileObject getParent() {
        return mParent;
    }

    /**
     * Check to see if this profile object represents a call that is complete.
     * 
     * @return true if the event being profiled has completed.
     */
    public boolean getIsComplete() {
        return mIsComplete;
    }

    /**
     * Get the children of this object.
     * 
     * @return The children of this object.
     */
    public List getChildren() {
        return mChildren;
    }

    /**
     * Get a summary of all queries executed beneath this profile object.
     * 
     * @return A map of total queries, total query time and longest query and longest query profile object id.
     */
    public ProfileSummary getQuerySummary() {
        if (mQuerySummary == null) {
            mQuerySummary = getChildQueryInformation(new ProfileSummary(), mChildren);
        }

        return mQuerySummary;
    }

    /**
     * Get a summary of all api transition information beneath this profile object.
     * 
     * @return A map of api transition information.
     */
    public ProfileSummary getApiTransitionSummary() {
        if (mApiTransitionSummary == null) {
            mApiTransitionSummary = getChildApiTransitionInformation(new ProfileSummary(), mChildren);
        }

        return mApiTransitionSummary;
    }

    /**
     * Get a summary map of information about all queries executed underneath this profile object.
     * 
     * @param profileSummary
     *            The summary of all
     * @param children
     *            A list of profile objects that are children of the object getting query information.
     * @return The summary or query information with all query information from the list added in.
     */
    private ProfileSummary getChildQueryInformation(final ProfileSummary profileSummary, final List children) {
        if (children != null) {
            for (final Iterator iterator = children.iterator(); iterator.hasNext();) {
                final ProfileObject profileObject = (ProfileObject) iterator.next();
                if (profileObject instanceof QueryProfile) {
                    profileSummary.addProfileInformation(profileObject);
                }

                getChildQueryInformation(profileSummary, profileObject.getChildren());
            }
        }

        return profileSummary;
    }

    /**
     * Get a summary map of information about all queries executed underneath this profile object.
     * 
     * @param profileSummary
     *            The summary of all
     * @param children
     *            A list of profile objects that are children of the object getting query information.
     * @return The summary or query information with all query information from the list added in.
     */
    private ProfileSummary getChildApiTransitionInformation(final ProfileSummary profileSummary, final List children) {
        if (children != null) {
            for (final Iterator iterator = children.iterator(); iterator.hasNext();) {
                final ProfileObject profileObject = (ProfileObject) iterator.next();
                if (profileObject instanceof MethodProfile) {
                    final MethodProfile method = (MethodProfile) profileObject;
                    if ("Call through local API layer.".equals(method.getDescription())) {
                        profileSummary.addProfileInformation(profileObject);
                    } else if ("Call through RMI API layer.".equals(method.getDescription())) {
                        profileSummary.addProfileInformation(profileObject);
                    }
                }

                getChildApiTransitionInformation(profileSummary, profileObject.getChildren());
            }
        }

        return profileSummary;
    }

    /**
     * Get the start tag for an xml representation document.
     * 
     * @return The start tag to the profile entry xml dump.
     */
    public static final String getXmlRepresentationStartTag() {
        return "<profile>";
    }

    /**
     * Get the end tag for an xml representation document.
     * 
     * @return The end tag to the profile entry xml dump.
     */
    public static final String getXmlRepresentationEndTag() {
        return "</profile>";
    }

    /**
     * Export this profile object and everything beneath it as XML.
     * 
     * @return The xml representing this profile object and all profile objects beneath it.
     */
    public final String getXmlRepresentation() {
        final StringBuffer xmlBuffer = new StringBuffer(ProfileObject.getXmlRepresentationStartTag()).append("\n");
        appendXmlRepresentation(xmlBuffer);
        xmlBuffer.append(ProfileObject.getXmlRepresentationEndTag()).append("\n");

        return xmlBuffer.toString();
    }

    /**
     * Append an XML representation of this profiled object to a buffer of profiled objects.
     * 
     * @param buffer
     *            The buffer to append to.
     */
    public void appendXmlRepresentation(final StringBuffer buffer) {
        buffer.append("<entry type=\"").append(getType()).append("\"");

        if (getParent() != null) {
            buffer.append(" parentId=\"").append(getParent().getId()).append("\"");
        }

        buffer.append(" id=\"").append(getId()).append("\"");
        buffer.append(" starting_memory=\"").append(getStartingMemoryUse()).append("\"");
        buffer.append(" ending_memory=\"").append(getEndingMemoryUse()).append("\"");
        buffer.append(" starting_memory_allocated=\"").append(getStartingAllocatedMemory()).append("\"");
        buffer.append(" ending_memory_allocated=\"").append(getEndingAllocatedMemory()).append("\"");
        buffer.append(" start_time=\"").append(getStartTime()).append("\"");
        buffer.append(" end_time=\"").append(getEndTime()).append("\"");
        buffer.append(">\n");

        if (getErrorMessage() != null) {
            buffer.append("    <error>").append(getErrorMessage()).append("</error>\n");
        }

        // Now fill in the custom data from the profile object.
        appendCustomDataXml(buffer);

        buffer.append("</entry>\n");

        if (mChildren != null) {
            for (final Iterator iterator = mChildren.iterator(); iterator.hasNext();) {
                final ProfileObject profileObject = (ProfileObject) iterator.next();
                profileObject.appendXmlRepresentation(buffer);
            }
        }
    }

    /**
     * Export the profile object and all profile objects beneath as HTML viewable in a web browser.
     * 
     * @return The html that represents the entire hierarchy of profile objects.
     */
    public final String getHtmlRepresentation() {
        final StringBuffer buffer = new StringBuffer();
        appendHtmlRepresentation(buffer);
        return buffer.toString();
    }

    /**
     * Append an HTML representation of this profiled object to a buffer of profiled objects.
     * 
     * @param buffer
     *            The string buffer to append to.
     */
    public void appendHtmlRepresentation(final StringBuffer buffer) {
        double startingAllocated = (double) mStartingAllocatedMemory / 1024 / 1024;
        startingAllocated = (double) Math.round(startingAllocated * 100) / 100;

        double endingAllocated = (double) mEndingAllocatedMemory / 1024 / 1024;
        endingAllocated = (double) Math.round(endingAllocated * 100) / 100;

        double startingMemory = (double) mStartingMemoryUse / 1024 / 1024;
        startingMemory = (double) Math.round(startingMemory * 100) / 100;

        double memoryChange = (double) ((mEndingMemoryUse - mStartingMemoryUse)) / 1024;
        memoryChange = (double) Math.round(memoryChange * 100) / 100;

        String style = "Profile " + getType();
        if (getDuration() > 4000) {
            style += " Long";
        }

        if (getErrorMessage() != null) {
            style += " Error";
        }

        buffer.append("<table id=\"summary_").append(getId()).append("\" class=\"").append(style).append("\"><tr>\n");

        if (getErrorMessage() != null) {
            buffer.append("<td class=\"ErrorMessage\" colspan=\"2\">Error Message:</strong>&nbsp;");
            buffer.append(getErrorMessage()).append("</td></tr><tr>\n");
        }

        buffer.append("<td class=\"Summary\">");
        buffer.append("<strong>Id:</strong>&nbsp;").append(getId()).append("<br/>");
        buffer.append("<strong>Starting Allocated Memory:</strong>&nbsp;").append(startingAllocated).append(" MB<br/>");
        buffer.append("<strong>Ending Allocated Memory:</strong>&nbsp;").append(endingAllocated).append(" MB<br/>");
        buffer.append("<strong>Starting Memory Use:</strong>&nbsp;").append(startingMemory).append(" MB<br/>");
        buffer.append("<strong>Memory Change:</strong>&nbsp;").append(memoryChange).append(" KB<br/>");
        buffer.append("<strong>Start Time:</strong>&nbsp;").append(new Date(getStartTime())).append("<br/>");
        buffer.append("<strong>Duration:</strong>&nbsp;").append(getDuration()).append("&nbsp;ms<br/>");
        buffer.append("<hr size=\"1\" color=\"#9999FF\" width=\"80%\"/>\n");
        buffer.append("<strong>Completed:</strong>&nbsp;").append(getIsComplete()).append("<br/>");

        appendCustomDataHtml(buffer);

        buffer.append("</td></tr></table>\n");

        if (mChildren != null) {
            buffer.append("<div style=\"padding-left: 20px; border: 0px none;\" id=\"children_");
            buffer.append(getId()).append("\">\n");
            for (final Iterator iterator = mChildren.iterator(); iterator.hasNext();) {
                buffer.append("<br/>");
                final ProfileObject profileObject = (ProfileObject) iterator.next();
                profileObject.appendHtmlRepresentation(buffer);
            }
            buffer.append("</div><br/>\n");
        }
    }

    /**
     * Get the string that holds the current top level parent.
     * 
     * @return The current top level parent on the thread.
     */
    public static final String getRootAncestorIdString() {
        ProfileObject root = (ProfileObject) CURRENT_PARENT.get();
        if (root == null) {
            return StringUtil.EMPTY;
        }

        while (root.getParent() != null) {
            root = root.getParent();
        }

        return root.getLoggingIdString();
    }

    /**
     * Get the type of profiled object that is represented. This should be a name that is valid to use as an XML element
     * name.
     * 
     * @return The type of profiled object.
     */
    public abstract String getType();

    /**
     * Append a section of XML elements that contain custom information about the specific profile object.
     * 
     * @param buffer
     *            The string buffer to append XML to.
     */
    protected abstract void appendCustomDataXml(StringBuffer buffer);

    /**
     * Append a section of HTML elements that contain custom information about the specific profile object.
     * 
     * @param buffer
     *            The string buffer to append HTML to.
     */
    protected abstract void appendCustomDataHtml(StringBuffer buffer);

    /**
     * Check to see if query profiling is enabled.
     * 
     * @return true if query profiling is enabled.
     */
    public static boolean isQueryProfilingEnabled() {
        return smQueryProfilingEnabled;
    }

    /**
     * Enable query profiling.
     */
    public static void enableQueryProfiling() {
        smQueryProfilingEnabled = true;
    }

    /**
     * Disable query profiling,
     */
    public static void disableQueryProfiling() {
        smQueryProfilingEnabled = false;
    }

    /**
     * Check to see if api entry/exit profiling is enabled.
     * 
     * @return true if api entry/exit is profiled
     */
    public static boolean isApiProfilingEnabled() {
        return smApiProfilingEnabled;
    }

    /**
     * Enable api entry/exit profiling.
     */
    public static void enableApiProfiling() {
        smApiProfilingEnabled = true;
    }

    /**
     * Disable api entry/exit profiling,
     */
    public static void disableApiProfiling() {
        smApiProfilingEnabled = false;
    }

    /**
     * Check to see if event entry/exit profiling is enabled.
     * 
     * @return true if event entry/exit is profiled
     */
    public static boolean isEventProfilingEnabled() {
        return smEventProfilingEnabled;
    }

    /**
     * Enable event entry/exit profiling.
     */
    public static void enableEventProfiling() {
        smEventProfilingEnabled = true;
    }

    /**
     * Disable event entry/exit profiling,
     */
    public static void disableEventProfiling() {
        smEventProfilingEnabled = false;
    }

    /**
     * Sets threshold for soap request
     * 
     * @param threshold
     *            threshold value in msec
     */
    public static void setSoapThreshold(final long threshold) {
        smSoapThreshold = threshold;
    }

    /**
     * Gets threshold for soap request
     * 
     * @return threshold value in msec
     */
    public static Long getSoapThreshold() {
        return smSoapThreshold;
    }

    /**
     * An object that contains a basic summary of query information.
     */
    public class ProfileSummary {
        private long mTotalTime = 0;
        private long mLongestEntryTime = 0;
        private long mLongestEntryId = 0;
        private int mEntryCount = 0;

        /**
         * Add the data from a query profile object to the summary information.
         * 
         * @param profile
         *            The profile that is being added.
         */
        public void addProfileInformation(final ProfileObject profile) {
            if (profile != null) {
                mEntryCount++;
                mTotalTime += profile.getDuration();

                if (profile.getDuration() > mLongestEntryTime) {
                    mLongestEntryTime = profile.getDuration();
                    mLongestEntryId = profile.getId();
                }
            }
        }

        /**
         * Get the total time of all queries executed.
         * 
         * @return The total query time.
         */
        public long getTotalTime() {
            return mTotalTime;
        }

        /**
         * Get the time of the longest query in milliseconds.
         * 
         * @return The time of the longest query in milliseconds.
         */
        public long getLongestEntryTime() {
            return mLongestEntryTime;
        }

        /**
         * Get the id of the query profile that had the longest run time.
         * 
         * @return The id of the query profile with the longest run time.
         */
        public long getLongestEntryId() {
            return mLongestEntryId;
        }

        /**
         * Get the total number of queries executed.
         * 
         * @return The total number of queries executed.
         */
        public int getEntryCount() {
            return mEntryCount;
        }
    }
}