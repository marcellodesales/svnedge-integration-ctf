#!/usr/bin/env python
# -*-python-*-
#
# ------------------------------------------------------------------------------
#
# Mod_Python handler for running ViewVC in a CollabNet TeamForge environment
#
# ------------------------------------------------------------------------------

SFAUTH_COOKIE_NAME = 'sf_auth'
KNOWN_SCM_ADAPTER_TYPES = {
  'Subversion': 'svn',
  'WANdiscoSubversion': 'svn',
  'CVS': 'cvs',
  'CVS_PSERVER': 'cvs',
  'WANdiscoCVS': 'cvs',
  'Clearcase': 'clearcase',
}

KNOWN_SCM_ADAPTER_TYPE_NAMES = {
  'svn': 'subversion',
  'cvs': 'cvs',
  'clearcase': 'clearcase',
}

LIBRARY_DIR = None
VIEWVC_LIBRARY_DIR = None
SVN_LIBRARY_DIR = None
CSVN_HOME_DIR = None
CONF_PATHNAME = None
INTEGRATION_DIR = None

#########################################################################
#
# Adjust sys.path to include our library directory
#

import sys
import os
import cgi
from urlparse import urlparse
import Cookie
import string
import urllib
import urllib2

DEBUG = False

if DEBUG:
  import cgitb
  cgitb.enable()

CSVN_HOME_DIR = os.getenv("CSVN_HOME")
if CSVN_HOME_DIR:
  LIBRARY_DIR = os.path.abspath(os.path.join(CSVN_HOME_DIR, "lib"))
  SVN_LIBRARY_DIR = os.path.abspath(os.path.join(LIBRARY_DIR,
                                    "svn-python"))
  VIEWVC_LIBRARY_DIR = os.path.abspath(os.path.join(LIBRARY_DIR,
                                       "viewvc"))
  CONF_PATHNAME   = os.path.abspath(os.path.join(CSVN_HOME_DIR,
                                    "data", "conf", "viewvc.conf"))
  INTEGRATION_DIR = os.path.abspath(os.path.join(LIBRARY_DIR,
                                    "integration"))

  sys.path.insert(0, LIBRARY_DIR)
  sys.path.insert(0, VIEWVC_LIBRARY_DIR)
  sys.path.insert(0, SVN_LIBRARY_DIR)

if INTEGRATION_DIR and os.path.exists(INTEGRATION_DIR):
  sys.path.append(INTEGRATION_DIR)

from suds import WebFault
import SourceForge

def _prepare(params):
  """ Prepares for handling the request.  There are two possible scenarios for
  this script:

    [1]: If there are session query parameters available, create/overwrite the
         sf_auth cookie with their information and then redirect back to the
         requested URL without those parameters in the query string.
    [2]: If there are no session query parameters available, get the session
         information from the sf_auth cookie and stuff them into the request
         object.  (If there is no sf_auth cookie, redirect back to TeamForge.)

  Whenever scenario #2 occurs, req.java_session and req.user_session will be
  set to the proper values in the sf_auth cookie. """

  # If the request has the js and us query parameters, create the sf_auth
  # cookie and then redirect back to the same url minus the js and us query
  # parameters.
  if 'js' in params and 'us' in params:
    js = params['js'].value
    us = params['us'].value

    _create_sf_auth_cookie(js, us)

    # Redirect back to the requested url WITHOUT the js/us query parameters
    _redirect(_get_return_to_url(params))

  # If there was no redirect, this means there were no js/us query parameters.
  # We will now check if there is a cookie already set.
  sf_auth = _get_cookie(SFAUTH_COOKIE_NAME)
  js = None
  us = None

  if sf_auth is not None:
    cookie_parts = sf_auth.split('&')
    us = cookie_parts[0]
    js = cookie_parts[1]

  if us is None or js is None:
    _redirect_to_ctf_error_page(None, params, 'NoAuth')

  # session information
  return { 'java_session': js, 'user_session': us }

# _prepare()

