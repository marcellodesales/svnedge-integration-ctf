#!/usr/bin/env python2
import os
import sys
import LogFile
import SourceForge

def assertProperty(propertyMap, key):
    if not key in propertyMap:
        raise Exception(key + ' must be set')

def perform(args, env):
    tempdir = SourceForge.getRequired('sfmain.tempdir')
    host = SourceForge.getRequired('sfmain.integration.listener_host')
    port = SourceForge.getRequired('sfmain.integration.listener_port')

    assertProperty(env, 'USER')
    assertProperty(env, 'CVSROOT')

    user = env['USER']
    cvsroot = SourceForge.normalizeRepositoryPath(env['CVSROOT'])

    if (args[1] == 'true' or args[1] == 'false'):
        isWandisco = args[1] == 'true';
        # remove argument from list
        args = args[1:]
    else:
        isWandisco = False;
    
    systemid = args[1]
    if (args[2] == '%r/%p'):
        directory = args[3]
    else:
        directory = args[2]

    # figure out if we are trying to commit to CVSROOT.
    # If so, make sure we have the correct permission by making another call to the scmlistener.
    if (not isWandisco and directory == cvsroot + '/CVSROOT'):

        scm = SourceForge.getSOAPClient("ScmListener")
        key = SourceForge.createScmRequestKey()

        # this will throw an exception if the user does not have permission
        scm.checkTriggerPermission(key, user, systemid, cvsroot)

    pgid = os.getpgrp() # get our process group id

    # skip the commit message on stdin
    # sys.stdin.read()

    lastDirectoryFilename =  os.path.join(tempdir, user + '-' + str(pgid) + '-lastdir')
    # read in the commit id from the temp file

    lastDirectoryFile = file(lastDirectoryFilename, 'w')
    lastDirectoryFile.write(directory)
    lastDirectoryFile.close()
    return

try:
    log = LogFile.LogFile('/tmp/CommitInfo.log')
    log.setLogging(False)
    perform(sys.argv,os.environ)
    log.close()
except Exception, e:
    import traceback
    traceback.print_tb(sys.exc_info()[2], None, log)
    log.close()
    print 'CommitInfo Failed: ' + e.__str__()
    sys.exit(1)
