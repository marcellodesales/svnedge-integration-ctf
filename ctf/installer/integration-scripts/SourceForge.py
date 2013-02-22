import os
import string
import struct
import random
import sys
import time
import base64
import urllib
import urllib2
import urlparse

from suds.client import Client

_props = {}

DEFAULT_SOURCEFORGE_PROPERTIES_PATH="/etc/sourceforge.properties"
FAKE_WINDOWS_SCMROOT = '/windows-scmroot'

# If kconv module exists, character code conversion is enabled.
# default: false(0)
try:
  import kconv
except ImportError:
  useKconv = False
else:
  useKconv = True

# decide japanese character code in string, and convert japanese character code to utf-8.
# if cannot decide code, kconv does not convert a character code.

def toutf8(txt, log):
    if useKconv:
        log.write("Using kconv to convert encoding")
        kc = kconv.Kconv(kconv.UTF8, kconv.AUTO)
        txt = kc.convert(txt)
        return txt
    else:
        try:
          txt = unicode(txt, 'utf-8')
        except Exception, e:
          try:
            txt = unicode(txt, 'windows-1252')
          except Exception, e:
            log.write("Using UniversalDetector to convert encoding")
            detector = UniversalDetector()
            detector.feed(txt)
            detector.close()
            if detector.result["encoding"] is not None:
                log.write("Detected encoding: " + detector.result["encoding"] + " confidence: " +  str(detector.result["confidence"]))
                try:
                    txt = unicode(txt, detector.result["encoding"])
                except Exception, e:
                    txt = unicode(txt, 'raw-unicode-escape')
                    log.write("Detected encoding seems to be wrong")
            else:
                txt = unicode(txt, 'raw-unicode-escape')
                log.write("Failed to detect encoding")
        return txt

def load(filename=DEFAULT_SOURCEFORGE_PROPERTIES_PATH):
    global _props

    # When filename is the default, check for the SOURCEFORGE_PROPERTIES_PATH environment variable as a potential override
    if filename == DEFAULT_SOURCEFORGE_PROPERTIES_PATH and os.environ.has_key('SOURCEFORGE_PROPERTIES_PATH'):
      filename = os.environ['SOURCEFORGE_PROPERTIES_PATH']

    propFile = open(filename, "r")
    propLine = propFile.readline()

    while propLine != '':
        if not propLine.startswith('#'):
            propInfo = propLine.split('=', 2)
            propName = propInfo[0].strip()
            if propName != '':
                propValue = propInfo[1].strip()
                _props[propName] = propValue
        propLine = propFile.readline()

    propFile.close()
    return _props

def assertProperty(propertyMap, key):
    if not key in propertyMap:
        raise Exception(key + ' must be set in sourceforge.properties')

def get(key, default = None):
    if not _props: load()
    return _props.get(key, default)

# get a property from a map, after determining that the key is present
def getRequired(key):
    if not _props: load()
    assertProperty(_props, key)
    return _props[key]

def getBoolean(key, default = None):
    return string.lower(get(key, default)) in ["1", "true", "on"]


def normalizeRepositoryPath(path):
    # CVS workaround
    if path.startswith(':local:'): path = path[7:]
    return os.path.abspath(path)

SOAP_API_DEFAULT_NAMESPACE = ''
def getDefaultSoapVersion():
    global SOAP_API_DEFAULT_NAMESPACE
    if SOAP_API_DEFAULT_NAMESPACE == '':
        try:
            cnSoap = getSOAPClient("CollabNet", "60")
            cnSoap.getApiVersion()
            SOAP_API_DEFAULT_NAMESPACE = "60"
        except Exception, e:
            SOAP_API_DEFAULT_NAMESPACE = "50"
    return SOAP_API_DEFAULT_NAMESPACE

def getSOAPClient(serviceName = 'ScmListener', soapVer = ''):
    # find the service url
    SOAPServiceUrl = getSOAPServiceUrl(serviceName, soapVer)
    
    # add proxy settings if they exist
    httpProxy = getProxyUrl()
    proxyDict = {}
    if httpProxy and len(httpProxy) > 0:
        proxyDict[getProxyProtocol(SOAPServiceUrl)] = httpProxy 
    
    # Create the client with proxy and location options    
    # We need to override the location from the WSDL which reports localhost
    scm = Client(SOAPServiceUrl + '?wsdl', location=SOAPServiceUrl, proxy=proxyDict)                                       
    return scm.service

