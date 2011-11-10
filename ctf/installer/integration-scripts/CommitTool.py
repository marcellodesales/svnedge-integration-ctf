#!/usr/bin/env python2

# Example usage:
#
#  CommitTool.py create exsy1001 /cvsroot/repo1 admin localhost 8080
#    CommitTool.py add spam/Yummy.java 1.1 Added
#    CommitTool.py add spam/Tasty.java 1.3 Deleted
#    CommitTool.py add spam/Spork.java 1.8 Modified
#    CommitTool.py add spam/New.java 1.1 Moved spam/Original.java 1.10
#    CommitTool.py add spam/New.java 1.1 Copied spam/Source.java 1.11
#    CommitTool.py print
#  CommitTool.py commit "Fixed NPE in spam"
#  CommitTool.py create exsy1001 /cvsroot/repo2 admin localhost 443 1

import os
import sys
import pickle
import SourceForge

ppid = os.getppid()
transaction_file = '/tmp/scm_transaction.' + str(ppid)

def usage(message=None):
    if message:
        print "usage: scm " + message
    else:
        print "usage: scm <create|add|remove|modify|commit|print> [options]"
    sys.exit(1)


class ScmTransaction:
    def __init__(self):
        self.exsyid = None
        self.path = None
        self.username = None
        self.host = None
        self.port = None
        self.entries = {}
        self.statuses = ["Added", "Deleted", "Modified", "Moved", "Copied"]

    def hasFile(self, name):
        return self.entries.has_key(name)

    def assertFileDefined(self, name):
        if not self.hasFile(name):
            raise Exception("File not defined: " + name)

    def assertFileNotDefined(self, name):
        if self.hasFile(name):
            raise Exception("File is already defined: " + name)

    def assertValidStatus(self, status):
        if not status in self.statuses:
            raise Exception("Invalid Status: " + status + "\nValid Status values: " + str(self.statuses))

    def addFile(self, name, version, status='Added'):
        self.assertFileNotDefined(name)
        self.assertValidStatus(status)
        entry = {'version': version, 'status': status, 'fromFile': '', 'fromVersion': ''}
        self.entries[name] = entry

    def addFileWithReference(self, name, version, status, fromFile, fromVersion):
        self.assertFileNotDefined(name)
        self.assertValidStatus(status)
        entry = {'version': version, 'status': status, 'fromFile': fromFile, 'fromVersion': fromVersion}
        self.entries[name] = entry

    def rmFile(self, name):
        self.assertFileDefined(name)
        del self.entries[name]

    def touchFile(self, name, version, status=None):
        self.assertFileDefined(name)
        entry = self.entries[name]
        if status != None:
            self.assertValidStatus(status)
            entry['status'] = status
        entry['version'] = version
        entry['fromFile'] = ''
        entry['fromVersion'] = ''

    def touchFileWithReference(self, name, version, status, fromFile, fromVersion):
        self.assertFileDefined(name)
        entry = self.entries[name]
        entry['status'] = status
        entry['version'] = version
        entry['fromFile'] = fromFile
        entry['fromVersion'] = fromVersion

    def clear(self):
        self.entries = {}

    def describe(self):
        systemId = str(self.exsyid)
        username = str(self.username)
        path = str(self.path)
        print "Repository: " + username + "@" + systemId + ":" + path
        for entryKey in self.entries.keys():
            entry = self.entries[entryKey]
            version = str(entry['version'])
            status = str(entry['status'])
            fromFile = entry['fromFile']
            fromVersion = str(entry['fromVersion'])
            fromString = ''
            if fromFile:
                fromString = ' (From: ' + fromFile + ' ' + fromVersion + ')'
            print status + "\t" + entryKey + " " + version + fromString


