# Test script for ScmListenerService class

import SOAPpy
import SourceForge

def testCheckPermission(user='jgray', password='foobar', repositoryDirectory='/svnroot/rep1'):
    """ This method queries against the sourceforge server to check a users access to a directory """

    systemId = SourceForge.getRequired('external_system_id')

    scm = SOAPpy.SOAPProxy(SourceForge.getSOAPServiceUrl("ScmListener"))

    try:
        print "Making checkPermission() call"
        key = SourceForge.createScmRequestKey()
        permissionResult = scm.checkPermission(key, "PUT", user, password, systemId, repositoryDirectory)
    except Exception, inst:
        print "Failed to get permissions: " + inst.__str__()
        return 1

    print "checkPermission() returned: " + str(permissionResult)

if __name__ == '__main__':
    testCheckPermission()
