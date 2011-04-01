#
# -*-python-*-
#
# ------------------------------------------------------------------------------
#
# Mod_Python handler for running ViewVC in a CollabNet TeamForge environment
#
# ------------------------------------------------------------------------------

from mod_python import apache, Cookie, util
from suds import WebFault
from suds.client import Client
from urlparse import urlparse
import os
import SourceForge
import string
import sys
import urllib
import urllib2

DEBUG = False

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

def handler(req):
  """ mod_python publisher handler.  (Script entry point.) """

  # Begin TeamForge Logic
  if DEBUG:
    req.log_error('[viewvc] Request: (%s) %s' % (req.method, req.unparsed_uri))

  _configure_environment(req)
  _prepare(req)
  _verify_session(req)
  _prepare_viewvc(req)
  # End TeamForge Logic

  try:
    module = apache.import_module('viewvc', path=[os.path.dirname(__file__)])
  except ImportError, e:
    req.log_error('[viewvc] Unable to initialize ViewVC: %s' % str(e))

    raise apache.SERVER_RETURN, apache.HTTP_NOT_FOUND

  req.add_common_vars()

  module.index(req, req.java_session)

  return apache.OK

# handler()

def _prepare(req):
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
  query_params = {}

  if req.args is not None:
    query_params = util.parse_qs(req.args)

  # Put the parsed query params into the request object
  req.query_params = query_params

  # If the request has the js and us query parameters, create the sf_auth
  # cookie and then redirect back to the same url minus the js and us query
  # parameters.
  if query_params.has_key('js') and query_params.has_key('us'):
    js = query_params['js'][0]
    us = query_params['us'][0]

    _create_sf_auth_cookie(req, js, us)

    # Redirect back to the requested url WITHOUT the js/us query parameters
    _redirect(req, _get_return_to_url(req))

  # If there was no redirect, this means there were no js/us query parameters.
  # We will now check if there is a cookie already set.
  sf_auth = _get_cookie(req, SFAUTH_COOKIE_NAME)
  js = None
  us = None

  if sf_auth is not None:
    cookie_parts = sf_auth.value.split('&')
    us = cookie_parts[0]
    js = cookie_parts[1]

  # Put the session information into the request object
  req.java_session = js
  req.user_session = us

  if us is None or js is None:
    _redirect_to_ctf_error_page(req, 'NoAuth')

# _prepare()

def _verify_session(req):
  """ Verifies that the user's session is valid and if not, redirects
  back to TeamForge with an error code that TeamForge will use in
  handling the request. This function will also stuff the ViewVC viewer
  information into the session upon successful session verification. """
  query_params = req.query_params
  root = None
  system_id = None

  if not hasattr(req, 'java_session') or not hasattr(req, 'user_session'):
    _redirect_to_ctf_error_page(req, 'NoAuth')

  if query_params.has_key('root'):
    root = query_params['root'][0]

  if query_params.has_key('system'):
    system_id = query_params['system'][0]

  try:
    scm_listener_url = SourceForge.getSOAPServiceUrl('ScmListener')
    scm = Client('%s?wsdl' % scm_listener_url,
                 location=scm_listener_url)
    response = scm.service.getViewVCInformation(req.user_session, system_id, root)

    if DEBUG:
      req.log_error('[viewvc] Requesting ViewVC information from TeamForge (%s, %s, %s) -> %s' % (req.user_session, system_id, root, repr(response)))

    proj_path = str(response[0])
    repo_root = str(response[1])
    scm_type = str(response[2])
    username = str(response[3])

    if KNOWN_SCM_ADAPTER_TYPES.has_key(scm_type):
      scm_type = KNOWN_SCM_ADAPTER_TYPES[scm_type]
    else:
      _redirect_to_ctf_error_page(req, 'InvRepo')

    # On Windows, we are given a fake repository root and we need to replace
    # that with what's in <scm_type_name>.repository_base for ViewVC to work.
    if SourceForge.isWindows():
        old_repo_root = repo_root
        repo_root = SourceForge.get('%s.repository_base' % KNOWN_SCM_ADAPTER_TYPE_NAMES[scm_type])

        if DEBUG:
            req.log_error('Converting CTF full path to Windows full path: %s->%s' % (old_repo_root, repo_root))

    req.proj_path = proj_path
    req.repo_root = repo_root
    req.scm_type = scm_type
    req.username = username
  except WebFault, e:
    faultcode = str(e.fault.faultcode)
    faultstring = str(e.fault.faultstring)

    if DEBUG:
      req.log_error('[viewvc] Exception validating session: %s' % faultstring)

    if faultcode.find('PermissionDenied') > -1:
      _redirect_to_ctf_error_page(req, 'PermDenied')
    elif faultcode.find('InvalidSessionFault') > -1:
      _redirect_to_ctf_error_page(req, 'InvSession')
    elif faultcode.find('NoSuchObject') > -1:
      _redirect_to_ctf_error_page(req, 'InvRepo')
    else:
      _redirect_to_ctf_error_page(req, 'Unk-' + faultcode.split(':')[1] + '-InvalidSession')
  except:
    import traceback

    exception = sys.exc_info()
    traceLines = traceback.format_exception(exception[0], exception[1], exception[2])

    for line in traceLines:
      req.log_error('[viewvc] %s' % line)

    raise apache.SERVER_RETURN, apache.HTTP_INTERNAL_SERVER_ERROR