def _verify_session(session_ids, params):
  """ Verifies that the user's session is valid and if not, redirects
  back to TeamForge with an error code that TeamForge will use in
  handling the request. This function will also stuff the ViewVC viewer
  information into the session upon successful session verification. """
  root = None
  system_id = None

  if not session_ids.get('java_session') or not session_ids.get('user_session'):
    _redirect_to_ctf_error_page(session_ids, params, 'NoAuth')

  if 'root' in params:
    root = params.getlist('root')[0]

  if 'system' in params:
    system_id = params.getlist('system')[0]

  try:
    scm = SourceForge.getSOAPClient("ScmListener")
    response = scm.getViewVCInformation(session_ids['user_session'], system_id, root)

    if DEBUG:
      log_error('[viewvc] Requesting ViewVC information from TeamForge (%s, %s, %s) -> %s' % (session_ids['user_session'], system_id, root, repr(response)))

    proj_path = str(response[0])
    repo_root = str(response[1])
    scm_type = str(response[2])
    username = str(response[3])

    if KNOWN_SCM_ADAPTER_TYPES.has_key(scm_type):
      scm_type = KNOWN_SCM_ADAPTER_TYPES[scm_type]
    else:
      _redirect_to_ctf_error_page(session_ids, params, 'InvRepo')

    # On Windows, we are given a fake repository root and we need to replace
    # that with what's in <scm_type_name>.repository_base for ViewVC to work.
    if SourceForge.isWindows():
        old_repo_root = repo_root
        repo_root = SourceForge.get('%s.repository_base' % KNOWN_SCM_ADAPTER_TYPE_NAMES[scm_type])

        if DEBUG:
            log_error('Converting CTF full path to Windows full path: %s->%s' % (old_repo_root, repo_root))

    return { 'proj_path': proj_path, 'repo_root': repo_root, 
             'scm_type': scm_type, 'username': username }
  except WebFault, e:
    faultcode = str(e.fault.faultcode)
    faultstring = str(e.fault.faultstring)

    if DEBUG:
      log_error('[viewvc] Exception validating session: %s' % faultstring)

    if faultcode.find('PermissionDenied') > -1:
      _redirect_to_ctf_error_page(session_ids, params, 'PermDenied')
    elif faultcode.find('InvalidSessionFault') > -1:
      _redirect_to_ctf_error_page(session_ids, params, 'InvSession')
    elif faultcode.find('NoSuchObject') > -1:
      _redirect_to_ctf_error_page(session_ids, params, 'InvRepo')
    else:
      _redirect_to_ctf_error_page(session_ids, params, 'Unk-' + faultcode.split(':')[1] + '-InvalidSession')
  except:
    import traceback

    exception = sys.exc_info()
    traceLines = traceback.format_exception(exception[0], exception[1], exception[2])

    for line in traceLines:
      log_error('[viewvc] %s' % line)

    print "Status: 500 Internal Server Error"
    print ""
    exit(-1)

# _verify_session()

def _get_sf_header(java_session):
  return_to_url = os.getenv('CTF_RETURN_TO_URL')
  ctf_url = os.getenv('CTF_BASE_URL')
  
  """ Calls the topInclude url to get the header contents of CTF. """
  top_include_url = '%s/sfmain/do/topInclude/%s;jsessionid=%s?base=%s&returnTo=%s&helpTopicId=26' % (ctf_url, os.getenv('CTF_PROJECT_PATH'), 
          java_session, urllib.quote_plus(ctf_url[0:ctf_url.rfind('/')]), urllib.quote_plus(return_to_url))

  return urllib2.urlopen(top_include_url).read().strip()

# _get_sf_header()

def _get_cookie(name):
  """ For the given request, get the cookie with the supplied name or
  return None if no cookie can be found. """
  try:
    cookie = Cookie.SimpleCookie(os.environ["HTTP_COOKIE"])
    return cookie[name].value
  except (Cookie.CookieError, KeyError):
    log_error("session cookie not set!")
    return None

# _get_cookie()

def _redirect_to_ctf_error_page(session_ids, params, code):
  """ Redirects the user back to TeamForge with the respective code. """
  ctf_url = _get_ctf_url()
  return_to_url = _get_return_to_url(params)
  session_query_params = ''

  # When the TeamForge host and return to host are not the same, we assume
  # this is a situation where the integration server is on a remote system
  # and we put the session information into the redirect url so that the
  # ViewRepositorySourceAction can respond accordingly.  [artf66331]
  ctf_url_host = urlparse(ctf_url)[1].split(':')[0]
  return_to_url_host = urlparse(return_to_url)[1].split(':')[0]
  js = 'null'
  us = 'null'

  if session_ids is not None and session_ids.get('java_session') is not None:
    js = session_ids['java_session']

  if session_ids is not None and session_ids.get('user_session') is not None:
    us = session_ids['user_session']

  if ctf_url_host != return_to_url_host:
    session_query_params = '&js=%s&us=%s' % (urllib.quote_plus(js), urllib.quote_plus(us))

  _redirect('%s/scm/do/viewRepositoryError?code=%s&url=%s%s' % (ctf_url, code, urllib.quote_plus(return_to_url), session_query_params))

# _redirect_to_ctf_error_page()

