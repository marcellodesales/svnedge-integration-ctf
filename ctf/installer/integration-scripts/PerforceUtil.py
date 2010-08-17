import string
import os

def parsePerforceDescribe(output, commandParams, duringCommit):
    parts = output.split('\n')
    parts.pop(0) # skip the line Change X by foo@bar on YYY
    parts.pop(0) # skip the first blank line
    thisline = parts.pop(0)
    logmsg = ''
    while not thisline.startswith("Affected files ..."):
        logmsg = logmsg + thisline.strip() + ' '
        thisline = parts.pop(0)
    thisline = parts.pop(0) # skip first blank line after Affected files ...

    if duringCommit == 'false':
        return {'logmsg' : unicode(logmsg.strip(), 'utf-8') }

    files = []
    commits = []
    statuses = []
    versions = []
    reffiles = []
    refversions = []

    # statusMap is used to map python's statuses to statuses expected by SF
    statusMap = {'delete': 'Deleted', 'add': 'Added', 'edit': 'Modified', 'branch': 'Copied'}

    while len(parts) > 0:
        thisline = parts.pop(0)
        if len(thisline.strip()) < 1:
            continue # skip blank lines
        # We take a slice from char 4 to get rid of the leading '... '
        lastspace = string.rindex(thisline, ' ')
        lastpound = string.rindex(thisline, '#')
        # get full path name including // and depot to be used by 'p4 filelog'
        fullPathFilename = thisline[4:lastpound]
        # stripping out the '//' and the depot name to get the file path
        end_of_depot_index = string.index(thisline, '/', 6) + 1
        filename = thisline[end_of_depot_index:lastpound]
        action = thisline[lastspace + 1:]
        version = thisline[lastpound + 1: lastspace]
        files.append(unicode(filename, 'utf-8'))
        versions.append(version)

        # Here we try to map the status to SF's.  If not found, plug in perforce's status for now
        status = statusMap.get(action, action)
        statuses.append(status)
        # if the status is 'Copied', then we need to figure out the real 'reffile'
        if status == 'Copied':
            command = commandParams + " filelog " + fullPathFilename

            pfd = os.popen(command)
            filelogout = pfd.read()
            returncode = pfd.close()
            if returncode != None:
                raise Exception('p4 filelog exited abnormally: returncode=' + str(returncode) +
                                ' command=[' + command + ']' +
                                ' output=' + filelogout)
            filelogdata = parsePerforceFilelog(filelogout)
            reffile = filelogdata['reffile']
            reffiles.append(unicode(reffile, 'utf-8'))
            refversion = filelogdata['refversion']
            refversions.append(refversion)
        else:
            reffiles.append(unicode('', 'utf-8'))
            refversions.append('')

    return {'logmsg' : unicode(logmsg.strip(), 'utf-8'), 'files' : files, 'versions' : versions, 'statuses' : statuses, 'reffiles' : reffiles, 'refversions' : refversions}

def parsePerforceFilelog(output):
    parts = output.split('\n')
    # skip the line: //[depot]/[filename]
    thisline = parts.pop(0)

    # might have to deal with situations other than branch
    while not thisline.startswith("... ... branch from "):
    	thisline = parts.pop(0)

    #if len(thisline.strip()) < 1:
    # some kind of exception?

    first_pound = string.index(thisline, '#')
    last_pound = string.rindex(thisline, '#')
    end_of_depot_index = string.index(thisline, '/', 22) + 1

    # stripping out the '... ... //' and the depot name to get the file path
    filename = thisline[end_of_depot_index:first_pound]

    # now getting the version number that we copy from
    refversion = thisline[last_pound + 1:]

    return {'reffile' : filename, 'refversion' : refversion}