# _verify_session()

def _configure_environment(req):
  """ Configures the environment for running ViewVC via mod_python
  for CollabNet TeamForge. """
  options = req.get_options()

  if options.has_key('sourceforge.home'):
    os.environ['SOURCEFORGE_HOME'] = options['sourceforge.home']

  if options.has_key('sourceforge.properties.path'):
    os.environ['SOURCEFORGE_PROPERTIES_PATH'] = options['sourceforge.properties.path']

# _configure_environment

def _get_cookie(req, name):
  """ For the given request, get the cookie with the supplied name or
  return None if no cookie can be found. """
  cookies = Cookie.get_cookies(req)

  if cookies.has_key(name):
    return cookies[name]
  else:
    return None

# _get_cookie()

def _redirect_to_ctf_error_page(req, code):
  """ Redirects the user back to TeamForge with the respective code. """
  ctf_url = _get_ctf_url(req)
  return_to_url = _externalize_url(req, _get_return_to_url(req))
  session_query_params = ''

  # When the TeamForge host and return to host are not the same, we assume
  # this is a situation where the integration server is on a remote system
  # and we put the session information into the redirect url so that the
  # ViewRepositorySourceAction can respond accordingly.  [artf66331]
  ctf_url_host = urlparse(ctf_url)[1].split(':')[0]
  return_to_url_host = urlparse(return_to_url)[1].split(':')[0]
  js = 'null'
  us = 'null'

  if hasattr(req, 'java_session') and req.java_session is not None:
    js = req.java_session

  if hasattr(req, 'user_session') and req.user_session is not None:
    us = req.user_session

  if ctf_url_host != return_to_url_host:
    session_query_params = '&js=%s&us=%s' % (js, us)

  _redirect(req, '%s/scm/do/viewRepositoryError?code=%s&url=%s%s' % (ctf_url, code, urllib.quote(return_to_url), session_query_params))

# _redirect_to_ctf_error_page()

def _get_return_to_url(req):
  """ Returns the url to return back to when redirecting back to TeamForge. """
  query_string = ''
  query_params = req.query_params

  for (key, value) in query_params.iteritems():
    if key == 'js' or key == 'us':
      continue

    query_string = query_string + '&'

    if key == 'root' or key == 'system':
      query_string = query_string + key + "=" + value[0]
    else:
      for value in query_params[key]:
        query_string = query_string + key + "=" + value

    query_string = query_string

  # Trim the first character as it's a & and is unnecessary
  query_string = query_string[1:]

  return_to_url = urllib.quote(req.subprocess_env['SCRIPT_URI'], '/:')
  return_to_url = return_to_url + '?' + query_string

  return return_to_url

