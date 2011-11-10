#!/usr/bin/env python2
#
# $RCSfile: data-publish.py,v $
#
# SourceForge(r) Enterprise Edition
# Copyright 2007 CollabNet, Inc.  All rights reserved.
# http://www.collab.net
#

import sys, os, os.path, getopt
import popen2, fcntl, select
import svn
import shutil
import LogFile
import SourceForge
import getpass
from socket import socket, gethostname

def _dontBlock(fd):
    """
    Make the file descriptor non blocking
    """
    flags = fcntl.fcntl(fd, fcntl.F_GETFL)
    fcntl.fcntl(fd, fcntl.F_SETFL, flags | os.O_NONBLOCK)


def _getExeSleepTime():
    """
    Time in seconds for which the process should wait for some output (from a
    subprocess)
    """
    return 60


def executeSVNOperation(command):
    # create a child process, we want the stderr as a sepearte stream
    TRUE = 1
    child = popen2.Popen3(command, True)
    childOut = child.fromchild
    childErr = child.childerr
    _dontBlock(childOut.fileno())
    _dontBlock(childErr.fileno())
    outData = ''
    errData = ''
    outEOF = errEOF = 0
    while TRUE:
        # Get the objects which are ready
        # Wait for the specified time for some output
        readList, writeList, errList =\
        select.select([childOut.fileno(), childErr.fileno()], [], [],
                      _getExeSleepTime())
        if childOut.fileno() in readList:
            outChunk = childOut.read()
            if outChunk == '': outEOF = TRUE
            outData = outData + outChunk
        if childErr.fileno() in readList:
            errChunk = childErr.read()
            if errChunk == '': errEOF = TRUE
            errData = errData + errChunk
        if outEOF and errEOF: break
        # Allow a little time for buffers to fill
        select.select([], [], [], .1)
        # Let's wait for the child to close
    #FIXME: If we overrun the wait time, we pbly have to skip this as well
    err = child.wait()
    if err != 0:
        raise RuntimeError, '%s failed with exit code %d\nStd Err: %s \nStd'\
                            'output: %s' % (command, err, errData, outData)
    return outData


def printUsageAndExit(error=None, exit_code=0):
    print
    print "This script Checkout the branding repository contents to data directory. "
    print "The result/status is logged in to /tmp/remotePublishLog.txt file."
    print "Valid options: "
    print "    -h | --help             print this usage message and exit"
    print "    -d | --directory=       specify directory path containing the branding files"

    print
    if error:
        print >> sys.stderr, error

    sys.exit(exit_code)


def main( ):
    chkout_dir = SourceForge.getRequired("sfmain.publish.data_dir")
    svn_base_repo = SourceForge.getRequired("subversion_branding.repository_base")
    changeOwnerdataDir = False
    try:
        opts, args = getopt.getopt(sys.argv[1:], "hqd:VnF",
                                   ["help", "directory="])

        reponame = unicode(args[0], 'UTF-8')
        projectrepo = reponame.split("/")[-1]
        chkout_dir = chkout_dir + "/" + projectrepo
    except getopt.GetoptError, exc:
        printUsageAndExit("Getopt Error:%s\n" % exc, 1)
    for option, value in opts:
        if option in ("-h", "--help"):
            printUsageAndExit()
        elif option in ('-d', "--directory"):
            changeOwnerdataDir = True

    root_dir = chkout_dir
    chkout_dir = chkout_dir + "/www"
    isSvnUpoperation = 0;
    #svn check in operation logged in to remotePublishLog.txt
    f = open("/tmp/remotePublishLog.txt", "w")
    try:
        if not os.path.isdir(chkout_dir):
            os.makedirs(chkout_dir)
            f.write("\n No publish www folder gona checkout:")
            svn_cmd = "svn checkout file://" + svn_base_repo + "/" + projectrepo + "/www" + " " + chkout_dir;
        elif not os.path.isdir(chkout_dir + "/.svn"):
            f.write("\n No .svn folder inside www folder,so gona checkout:")
            svn_cmd = "svn checkout file://" + svn_base_repo + "/" + projectrepo + "/www" + " " + chkout_dir;
        else:
            isSvnUpoperation = 1; # svn up operation
            f.write("\n Doing svn up operation  :")
            svn_cmd = "svn up --accept theirs-full " + chkout_dir;
        try:
            status = executeSVNOperation(svn_cmd)
            f.write("\n succeeded: " + status)
        except RuntimeError, re:
            f.write("\n runtime error raised:" + str(re));
            if (isSvnUpoperation):
                try:
                    if os.path.isdir(chkout_dir):
                        for dr in os.listdir(chkout_dir):
                            if os.path.isdir(os.path.join(chkout_dir)):
                                shutil.rmtree(os.path.join(chkout_dir))
                    f.write("\n publish www folder cleaned, repository www would have been deleted")
                except:
                    f.write("\n Fatal error :Try again after removing directory:" + chkout_dir);
        except Exception, ex:
            f.write("\n exception raised:" + str(ex));
    finally:
        f.close()
    if changeOwnerdataDir or os.getuid() == 0:
        user = SourceForge.getRequired("httpd.user")
        group = SourceForge.getRequired("httpd.group")
        os.system("chown -R %s:%s %s" % (user, group, root_dir))


if __name__ == '__main__':
    main()

