#!/usr/bin/env python2

import os
import PerforceUtil
import SourceForge
import subprocess
import sys

def perform(args, env):
    perforcePort = SourceForge.getRequired('sfmain.integration.perforce.port')
    p4bin = SourceForge.getRequired('sfmain.integration.executables.perforce')
    sourceforgeHome = SourceForge.getRequired('sfmain.sourceforge_home')

    # We should have exactly 6 arguments:
    # 1) the script,
    # 2) The external system id
    # 3) The admin user (who we run "p4 descibe" as)
    # 4) the user
    # 5) the changeset number
    # 6) the repository root
    if len(args) != 6:
        raise Exception("Invalid Arguments: " + str(args))

    systemid = args[1]
    adminUser = args[2]
    user = args[3]
    changelist = args[4]
    repositoryroot = args[5]

    perforcePortParam = " -p" + perforcePort
    perforceUserParam = " -u " + adminUser

    commandParams = p4bin + perforceUserParam + perforcePortParam
    command = commandParams + " describe -s " + changelist
    process = subprocess.Popen(command, shell=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    describeout, describeerr = process.communicate()
    returncode = process.returncode
    if returncode != 0:
        raise Exception('p4 describe exited abnormally: returncode=' + str(returncode) +
                        ' command=[' + command + ']' +
                        ' output=' + describeout + ' error=' + describeerr)

    describedata = PerforceUtil.parsePerforceDescribe(describeout, commandParams, 'false')
    content = describedata['logmsg']

    integrationLog = sourceforgeHome + '/var/log'

    # the encoding attribute on the file object is read only.
    # at this point in time i don't know how to explicitly set
    # the encoding that we read in.

    scm = SourceForge.getSOAPClient("ScmListener")

    key = SourceForge.createScmRequestKey()
    commitMessageResponseStr = scm.isValidCommitMessage(key, user, systemid, repositoryroot, content)

    commitMessageResponseParts = commitMessageResponseStr.split('\n')
    curLine = commitMessageResponseParts.pop(0)

    print ''
    while len(commitMessageResponseParts) > 0:
        print commitMessageResponseParts.pop(0)

    if curLine == 'false':
        raise Exception("Association required to commit.")

# Actual execution is started here.
try:
    perform(sys.argv, os.environ)
except Exception, e:
    print 'PerforceSubmit Failed: ' + e.__str__()
    sys.exit(1)
