#!/usr/bin/env python2

import os
import sys
import LogFile
import SourceForge
from chardet.universaldetector import UniversalDetector

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

#    pid = os.getpid() # get our process id
#    pgid = os.getpgrp() # get our process group id
#
#    # See if a commitid file already exists for our process group.
#    # If so, don't do anything all this work was done previously.
#    commitIdFilename = os.path.join(tempdir, user + '-' + str(pgid) + '-commitid' )
#    if os.path.exists(commitIdFilename):
#        return

    integrationLog = SourceForge.get('sfmain.sourceforge_home') + '/var/log'

    if (args[1] == 'true' or args[1] == 'false'):
        isWandisco = args[1] == 'true';
        # remove argument from list
        args = args[1:]
    else:
        isWandisco = False;

    if len(args) != 3 and len(args) != 4:
        raise Exception('Illegal arguments: ' + str(args))
    systemid = args[1]

    argNotExpanded = '%l' == args[2];
    if (argNotExpanded and len(args) != 4) or (not argNotExpanded and len(args) != 3):
        raise Exception('Illegal arguements: ' + str(args))

    if argNotExpanded:
        logfile = args[3]
    else:
        logfile = args[2]

    logmsgFile = file(logfile, "r")
    # the encoding attribute on the file ojbect is read only.
    # at this point in time i don't know how to explicitly set
    # the encoding that we read in.
    content = logmsgFile.read()
    logmsgFile.close()
    content = SourceForge.toutf8(content, log)
    
    scm = SourceForge.getSOAPClient("ScmListener")
    
    if (not isWandisco):
        key = SourceForge.createScmRequestKey()
        commitMessageResponseStr = scm.isValidCommitMessage(key, user, systemid, cvsroot, content)
    
        commitMessageResponseParts = commitMessageResponseStr.split('\n')
        curLine = commitMessageResponseParts.pop(0)
    
        print ''
        while len(commitMessageResponseParts) > 0:
            print commitMessageResponseParts.pop(0)
    
        if curLine == 'false':
            raise Exception("Association required to commit.")

#    key = SourceForge.createScmRequestKey()
#    commitId = scm.createCommit(key, user, systemid, cvsroot, content)
#
#    commitIdFile = file(commitIdFilename, 'w')
#    commitIdFile.write(commitId)
#    commitIdFile.close()


# Actual execution is started here.
try:
    log = LogFile.LogFile('/tmp/VerifyMsg.log')
    log.setLogging(False)
    perform(sys.argv,os.environ)
    log.close()
except Exception, e:
    import traceback
    traceback.print_tb(sys.exc_info()[2], None, log)
    log.close()
    print 'VerifyMessage Failed: ' + e.__str__()
    sys.exit(1)
