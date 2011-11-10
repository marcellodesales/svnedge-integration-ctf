#!/usr/bin/env python2
import LogFile, os, re, SourceForge, StringIO, sys

from chardet.universaldetector import UniversalDetector

MAX_DIFF_LENGTH = 25001

BINARY_EXTENSIONS = ('gif', 'jpeg', 'jpg', 'png', 'tiff', 'exe', 'rpm', 'dmg',
                     'so', 'a', 'dyld', 'zip', 'tar', 'gz', 'bz2', 'ear', 'jar',
                     'rar', 'sar', 'class', 'pyc', 'pyd', 'doc', 'ppt', 'xls',
                     'pdf', 'deb', 'war', 'docx', 'xlsx', 'pptx')

def getFileDiff(cvsroot, file, oldRev, newRev):
    """ Generates a file's differences between oldRev and newRev """
    diff = StringIO.StringIO()

    if newRev == 'NONE':
        # File has been deleted and with CVS, you can't retrieve the content
        # of deleted files.
        pass
    elif file.split('.')[-1] in BINARY_EXTENSIONS:
        # Write the header
        diff.write('Index: ' + file + '\n')
        diff.write('+++ ' + file + ':' + str(newRev) + '\n')
        diff.write('Unable to produce differences as the file is binary.\n')
    elif oldRev == 'NONE' and not file[-1] == '/':
        cvs_diff_with_opts = ['cvs', '-Qn', '-d', cvsroot, 'checkout', '-p', '-r', '1.1',
                              file]

        # Write the header
        diff.write('Index: ' + file + '\n')
        diff.write('+++ ' + file + ':1.1\n')

        output = os.popen(" ".join(cvs_diff_with_opts))

        for line in output.readlines():
            diff.write('+' + line[:-1] + '\n')
    else:
        cvs_diff_with_opts = ['cvs', '-Qn', '-d', cvsroot, 'rdiff', '-r', str(oldRev),
                              '-r', str(newRev), '-u', file]

        output = os.popen(" ".join(cvs_diff_with_opts))

        for line in output.readlines():
            diff.write(line[:-1] + '\n')

        diff.write('\n')

    return diff.getvalue()

# getFileDiff()

def parseStandardIn(txt):
    lines = txt.splitlines()
    while len(lines) > 0:
        line = lines.pop(0)
        if not line:
            continue
        line = line.strip()
        if line == 'Log Message:':
            break

    result = ""
    while len(lines) > 0:
        line = lines.pop(0)
        result += line + '\n'

    return result

# parseStandardIn()

def assertProperty(propertyMap, key):
    if not key in propertyMap:
        raise Exception(key + ' must be set')

# assertProperty()