def getSOAPServiceUrl(serviceName, soapVer = ''):
    proto = ["http", "https"] [getBoolean("sfmain.integration.listener_ssl", "false")]
    host = getRequired('sfmain.integration.listener_host')
    port = getRequired('sfmain.integration.listener_port')

    if soapVer == '':
        soapVer = getDefaultSoapVersion()

    if int(soapVer) >= 50 and serviceName == 'SourceForge':
        serviceName = 'CollabNet'

    soapPrefix = "/ce-soap"
    if int(soapVer) < 50:
        soapPrefix = "/sf-soap"
    
    if serviceName == 'ScmListener':
        if int(soapVer) == 50:
            soapPrefix = "/sf-soap"
        soapVer = ''

    return proto + "://" + host + ":" + port + soapPrefix + soapVer + "/services/" + serviceName

def getProxyProtocol(url):
    if url.startswith("https"): 
        return "https"
    else: 
        return "http" 

def getProxyUrl():
    httpProxy = None
    httpProxyHost = get('sfmain.integration.http_proxy_host')
    httpProxyPort = get('sfmain.integration.http_proxy_port')
    httpProxyUsername = get('sfmain.integration.http_proxy_username')
    httpProxyPassword = get('sfmain.integration.http_proxy_password')
    if httpProxyHost and len(httpProxyHost) > 0:
        if httpProxyUsername and len(httpProxyUsername) > 0:
            httpProxy = 'http://%s:%s@%s:%s' % (httpProxyUsername, httpProxyPassword, httpProxyHost, httpProxyPort)
        else: 
            httpProxy = 'http://%s:%s' % (httpProxyHost, httpProxyPort)
    return httpProxy

def getIntegrationServerUrl():
  """Creates a url to the integration server"""
  proto = ["http", "https"] [getBoolean("sfmain.integration.listener_ssl", "false")]
  host = getRequired('sfmain.integration.listener_host')
  port = getRequired('sfmain.integration.listener_port')

  return proto + "://" + host + ":" + port + "/integration"

SCM_TIMESTAMP_SALT = 0x34A49f41
SCM_KEY_SEED = "kaboomastringa"
SCM_DEFAULT_SHARED_SECRET = "xnaskdxy*B R^qbiwgd"

def getSha1Hash(string):
    py_ver = sys.version_info[:2]
    msg = None

    if py_ver[0] == 2 and py_ver[1] == 4:
        import sha

        msg = sha.new(string)
    elif py_ver[0] == 2 and py_ver[1] >= 5:
        import hashlib

        msg = hashlib.sha1()
        msg.update(string)
    else:
        # Unsupported version of Python
        raise Exception('Unsupported version of Python.  Python version must be ' \
                          'greater or equal to 2.4 or less than 3.0')

    return msg.digest()

def initTimestampSalt():
    global SCM_TIMESTAMP_SALT
    h = getSha1Hash(SCM_DEFAULT_SHARED_SECRET)
    SCM_TIMESTAMP_SALT = struct.unpack("i", h[0:4])[0]

def prepareScmRequestKey(rnd, timestamp):
    rndBytes = struct.pack("i", rnd)
    timeBytes = struct.pack("i", timestamp ^ SCM_TIMESTAMP_SALT)
    passwd = get("sfmain.integration.security.shared_secret", SCM_DEFAULT_SHARED_SECRET)
    hash = getSha1Hash(passwd + rndBytes + timeBytes + passwd + SCM_KEY_SEED)
    return rndBytes + timeBytes + hash #+ "13"

def createScmRequestKey():
    key = prepareScmRequestKey(random.randint(0, 2^32-1), int(time.time()))
    return base64.encodestring(key)

def getCTFWindowsRepositoryPath(path):
  """ On Windows we have a special scenario where we have to fake CTF out to
  work with Windows.  The way this is done is when we initialize an external
  system integration server, we use /windows-scmroot as the repository root
  path even though the repositories really will reside at
  <scm_type>.repository_base.  The reason for this is that CTF only works
  with Unix paths. So whenever CTF gets a full repository path as a method
  parameter, we have to ensure that our paths are /windows-scmroot/<repo_name>
  so that CTF recognizes the repository path. This function does no operating
  system checks so it is up to the consumer to decide if this function call is
  necessary. """
  return '%s/%s' % (FAKE_WINDOWS_SCMROOT, os.path.basename(path))

# getCTFWindowsRepositoryPath()

def isWindows():
  """ Returns True if os.name == 'nt' and False otherwise. """
  return os.name == 'nt'

# isWindows()

def getTemporaryDirectory():
  """ Returns the temporary directory for this system.  This can be configured in
  sourceforge.properties via the sfmain.tempdir property.  If that propery is not
  found, we then resort to using Python's mechanism for identifying the temporary
  directory. """
  tmp_dir = get('sfmain.tempdir')

  if tmp_dir is None:
    import tempfile

    tmp_dir = tempfile.gettempdir()

  return tmp_dir

# getTemporaryDirectory()

initTimestampSalt()
