#!/usr/bin/env python2
#
# -*-python-*-
#
# TeamForge(r) Enterprise Edition
# Copyright 2013 CollabNet, Inc.  All rights reserved.
# http://www.collab.net

""" Implementation of Subversion's post-revpropchange hook used by CollabNet
TeamForge to verify the following:
  [1]: Changes to svn:log get updated to TeamForge

Authentication and authorization are handled upstream and are not a
concern here. Below are the arguments passed in my Subversion
  [1] REPOS-PATH   (the path to this repository)
  [2] REVISION     (the revision being tweaked)
  [3] USER         (the username of the person tweaking the property)
  [4] PROPNAME     (the property being set on the revision)
  [5] ACTION       (the property is being 'A'dded, 'M'odified, or 'D'eleted)

Standard in is used to pass in the new property value.  We also pass in
the external sytem id as the sixth script argument.
"""

import sys
import svn
import LogFile
import datetime
import SourceForge
import SubversionUtil
from suds import MethodNotFound

DEBUG = False

def doIt(pool, repo_path, revision, user, prop_name, action, system_id):

    # global log
    hostName = SourceForge.getRequired('sfmain.integration.listener_host')
    hostPort = SourceForge.getRequired('sfmain.integration.listener_port')

    if system_id == None:
        system_id = SourceForge.getRequired('external_system_id')

    svnlook = SubversionUtil.createSVNLook(repo_path, rev = revision)
    log_msg = ''    

    # Make sure changes to svn:log validate just like during pre-commit
    if prop_name == 'svn:log' and action == 'M':

        log_msg = svnlook.log()

	try:
            log.write("Log message: '%s'" % log_msg)
            log.write("Making updateCommit() call")

            scm = SourceForge.getSOAPClient("ScmListener")
            key = SourceForge.createScmRequestKey()

            scm.updateCommit(key, user, system_id, repo_path, str(revision), log_msg) 
            log.write('Commit updated successfully.')

	except MethodNotFound:
	    log.write('Failed to update commit object. ScmListener.updateCommit is added in CTF v7.0\n')

	except Exception, e:
	    log.write('Failed to update commit object: ' + str(e) + '\n')
            import traceback
            traceback.print_exc()
            traceback.print_exc(None, log)
            return 1
    return 0


def main():
    args = sys.argv

    if len(args) != 7:
        log.write('Error: not enough args: %s' % str(args))
        return 1

    repo_path = unicode(args[1])
    revision = int(args[2])
    user = args[3]
    prop_name = args[4]
    action = args[5]
    system_id = args[6]

    if not SubversionUtil.isSVNRepository(repo_path):
        raise Error, "path '%s' doesn't look like a repository" % repo_path

    log.write('Arguments: %s' % ' '.join(args))
    log.write('Property change request (%s)' % datetime.datetime.now().strftime('%Y-%m-%d %H:%M:%S'))
    log.write('Repository   : %s' % repo_path)
    log.write('Revision     : %s' % revision)
    log.write('User         : %s' % user)
    log.write('Property name: %s' % prop_name)
    log.write('Action       : %s' % action)
    log.write('System id    : %s' % system_id)

    print 'repository: ', repo_path
    print 'revision: ', revision
    svn.core.run_app(doIt, repo_path, revision, user, prop_name, action, system_id)



if __name__ == '__main__':
    result = 0
    log = LogFile.LogFile('%s/%s' % (SourceForge.get('sfmain.logdir', SourceForge.getTemporaryDirectory()), 'post-commit.log'))
    log.setLogging(DEBUG)

    try:
        result = main()
    except Exception, inst:
        import traceback
        traceback.print_exc()
        traceback.print_exc(None, log)
        result = 1

    log.close()

    sys.exit(result)
