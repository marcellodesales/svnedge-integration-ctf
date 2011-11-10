#!/usr/bin/env python2

#
# $RCSfile: data-checkout.py,v $
#
# SourceForge(r) Enterprise Edition
# Copyright 2007 CollabNet, Inc.  All rights reserved.
# http://www.collab.net
#

import fcntl
import getopt
import getpass
import LogFile
import os
import os.path
import select
import shutil
import SourceForge
import subprocess
import svn
import sys

from socket import socket, gethostname

def executeSVNOperation(command):
    process = subprocess.Popen(command, shell=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    stdout, stderr = process.communicate()
    returncode = process.returncode
    if returncode != 0:
        raise RuntimeError, '%s failed with exit code %d\nStd Err: %s \nStd'\
            'output: %s' % (command, returncode, stderr, stdout)
    return stdout

def printUsageAndExit(error=None, exit_code=0):
    print
    print "This script Checkout the branding repository contents to data directory. "
    print
    print "Valid options: "
    print "    -h | --help             print this usage message and exit"
    print "    -d | --directory=       specify directory path containing the branding files"

    print
    if error:
        print >> sys.stderr, error

    sys.exit(exit_code)



def main( ):
    chkout_dir=SourceForge.getRequired("sfmain.integration.data_dir")
    svn_repo=SourceForge.getRequired("scm.branding.repo")
    changeOwnerdataDir = False
    # Condition remove the contents for the branding checkout folder
    # and check in the new contents to it.

    try:
        opts, args = getopt.getopt(sys.argv[1:], "hqd:VnF",
                                   ["help", "directory="])
    except getopt.GetoptError, exc:
        printUsageAndExit("Getopt Error:%s\n" % exc, 1)
    for option, value in opts:
        if option in ("-h", "--help"):
            printUsageAndExit()
        elif option in ('-d', "--directory"):
            changeOwnerdataDir = True

    if os.path.isdir(chkout_dir):
       for dr in os.listdir(chkout_dir):
           if os.path.isdir(dr) and dr != '.svn':
               shutil.rmtree(os.path.join(chkout_dir,dr))

    if not os.path.isdir(chkout_dir):
        os.makedirs(chkout_dir)
    svn_cmd="svn checkout file://"+svn_repo+ " " + chkout_dir
    executeSVNOperation(svn_cmd)
    
    if changeOwnerdataDir or os.getuid() == 0:
        user=SourceForge.getRequired("httpd.user")
        group=SourceForge.getRequired("httpd.group")
        os.system("chown -R %s:%s %s" % (user,group,chkout_dir))


    scm = SourceForge.getSOAPClient("ScmListener")
    try:
        key = SourceForge.createScmRequestKey()
        scm.clearBrandingOverrideCache(key)
    except Exception, inst:
        import traceback
        traceback.print_tb(sys.exc_info()[2], None, None)
        return 1
 
if __name__ == '__main__':
    main( )