def doCommit(args, transaction):
    if len(args) != 1:
        usage("commit <commitmessage>")
    logmsg = args[0]
    filenames = []
    versions = []
    statuses = []
    reffiles = []
    refversions = []
    doTheCommit = 0

    for entryKey in transaction.entries.keys():
        entry = transaction.entries[entryKey]
        doTheCommit = 1
        filenames += [entryKey]
        versions += [str(entry['version'])]
        statuses += [str(entry['status'])]
        reffiles += [entry['fromFile']]
        refversions += [str(entry['fromVersion'])]

    if not doTheCommit:
        print "Nothing to do."
        return

    user = transaction.username
    systemid = transaction.exsyid
    path = transaction.path
    host = transaction.host
    port = transaction.port

    scm = SourceForge.getSOAPClient("ScmListener")

    key = SourceForge.createScmRequestKey()
    if not scm.isValidCommitMessage(key, user, systemid, path, logmsg):
        raise Exception("Artifact association required to commit.")

    key = SourceForge.createScmRequestKey()
    commitId = scm.createCommit(key, user, systemid, path, logmsg, filenames, versions, statuses, reffiles, refversions,
                                None)
    transaction.clear()

# takes systemid, path, username
def doCreate(args, transaction):
    if len(args) not in (5, 6):
        usage("create <systemId> <repoPath> <username> <host> <port> [<use_ssl>]")
    transaction.exsyid = args[0]
    transaction.path = args[1]
    transaction.username = args[2]
    transaction.host = args[3]
    transaction.port = args[4]
    if len(args) == 6:
        transaction.use_ssl = int(args[5])
    else:
        transaction.use_ssl = 0
    transaction.entries = {}


def doAdd(args, transaction):
    if len(args) < 2:
        usage("add <filename> <version> [<status> [<fromfile> <fromversion>]]")
    if len(args) == 2:
        transaction.addFile(args[0], args[1])
    else:
        if args[2] == 'Moved' or args[2] == 'Copied':
            if len(args) > 4:
                transaction.addFileWithReference(args[0], args[1], args[2], args[3], args[4])
            else:
                raise Exception("Copied and Moved statuses require a from filename and version")
        else:
            transaction.addFile(args[0], args[1], args[2])


def doRemove(args, transaction):
    if len(args) != 1:
        usage("remove <filename>")
    transaction.rmFile(args[0])


def doModify(args, transaction):
    if len(args) < 2:
        usage("modify <filename> <version> [<status> [<fromfile> <fromversion>]]")
    if len(args) == 2:
        transaction.touchFile(args[0], args[1])
    else:
        if args[2] == 'Moved' or args[2] == 'Copied':
            if len(args) > 4:
                transaction.touchFileWithReference(args[0], args[1], args[2], args[3], args[4])
            else:
                raise Exception("Copied and Moved statuses require a from filename and version")
        else:
            transaction.touchFile(args[0], args[1], args[2])


def doPrint(args, transaction):
    transaction.describe()

# Actual execution is started here.
if len(sys.argv) < 2:
    usage()
command = sys.argv[1]

transaction = ScmTransaction()
if command != "create" and os.path.exists(transaction_file):
    try:
        fp = open(transaction_file, 'r')
        transaction = pickle.load(fp)
        fp.close()
    except:
        print "Warn: Corrupt transaction file, creating new one."
        transaction = ScmTransaction()

try:
    if command == "create":
        doCreate(sys.argv[2:], transaction)
    elif command == "add":
        doAdd(sys.argv[2:], transaction)
    elif command == "remove":
        doRemove(sys.argv[2:], transaction)
    elif command == "commit":
        doCommit(sys.argv[2:], transaction)
    elif command == "modify":
        doModify(sys.argv[2:], transaction)
    elif command == "print":
        doPrint(sys.argv[2:], transaction)
    else:
        usage()
except Exception, e:
    print e.__str__()
    sys.exit(1)

try:
    fp = open(transaction_file, 'w')
    pickle.dump(transaction, fp)
    fp.close()
except:
    print "Error: Could not write transaction file."
    sys.exit(1)
