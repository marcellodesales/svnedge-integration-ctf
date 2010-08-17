package com.vasoftware.sf.common.util;

/**
 * The <code>EnvironmentUtil</code> class is a singleton giving access to information about the TeamForge environment.
 * (This class is a thread-safe, lazily-loaded singleton.
 * 
 * @since 5.2
 */
public class EnvironmentUtil {
    private boolean mIsSandbox = false;

    /**
     * Protected constructor.
     */
    protected EnvironmentUtil() {
        try {
            Class.forName("com.vasoftware.sf.server.apps.testsupport.TestSupportServiceImpl");

            mIsSandbox = true;
        } catch (final Throwable e) {
            ; /* Do nothing */
        }
    }

    /**
     * <code>SingletonHolder</code> is loaded on the first execution of
     * 
     * @see EnvironmentUtil#getInstance() or the first access to
     * @see SingletonHolder#INSTANCE, not before.
     */
    private static class SingletonHolder {
        private static final EnvironmentUtil INSTANCE = new EnvironmentUtil();
    }

    /**
     * Returns a thread-safe <code>EnvironmentUtil</code>.
     * 
     * @return the thread-safe, EnvironmentUtil singleton.
     */
    public static EnvironmentUtil getInstance() {
        return SingletonHolder.INSTANCE;
    }

    /**
     * This is a method that will determine whether or not we have the testsupport service.
     * 
     * @return true if this is running in a sandbox environment.
     */
    public boolean isSandboxEnvironment() {
        return mIsSandbox;
    }

    /**
     * This method will set whether or not we have the testsupport service.
     * 
     * @param isSandbox
     *            whether or not we are running in a sandbox environment
     */
    public void setIsSandboxEnvironment(final boolean isSandbox) {
        this.mIsSandbox = isSandbox;
    }
}