def perform(args, env):
    global log

    assertProperty(env, 'USER')
    assertProperty(env, 'CVSROOT')

    host = SourceForge.getRequired('sfmain.integration.listener_host')
    port = SourceForge.getRequired('sfmain.integration.listener_port')
    user = env['USER']
    cvsroot = SourceForge.normalizeRepositoryPath(env['CVSROOT'])

    # Remove optional WANdisco flag
    if (args[1] == 'true' or args[1] == 'false'):
        isWandisco = args[1] == 'true';
        # remove argument from list
        args = args[1:]
    else:
        isWandisco = False;

    systemid = args[1] # The external system id
    pid = os.getpid() # The process id
    pgid = os.getpgrp() # The process group id
    stdin = SourceForge.toutf8(parseStandardIn(sys.stdin.read()), log)
    changeRecord = args[2] # The record of the change

    # The following paths are used for the files used to chain
    # multiple parts of a single CVS interaction together
    tempdir = SourceForge.getRequired('sfmain.tempdir')
    commitStatusesFilename = os.path.join(tempdir, user + '-' + str(pgid) + '-statuses')
    commitFilesFilename = os.path.join(tempdir, user + '-' + str(pgid) + '-filenames')
    commitVersionsFilename = os.path.join(tempdir, user + '-' + str(pgid) + '-versions')
    commitDiffsFilename = os.path.join(tempdir, user + '-' + str(pgid) + '-diffs')
    lastDirectoryFilename = os.path.join(tempdir, user + '-' + str(pgid) + '-lastdir')

    if not changeRecord:
        raise Exception('Invalid empty loginfo string.')

    # Set defaults for the ScmListener call
    logmsg = stdin
    files = []
    versions = []
    statuses = []
    reffiles = []
    refversions = []
    diff = ''

    if changeRecord.endswith(' - New directory'):
        # As of right now, there is no good way to support creating a commit
        # object from a directory add.
        log.write('New directory added to repository: %s' %
                  changeRecord.split(' - New directory')[0])

        if os.path.exists(lastDirectoryFilename):
            os.remove(lastDirectoryFilename)

        return
    elif changeRecord.endswith(' - Imported sources'):
        log.write('New sources imported into repository: %s' %
                  changeRecord.split(' - Imported sources')[0])
        stdin_lines = stdin.split('\n')
        status_index = -1
        release_tags_index = -1
        files = []

        for i in range(len(stdin_lines)):
            if stdin_lines[i].startswith('Status:') and status_index == -1:
                status_index = i

            if stdin_lines[i].startswith('Release Tags:') and release_tags_index == -1:
                release_tags_index = i

                break

        if status_index == -1 or release_tags_index == -1:
            raise Exception('Invalid import message format.')

        logmsg = '\n'.join(stdin_lines[0:status_index - 1])

        # Get the imported lines
        for i in range(release_tags_index + 2, len(stdin_lines)):
            if stdin_lines[i].strip() == '':
                break

            files.append(stdin_lines[i][2:].strip())

        # For import, create lists with 'default' values
        versions = ['1.1'] * len(files)
        statuses = ['Added'] * len(files)
        reffiles = [''] * len(files)
        refversions = [''] * len(files)

        # Generate the diff
        for file in files:
            if len(diff) <= MAX_DIFF_LENGTH:
                diff = diff + getFileDiff(cvsroot, file, 'NONE', '1.1')

                if file != files[-1]:
                    diff = diff + '\n'
    else:
        lastDirectoryFile = open(lastDirectoryFilename, 'r')
        lastDirectory = lastDirectoryFile.read().strip()
        repositoryfile = open(os.path.join(os.getcwd(), 'CVS/Repository'), 'r')
        repositorydir = repositoryfile.readline().strip()
        moduledir = repositorydir

        if repositorydir.startswith(cvsroot):
            moduledir = repositorydir[len(cvsroot) + 1:]

        lastDirectoryFile.close()
        repositoryfile.close()

        filenames = []
        versions = []
        statusMap = {'R': 'Deleted', 'A': 'Added', 'M': 'Modified'}
        entryLogFilename = os.path.join(os.getcwd(), 'CVS/Entries.Log')
        entrylogFile = open(entryLogFilename, 'r')
        entrylog = entrylogFile.readlines()

        entrylogFile.close()

        log.write('Entries = %s' % entryLogFilename)

        commitFilesFile = open(commitFilesFilename, 'a+')
        commitFilesFile.seek(2, 0)
        commitVersionsFile = open(commitVersionsFilename, 'a+')
        commitVersionsFile.seek(2, 0)
        commitDiffsFile = open(commitDiffsFilename, 'a+')
        commitDiffsFile.seek(2, 0)
        commitStatusesFile = open(commitStatusesFilename, 'a+')
        commitStatusesFile.seek(2, 0)

        for entry in entrylog:
            entryparts = entry.split('/')
            filename = os.path.join(moduledir, entryparts[1])

            log.write("Entry: %s" % entry)
            log.write("EntryParts: %s" % entryparts)
            log.write("Filename: %s" % filename)

            if len(entryparts) > 2:
                version = entryparts[2].strip()
            else:
                version = 'NONE'

            statusCheck = statusMap.get(entryparts[0].strip()[0], '')
            status = statusCheck

            # CVS uses the same value for adds and modifications so
            # identify which is the real case here.
            if statusCheck == 'Added':
                if version.endswith('.1'):
                    status = 'Added'
                elif version == 'NONE':
                    status = 'Added'
                else:
                    status = 'Modified'

            cvsLogInfo = args[-1]
            oldRevision = 'NONE'
            newRevision = 'NONE'

            if cvsLogInfo.find(',') > -1:
                oldRevision = cvsLogInfo.split(',')[-2]
                newRevision = cvsLogInfo.split(',')[-1]

            fileDiff = getFileDiff(cvsroot, filename, oldRevision, newRevision)

            commitFilesFile.write(filename + '\n')
            commitVersionsFile.write(version + '\n')
            commitStatusesFile.write(status + '\n')

            if len(fileDiff.strip()) > 0:
                commitDiffsFile.write(fileDiff + '\n')

        commitFilesFile.close()
        commitStatusesFile.close()
        commitVersionsFile.close()
        commitDiffsFile.close()

        log.write('Last directory: %s' % lastDirectory)
        log.write('Current directory: %s' % cvsroot + '/' + moduledir)

        # If this is part of a larger commit, return
        if lastDirectory not in [cvsroot + '/' + moduledir, cvsroot, cvsroot + '/', cvsroot + '/.']:
            log.write("Continuing with the next directory's changes")
            return

    if not changeRecord.endswith(' - Imported sources'):
        commitFilesFile = open(commitFilesFilename, 'r')
        files = commitFilesFile.read().strip().split('\n')
        commitFilesFile.close()

        commitVersionsFile = open(commitVersionsFilename, 'r')
        versions = commitVersionsFile.read().strip().split('\n')
        commitVersionsFile.close()

        commitStatusesFile = open(commitStatusesFilename, 'r')
        statuses = commitStatusesFile.read().strip().split('\n')
        commitStatusesFile.close()

        commitDiffsFile = open(commitDiffsFilename, 'r')
        diff = ''

        for line in commitDiffsFile.readlines():
            if len(diff) <= MAX_DIFF_LENGTH:
                diff = diff + line

        commitDiffsFile.close()

        reffiles = [''] * len(files)
        refversions = [''] * len(files)

    # Remove trailing '\n' for better email formatting
    if diff and len(diff) > 0:
        if diff[-1] == '\n':
            diff = diff[:-1]

    scm = SourceForge.getSOAPClient("ScmListener")
    key = SourceForge.createScmRequestKey()

    try:
        diff = re.sub(r'[\x00-\x08\x0B-\x0C\x0E-\x1F\x7f-\xff]', "", diff)
        diff = unicode(diff, 'utf-8')
    except Exception, e:
        log.write('Unable to encode diff: ' + str(e) + '\n')
        diff = 'Unable to generate diff.  (Diff content is not decodable.)'

    scm.createCommit(key, user, systemid, cvsroot, logmsg, files, versions, statuses, reffiles, refversions, diff)

    # Cleanup
    if os.path.exists(lastDirectoryFilename):
        os.remove(lastDirectoryFilename)

    if os.path.exists(commitFilesFilename):
        os.remove(commitFilesFilename)

    if os.path.exists(commitStatusesFilename):
        os.remove(commitStatusesFilename)

    if os.path.exists(commitVersionsFilename):
        os.remove(commitVersionsFilename)

    if os.path.exists(commitDiffsFilename):
        os.remove(commitDiffsFilename)

try:
    log = LogFile.LogFile('/tmp/LogInfo.log')
    log.setLogging(False)
    perform(sys.argv, os.environ)
except Exception, e:
    print 'LogInfo Failed: ' + str(e)
    import traceback

    traceback.print_tb(sys.exc_info()[2], None, log)
    log.close()
    # raise
    sys.exit(1)
