#!/usr/bin/env python2
#
# SourceForge(r) Enterprise Edition
# Copyright 2007 CollabNet, Inc.  All rights reserved.
# http://www.collab.net

"""Sourceforge Subversion integration pre-commit script - given a repository 
and a transaction id, verify that the commit is valid (for example, has 
required associations specified)."""

import LogFile
import SourceForge
import SubversionUtil
import os
import svn
import sys

DEBUG = False

def doIt(pool, repository, txn, systemId):
    host = SourceForge.getRequired('sfmain.integration.listener_host')
    port = SourceForge.getRequired('sfmain.integration.listener_port')

    if systemId == None:
        systemId = SourceForge.getRequired('external_system_id')

    tempdir = SourceForge.getRequired('sfmain.tempdir')

    svnlook = SubversionUtil.createSVNLook(repository, txn = txn)
    logMsg = unicode(svnlook.log(), 'utf-8')
    author = svnlook.author()

    log.write("commit message: %s\n" % logMsg)
    
    scm = SourceForge.getSOAPClient("ScmListener")
    key = SourceForge.createScmRequestKey()

    # On Windows, we DO NOT pass the actual repository's full path and instead
    # create a path based on the understanding that ALL Windows integration
    # servers will be configured with '/windows-scmroot' as the repository
    # root path since CTF doesn't work with Windows paths.
    if SourceForge.isWindows():
        old_repository = repository
        repository = SourceForge.getCTFWindowsRepositoryPath(repository)

        log.write('Converting full Windows path to CTF full path: %s->%s' % (old_repository, repository))

    commitMessageResponseStr = scm.isValidCommitMessage(key, author, systemId, repository, logMsg)

    commitMessageResponseParts = commitMessageResponseStr.split('\n')
    curLine = commitMessageResponseParts.pop(0)

    # Subversion seems to only show output to standard error, and discards standard out
    print commitMessageResponseParts
    while len(commitMessageResponseParts) > 0:
        sys.stderr.write(commitMessageResponseParts.pop(0) + '\n')

    if curLine == 'false':
        return 1

    return 0

def main():
    args = sys.argv
    if len(args) < 3:
        print 'Error: not enough args:', args
        return 1

    repository = unicode(args[1], 'utf-8')
    txn = unicode(args[2], 'utf-8')

    if len(args) >= 4:
        systemId = args[3]
    else:
        systemId = None

    if not SubversionUtil.isSVNRepository(repository):
        raise Exception("path '%s' doesn't look like a repository" % repository)

    log.write('repository: %s\n' % repository)
    log.write('txn: %s\n\n' % txn)

    return svn.core.run_app(doIt, repository, txn, systemId)

if __name__ == "__main__":
    result = 0
    log = LogFile.LogFile('%s/%s' % (SourceForge.get('sfmain.logdir', SourceForge.getTemporaryDirectory()), 'pre-commit.log'))
    log.setLogging(DEBUG)

    try:
        result = main()
    except:
        import traceback
        exception = sys.exc_info()
        traceLines = traceback.format_exception(exception[0], exception[1], exception[2])
        result = 1

        for line in traceLines:
            sys.stderr.write(line)
            log.write('[error] %s' % line)

    log.close()

    sys.exit(result)
