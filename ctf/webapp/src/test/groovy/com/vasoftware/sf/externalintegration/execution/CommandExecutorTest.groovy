/*
 * CollabNet Subversion Edge
 * Copyright (C) 2010, CollabNet Inc. All rights reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.vasoftware.sf.externalintegration.execution;

import junit.framework.TestCase
import com.vasoftware.sf.externalintegration.execution.executors.WindowsCommandExecutor
import com.vasoftware.sf.common.configuration.SfGlobalOptionsManager
import com.vasoftware.sf.externalintegration.execution.CommandExecutor.HookEvent
import com.vasoftware.sf.common.configuration.SfPaths
import com.vasoftware.sf.externalintegration.execution.executors.UnixCommandExecutor;

public class CommandExecutorTest extends TestCase {
    
    private File testRepo
  
    @Override 
    public void setUp() throws Exception {
        super.setUp();
        System.setProperty("sourceforge.home", "/path/to/sourceforge")
        testRepo = createTestDir("repo")
    }

    @Override 
    public void tearDown() throws Exception {
        super.tearDown();
        testRepo.deleteDir()
    }
    
    public void testCreateHookScriptWindows() {
        
        CommandExecutor exec = new WindowsCommandExecutor()
       
        def script = 'python /path/to/script.py $1 $2 $3'
        exec.createHookScript (testRepo.getAbsolutePath(), HookEvent.PRE_COMMIT, script) 
        
        File hookScript = new File(testRepo.getAbsolutePath() + "/hooks/" + "pre-commit.bat")
        assertTrue("The batch hook script should now exist", hookScript.exists() )
        
        def contents = hookScript.readLines()

        boolean testedScript = false
        for (String line in contents) {
            if (line.startsWith("python")) {
                testedScript = true
                def expected = 'python \\path\\to\\script.py %1 %2 %3'
                assertEquals("Expected Windows-conversion of script: ${expected}", expected, line)
                break
            }
        }
        assertTrue("Should have tested the scriptContents but did not", testedScript)

        // test shebang NOT PRESENT (start with the SOURCEFORGE SECTION)
        def expected = '# BEGIN SOURCEFORGE SECTION - Do not remove these lines'
        assertEquals("Did not find expected at line 1: ${expected}", expected, contents[0])

    }

    public void testCreateHookScriptUnix() {

        CommandExecutor exec = new UnixCommandExecutor()

        def script = 'python /path/to/script.py $1 $2 $3'
        exec.createHookScript (testRepo.getAbsolutePath(), HookEvent.PRE_COMMIT, script)

        File hookScript = new File(testRepo.getAbsolutePath() + "/hooks/" + "pre-commit")
        assertTrue("The batch hook script should now exist", hookScript.exists() )

        def contents = hookScript.readLines()

        boolean testedScript = false
        for (String line in contents) {
            if (line.startsWith("python")) {
                testedScript = true
                def expected = 'python /path/to/script.py $1 $2 $3'
                assertEquals("Expected Unix-conversion of script: ${expected}", expected, line)
            }

        }
        assertTrue("Should have tested the scriptContents but did not", testedScript)

        // test shebang
        def expected = '#!/bin/sh'
        assertEquals("Did not find expected at line 1: ${expected}", expected, contents[0])

    }
    
    private File createTestDir(String prefix) {
        
        def testDir = File.createTempFile(prefix + "-test", null)
        testDir.delete()
        testDir.mkdir()
        new File(testDir, "hooks").mkdir()
        testDir.deleteOnExit()
        return testDir
    }
    
    
}
