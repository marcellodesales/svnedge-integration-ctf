#!/usr/bin/env python2

#
# $RCSfile: post-commit.py,v $
#
# SourceForge(r) Enterprise Edition
# Copyright 2007 CollabNet, Inc.  All rights reserved.
# http://www.collab.net
#

"""Sourceforge post-commit script - given a repository and a revision number, create the commit
   in SourceForge and add the files to it."""

import LogFile
import SourceForge
import SubversionUtil
import re
import sys
import os
import svn

DEBUG = False

def doIt(pool, repository, revision, systemId):
    # global log
    hostName = SourceForge.getRequired('sfmain.integration.listener_host')
    hostPort = SourceForge.getRequired('sfmain.integration.listener_port')

    if systemId == None:
        systemId = SourceForge.getRequired('external_system_id')

    svnlook = SubversionUtil.createSVNLook(repository, rev = revision)
    changes = svnlook.changed()
    changes = SubversionUtil.generalizeChanges(changes)

    # Make a list of only paths, from a list of (op, prop, path) tuples
    changedPaths = []
    versions = []
    statuses = []
    refFilenames = []
    refVersions = []
    statusMap = {'D': 'Deleted', 'A': 'Added', 'M': 'Moved', 'C': 'Copied', 'U': 'Modified'}

    cachesToInvalidate = []
    vmToInvalidate = []
    
    for changeRec in changes:
        changedPaths.append(unicode(changeRec[2], 'utf-8'))
        if repository == "/sf-svnroot/branding":
            if changeRec[2].endswith(".properties") and changeRec[2].find('i18n/') != -1:
                fileName = changeRec[2]
                fileName = fileName[fileName.index('i18n/')+5:fileName.index('.properties')]
                fileName = fileName.replace('/','.')
                cachesToInvalidate.append(fileName)

            if changeRec[2].endswith(".vm") and changeRec[2].find('templates/mail/') != -1:
                fileName = changeRec[2]
                fileName = fileName[fileName.index('templates/mail/')+15:fileName.index('.vm')]
                fileName = fileName.replace('/','.')
                vmToInvalidate.append(fileName)       

        versions.append(str(revision))
        
        if changeRec[0]:
        	statuses.append(statusMap.get(changeRec[0], ''))
        elif changeRec[1]:
        	statuses.append(statusMap.get(changeRec[1], ''))
        
        if len(changeRec) > 3 and changeRec[3] != None:
            refFilenames.append(unicode(changeRec[3], 'utf-8'))
            refVersions.append(str(changeRec[4]))
        else:
            refFilenames.append(unicode('', 'utf-8'))
            refVersions.append('')

    author = svnlook.author()
    logmsg = svnlook.log()
    diff = svnlook.diff(max_length=25001)

    # Now hook up the SOAP call to create the commit
    log.write("Instantiating the scm listener")
    scm = SourceForge.getSOAPClient("ScmListener")

    commitId = None

    log.write("Making createCommit() call")
    key = SourceForge.createScmRequestKey()

    try:
        diff = re.sub(r'[\x00-\x08\x0B-\x0C\x0E-\x1F\x7f-\xff]', "", diff)
        diff = unicode(diff, 'utf-8')
    except Exception, e:
        log.write('Unable to encode diff: ' + str(e) + '\n')
        diff = 'Unable to generate diff.  (Diff content is not decodable.)'

    # On Windows, we DO NOT pass the actual repository's full path and instead
    # create a path based on the understanding that ALL Windows integration
    # servers will be configured with '/windows-scmroot' as the repository
    # root path since CTF doesn't work with Windows paths.
    if SourceForge.isWindows():
        old_repository = repository
        repository = SourceForge.getCTFWindowsRepositoryPath(repository)

        log.write('Converting full Windows path to CTF full path: %s->%s' % (old_repository, repository))

    commitId = scm.createCommit(key,
                                unicode(author, 'utf-8'),
                                systemId,
                                repository,
                                unicode(logmsg, 'utf-8'),
                                changedPaths,
                                versions,
                                statuses,
                                refFilenames,
                                refVersions,
                                diff)

    log.write('createCommit returned the commit id:' + commitId)

    # prepare and make the call to add files to the commit

    # Invalidate the cache if any commits need a cache refresh
    if len(cachesToInvalidate) != 0:
        cacheInvalidator = SourceForge.getSOAPClient("SourceForge", "50")
        for cache in cachesToInvalidate:
            cacheInvalidator.invalidateResourceBundleCache(cache)    

    if len(vmToInvalidate) != 0:
        cacheInvalidator = SourceForge.getSOAPClient("SourceForge", "50")
        for cache in vmToInvalidate:
            cacheInvalidator.invalidateEmailTemplate(cache)

    return 0

def main():
    args = sys.argv
    if len(args) < 3:
        print 'Error: not enough args: ', args
        return 1

    repository = unicode(args[1], 'utf-8')
    revision = int(args[2])

    if len(args) >= 4:
        systemId = args[3]
    else:
        systemId = None

    if not SubversionUtil.isSVNRepository(repository):
        raise Exception("path '%s' doesn't look like a repository" % repository)

    log.write('repository: %s' % repository)
    log.write('revision: %s' % revision)

    return svn.core.run_app(doIt, repository, revision, systemId)

if __name__ == "__main__":
    result = 0
    log = LogFile.LogFile('%s/%s' % (SourceForge.get('sfmain.logdir', SourceForge.getTemporaryDirectory()), 'post-commit.log'))
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