# _get_return_to_url()

def _get_ctf_url(req):
  """ Returns a url that points to the TeamForge installation this integration server
  is associated with. """
  ctf_url = SourceForge.get('webserver.root-url', 'http://localhost:8080/sf')

  # Replace localhost as the hostname if the requested host is not localhost
  if ctf_url.find('//localhost') > -1 and req.hostname != 'localhost':
    ctf_url = ctf_url.replace('//localhost', '//' + req.hostname)

  # Remove the trailing slash, if any
  if ctf_url.endswith('/'):
    ctf_url = ctf_url[:-1]

  return ctf_url

# _get_ctf_url()

def _prepare_viewvc(req):
  """ Prepares ViewVC for being ran. """

  os.environ['SCM_PARENT_PATH'] = req.repo_root
  os.environ['SCM_TYPE'] = req.scm_type
  os.environ['HTTP_USER_AGENT'] = req.headers_in['User-Agent']
  req.subprocess_env['REMOTE_USER'] = req.username
  req.subprocess_env['CTF_PROJECT_PATH'] = req.proj_path
  req.subprocess_env['CTF_BASE_URL'] = _get_ctf_url(req)
  req.subprocess_env['CTF_RETURN_TO_URL'] = _get_return_to_url(req)

  if req.get_options().has_key('viewvc.root.uri'):
    viewvc_root_uri = req.get_options()['viewvc.root.uri']

    # Hack the SCRIPT_NAME to be the same as the Apache Location directive URI
    req.subprocess_env['SCRIPT_NAME'] = viewvc_root_uri

    # Hack the PATH_INFO to be the request url minus the Apache Location directive URI
    req.subprocess_env['PATH_INFO'] = req.subprocess_env['SCRIPT_URL'][len(viewvc_root_uri):]

# _prepare_viewvc()

def _is_https(req):
  """ Returns whether or not the request was made over https or not. """
  req_uri = req.subprocess_env['SCRIPT_URI']
  is_https = False

  if req_uri.startswith('https:') or req_uri.find(':443') > -1:
    is_https = True

  return is_https

# _is_https()

def _create_sf_auth_cookie(req, js, us):
  """ Creates the sf_auth cookie. """
  # Based on the way mod_python sets the secure flag of a cookie, we need to
  # have this less elegant approach.
  if _is_https(req):
    # Create a secure cookie
    sf_auth = Cookie.Cookie(SFAUTH_COOKIE_NAME, '%s&%s' % (us, js), path='/', secure=True)
  else:
    # Create a cookie
    sf_auth = Cookie.Cookie(SFAUTH_COOKIE_NAME, '%s&%s' % (us, js), path='/')

  # Add the cookie to the request
  Cookie.add_cookie(req, sf_auth)

# _create_sf_auth_cookie()

def _externalize_url(req, url):
  """ For the given url, replace 'localhost' with the actual hostname that the
  user requested. """
  ext_url = url

  if urlparse(url)[1].split(':')[0] == 'localhost':
    if req.headers_in.has_key('X-Forwarded-Host'):
      ext_url = ext_url.replace('//localhost', '//%s' % req.headers_in['X-Forwarded-Host'], 1)

  return ext_url

# _externalize_url()

def _redirect(req, url):
  """ Redirects the user to the passed in url. """
  if DEBUG:
    req.log_error('[viewvc] Redirecting to %s' % url)

  util.redirect(req, url)

# _redirect()

def _debug_request(req):
  """ Writes the request information to the Apache error log. """
  for attr in dir(req):
    req.log_error('[viewvc] %s: %s' % (attr, repr(getattr(req, attr))))

# _debug_request()
