/*
 * $RCSfile: CommandResult.java,v $
 *
 * SourceForge(r) Enterprise Edition
 * Copyright 2007 CollabNet, Inc.  All rights reserved.
 * http://www.collab.net
 *
 * The source code included in this listing is the confidential
 * and proprietary information of CollabNet, Inc.
 */

package com.vasoftware.sf.externalintegration.execution;

/**
 * This class is used to provide a single unit of data that can be returned
 *
 * @author Richard Lee <rlee@vasoftware.com>
 * @author Priyanka Dhanda <pdhanda@vasoftware.com>
 * @version $Revision: 1.4 $ $Date: 2007/05/24 00:37:29 $
 */
public class CommandResult {
    /**
     * A constant representing a successful return code.
     */
    public static final int RETURN_SUCCESS = 0;

    private String mStdout;
    private String mStderr;
    private String mCommand;
    private int mReturnValue;

    /**
     * Constructor for commands which actually execute and have an return value.
     *
     * @param command     The command that was executed.
     * @param stdout      A string representing the command's output to its stdout stream.
     * @param stderr      A string representing the command's output to its stderr stream.
     * @param returnValue The OS return value of the command.
     */
    public CommandResult(String command, String stdout, String stderr, int returnValue) {
	mCommand = command;
	mStdout = stdout;
	mStderr = stderr;
	mReturnValue = returnValue;
    }

    /**
     * Returns the String representing any output sent to the commands stdout stream.
     *
     * @return String - The program's Stdout output as a string.
     */
    public String getStdout() {
	return mStdout;
    }

    /**
     * Returns the Standard error output of the program being run.
     *
     * @return String - the program's Stderr output as a string.
     */
    public String getStderr() {
	return mStderr;
    }

    /**
     * Returns the command's exit code.
     *
     * @return int - the command's OS exit code.
     */
    public int getReturnValue() {
	return mReturnValue;
    }


    /**
     * Get the command executed
     *
     * @return String - the command executed as a String.
     */
    public String getCommand() {
	return mCommand;
    }

    /**
     * returns a string representing the command's Standard output and standard error.
     *
     * @return String - the output as a String.
     */
    public String getCommandOutput() {
	return "Stderr: \"" + getStderr() + "\" Stdout: \"" + getStdout() + "\"";
    }

}