def _get_return_to_url(query_params):
  """ Returns the url to return back to when redirecting back to TeamForge. """
  query_string = ''

  for key in query_params.keys():
    if key == 'js' or key == 'us':
      continue

    for value in query_params.getlist(key):
      query_string = query_string + key + "=" + value + '&'

  # Trim the last character as it's a & and is unnecessary
  query_string = query_string[:-1]

  return_to_url = urllib.quote(os.getenv('SCRIPT_URI', 'SCRIPT_URI_UNDEFINED'), '/:')
  return_to_url = return_to_url + '?' + query_string

  return return_to_url

# _get_return_to_url()

def _get_ctf_url(hostname = None):
  """ Returns a url that points to the TeamForge installation this integration server
  is associated with. """
  ctf_url = SourceForge.get('webserver.root-url', 'http://localhost:8080/sf')

  # Replace localhost as the hostname if the requested host is not localhost
  if ctf_url.find('//localhost') > -1 and hostname != None and hostname != 'localhost':
    ctf_url = ctf_url.replace('//localhost', '//' + hostname)

  # Remove the trailing slash, if any
  if ctf_url.endswith('/'):
    ctf_url = ctf_url[:-1]

  return ctf_url

# _get_ctf_url()

def _prepare_viewvc(info, params, session_ids):
  """ Prepares ViewVC for being ran. """

  os.environ['SCM_PARENT_PATH'] = info['repo_root']
  os.environ['SCM_TYPE'] = info['scm_type']
  os.environ['REMOTE_USER'] = info['username']
  os.environ['CTF_PROJECT_PATH'] = info['proj_path']
  os.environ['CTF_BASE_URL'] = _get_ctf_url(os.getenv('SERVER_NAME'))
  os.environ['CTF_RETURN_TO_URL'] = _get_return_to_url(params)
  #os.environ['HTTP_USER_AGENT'] = req.headers_in['User-Agent']

#  if req.get_options().has_key('viewvc.root.uri'):
#    viewvc_root_uri = req.get_options()['viewvc.root.uri']
#
#    # Hack the SCRIPT_NAME to be the same as the Apache Location directive URI
#    req.subprocess_env['SCRIPT_NAME'] = viewvc_root_uri
#
#    # Hack the PATH_INFO to be the request url minus the Apache Location directive URI
#    req.subprocess_env['PATH_INFO'] = req.subprocess_env['SCRIPT_URL'][len(viewvc_root_uri):]

    # CollabNet TeamForge customization
  os.environ['CTF_TIMEZONE'] = SourceForge.get('ctf.displayTimezone', '');
  try:
    if int(SourceForge.getDefaultSoapVersion()) >= 60:
      scm = SourceForge.getSOAPClient("PluggableApp")
      prefixes = scm.getIntegratedAppPrefixes(session_ids['user_session'])
      if len(prefixes) > 0:
        regexSnippet = ''
        for prefix in prefixes:
          regexSnippet = regexSnippet + '|' + str(prefix) + '_'
          
        # Case-insensitive matches
        regexSnippet = '(?i)' + regexSnippet + r'(\w+)'
        os.environ['INTEGRATED_APP_PREFIXES'] = regexSnippet
  except Exception, e:
    log_error('[viewvc] Exception when calling getIntegratedAppPrefixes: %s' % str(e))

# _prepare_viewvc()

def _is_https():
  """ Returns whether or not the request was made over https or not. """
  return os.getenv('SCRIPT_URI').upper().startswith('HTTPS')

# _is_https()

def _create_sf_auth_cookie(js, us):
  """ Creates the sf_auth cookie. """

  sf_auth = Cookie.SimpleCookie()
  sf_auth[SFAUTH_COOKIE_NAME] = '%s&%s' % (us, js)
  sf_auth[SFAUTH_COOKIE_NAME]['path'] = '/'
  if _is_https():
    sf_auth[SFAUTH_COOKIE_NAME]['secure'] = True

  # Add the cookie to the response
  print sf_auth

# _create_sf_auth_cookie()

def _redirect(url):
  """ Redirects the user to the passed in url. """
  if DEBUG:
    log_error('[viewvc] Redirecting to %s' % url)

  print "Status: 302 Found"
  print "Location: %s" % url
  print ""
  exit(0)
# _redirect()

def log_error(s):
  sys.stderr.write(s)

if DEBUG:
  log_error('[viewvc] Request: (%s) %s %s' % (os.getenv('REQUEST_METHOD'), 
            os.getenv('SCRIPT_URI'), os.getenv('REQUEST_URI')))

params = cgi.FieldStorage()
session_ids = _prepare(params)
viewvc_info =_verify_session(session_ids, params)
_prepare_viewvc(viewvc_info, params, session_ids)

# go do the work
import sapi
import viewvc

server = sapi.CgiServer()
cfg = viewvc.load_config(CONF_PATHNAME, server)
java_session = session_ids['java_session']
cfg.general.header_html = _get_sf_header(java_session)
viewvc.main(server, cfg)

