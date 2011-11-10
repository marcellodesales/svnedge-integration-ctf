#!/usr/bin/env python2
#
# -*-python-*-
#
# SourceForge(r) Enterprise Edition
# Copyright 2010 CollabNet, Inc.  All rights reserved.
# http://www.collab.net

""" Implementation of Subversion's pre-revpropchange hook used by CollabNet
TeamForge to verify the following:
  [1]: Changes to svn:author and svn:date are not permitted
  [2]: Changes to svn:log get validated by TeamForge for validity

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

import LogFile
import SourceForge
import datetime
import sys

DEBUG = False

def main():
    """ The main function. """
    args = sys.argv

    if len(args) != 7:
        log.write('Error: not enough args: %s' % str(args))
        return 1

    # Initialize important variables
    repo_path = args[1]
    revision = args[2]
    user = args[3]
    prop_name = args[4]
    action = args[5]
    system_id = args[6]

    log.write('Arguments: %s' % ' '.join(args))
    log.write('Property change request (%s)' % datetime.datetime.now().strftime('%Y-%m-%d %H:%M:%S'))
    log.write('Repository   : %s' % repo_path)
    log.write('Revision     : %s' % revision)
    log.write('User         : %s' % user)
    log.write('Property name: %s' % prop_name)
    log.write('Action       : %s' % action)
    log.write('System id    : %s' % system_id)

    # Make sure changes to svn:author and svn:date are not permitted
    if prop_name == 'svn:author':
        log.write('Permission denied: Modifying/Removing svn:author is prohibited.')
        sys.stderr.write('Permission denied: Modifying/Removing svn:author is prohibited.')
        return 1
    elif prop_name == 'svn:date':
        log.write('Permission denied: Modifying/Removing svn:date is prohibited.')
        sys.stderr.write('Permission denied: Modifying/Removing svn:date is prohibited.')
        return 1

    if prop_name == 'svn:log' and action == 'D':
        log.write('Permission denied: Removing svn:log is prohibited.')
        sys.stderr.write('Permission denied: Removing svn:log is prohibited.')
        return 1

    # Make sure changes to svn:log validate just like during pre-commit
    if prop_name == 'svn:log' and action == 'M':
        log_msg = ''

        # If stdin is not open, so nothing was passed in, this can become a problem
        # since it is blocking.  Be care invoking this outside of Subversion.
        for line in iter(sys.stdin.readline, ''):
            log_msg = log_msg + line

        log_msg = unicode(log_msg, 'utf-8')

        log.write("Log message: '%s'" % log_msg)

        # On Windows, we DO NOT pass the actual repository's full path and instead
        # create a path based on the understanding that ALL Windows integration
        # servers will be configured with '/windows-scmroot' as the repository
        # root path since CTF doesn't work with Windows paths.
        if SourceForge.isWindows():
            old_repo_path = repo_path
            repo_path = SourceForge.getCTFWindowsRepositoryPath(repo_path)

            log.write('Converting full Windows path to CTF full path: %s->%s' % (old_repo_path, repo_path))

        scm = SourceForge.getSOAPClient("SourceForge")
        key = SourceForge.createScmRequestKey()
        response_raw = scm.isValidCommitMessage(key, user, system_id, repo_path, log_msg)
        response_parts = response_raw.split('\n')
        response = response_parts.pop(0).strip() # The first line of the response is a boolean for success/failure
        response_msg = '\n'.join(response_parts)

        log.write('Response: %s' % response)
        log.write('Message : %s' % response_msg)

        if response == 'false':
            response_msg = response.strip()

            log.write('svn:log is invalid:  %s' % response_msg)
            sys.stderr.write('svn:log is invalid:  %s' % response_msg)

            return 1

    return 0

# Following the same pattern as the other Subversion hooks
if __name__ == '__main__':
    result = 0
    log = LogFile.LogFile('%s/%s' % (SourceForge.get('sfmain.logdir', SourceForge.getTemporaryDirectory()), 'pre-revprop-change.log'))
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
