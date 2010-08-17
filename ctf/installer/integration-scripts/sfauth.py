#!/usr/bin/env python2

#
# $RCSfile: sfauth.py,v $
#
# SourceForge(r) Enterprise Edition
# Copyright 2007 CollabNet, Inc.  All rights reserved.
# http://www.collab.net
#
# This file contains an authorization method, authenhandler(), which can
# used as an apache authentication method.

import types, string, time, threading
import os
import sys
import SOAPpy
import SourceForge
import SubversionUtil
import urllib

from mod_python import apache
from urlparse import urlparse

DEBUG = False
REPORT_THRESHOLD = 50
BRANDING_REPOSITORY_PATH = "/svn/repository-branding"
SVN_SPECIAL_URI = "!svn"

# Below is a structure that contains the special folders that can be found in a Subversion uri.
# Here is the format for each element:
#  key: The actual folder name.
#  value: Two itemed list.
#    [0]: The number of path elements that should follow this folder path in the uri.
#    [1]: Whether or not there will be a repository path following the folder and its following paths.
SVN_SPECIAL_DIRS = {
    "ver": (1, True),
    "his": (0, False),
    "wrk": (1, True),
    "act": (1, False),
    "vcc": (1, False),
    "bc": (1, True),
    "bln": (1, False),
    "wbl": (2, False),
}

METHOD_2_ACCESS_TYPE = {
    "OPTIONS": "special", # Always granted
    "GET": "view",
    "HEAD": "view",
    "PROPFIND": "view",
    "REPORT": "view",
    "DELETE": "commit",
    "MKCOL": "commit",
    "COPY": "commit",
    "MOVE": "commit",
    "CHECKOUT": "view",
    "PROPPATCH": "commit",
    "PUT": "commit",
    "MKACTIVITY": "commit",
    "MERGE": "commit",
    "LOCK": "commit",
    "UNLOCK": "commit",
}

def authzhandler(req):
    """ This method gets called by mod_python to perform authorization """
    try:
        _update_environment(req.get_options())

        return doAuthzhandler(req)
    except Exception, e:
        import traceback
        exception = sys.exc_info()
        traceLines = traceback.format_exception(exception[0], exception[1], exception[2])
        req.log_error(string.join(traceLines))
        return apache.HTTP_INTERNAL_SERVER_ERROR

def authenhandler(req):
    """ This method gets called by mod_python to perform authentication """
    try:
        _update_environment(req.get_options())

        return doAuthenhandler(req)
    except Exception, e:
        import traceback
        exception = sys.exc_info()
        traceLines = traceback.format_exception(exception[0], exception[1], exception[2])
        req.log_error(string.join(traceLines))
        return apache.HTTP_INTERNAL_SERVER_ERROR

def doAuthzhandler(req):
    """ This method queries against the sourceforge server to check if the username can access a repository path """
    
    # The response is one of the following, or an unexpected error:
    #   0 = apache.OK
    #   1 = apache.HTTP_FORBIDDEN
    #   2 = apache.HTTP_INTERNAL_SERVER_ERROR
    response = 1
    options = req.get_options()

    try:
        repo_name, repo_path = __dav_svn_parse_uri(req.uri, options['svn.root.uri'])

        # Ignore the uri for MERGE requests, with both mod_dav_svn and mod_authz_svn do.
        if req.method == 'MERGE':
            repo_path = None
    except RuntimeError, re:
        req.log_error(re)

    if DEBUG:
        req.log_error("[scm debug] (%s)%s->%s" % (req.user, req.method, req.uri))
        req.log_error("[scm debug] Repository name: %s" % repr(repo_name))
        req.log_error("[scm debug] Repository path: %s" % repr(repo_path))

    # We use the connection's notes table to store per-connection cache information.  That being said,
    # we use the 'authz_paths' to store a cache of paths we've already requested authorization for, to
    # limit the number of ScmPermissionsProxyServlet requests.  The format for this:
    #   The key uses the following format: METHOD:REPO_NAME/REPO_PATH
    #   The value is a tuple with access for: (path, path and all children, path and/or any children)
    authorized_paths = _get_from_notes(req, 'authz_paths', True)

    if authorized_paths is None:
        authorized_paths = {}

    # Only proceed if there was a repository name found
    if repo_name and repo_name != "":
        systemId = _get_from_notes(req, 'system_id')
        accessType = 'view' # Reasonable default
        global_write_access_key = '%s:global_write_access' % repo_name
        global_read_access_key = '%s:global_read_access' % repo_name
        global_write_access = _get_from_notes(req, global_write_access_key, True)
        global_read_access = _get_from_notes(req, global_read_access_key, True)

        # Retrieve the systemId if necessary and persist it
        if systemId is None:
            try:
                # It's important to reload properties, in case system ID changes (e.g. during tests)
                SourceForge.load()

                # This is a branding repo, get the external system id from ScmListener
                if req.uri.startswith(BRANDING_REPOSITORY_PATH):
                    scm = SOAPpy.SOAPProxy(SourceForge.getSOAPServiceUrl("ScmListener"))
                    key = SourceForge.createScmRequestKey()
                    systemId = scm.getBrandingExternalSystemId(key)
                else:
                    # regular svn repo: load external system id from /svnroot/.scm.properties
                    repoBase = SourceForge.get('subversion.repository_base')
                    SourceForge.load(repoBase + "/.scm.properties")
                    systemId = SourceForge.get('external_system_id')
            except Exception, inst:
                req.log_error('Failed to get external system id: ' + inst.__str__())
                return apache.HTTP_INTERNAL_SERVER_ERROR

            _add_to_notes(req, 'system_id', systemId)

        # Retrieve the global access information if necessary and persist it
        if global_write_access is None or global_read_access is None:
            global_write_raw, global_read_raw = SourceForge.getScmPermissionForPath(req.user, systemId,
                                                                                    repo_name + '/', None)

            if DEBUG:
                req.log_error("[scm debug] [ScmPermissionsProxy]: (%s, %s, %s, %s) -> %s" % (req.user, systemId,
                                                                                             repo_name + '/',
                                                                                             None,
                                                                                             repr((global_write_raw,
                                                                                                   global_read_raw))))

            if global_write_raw == 0:
                global_write_access = True
            else:
                global_write_access = False

            if global_read_raw == 0:
                global_read_access = True
            else:
                global_read_access = False

            _add_to_notes(req, global_write_access_key, global_write_access, True)
            _add_to_notes(req, global_read_access_key, global_read_access, True)

        # Retrieve the required access type
        if not METHOD_2_ACCESS_TYPE.has_key(req.method):
            req.log_error('Unknown method: ' + req.method)
            return apache.HTTP_INTERNAL_SERVER_ERROR
        else:
            # There are a few scenarios that should be mentioned, since they are special:
            #     COPY: When a Subversion copy request comes in, the actual uri in the
            #           Subversion request is the copy source.  The copy source only needs
            #           to have recursive 'read' access.  The destination is in the
            #           'Destination' request header.  The destination needs to have
            #           recursive 'commit' access.  The destination is handled later.
            #
            #     MOVE: Move is just like copy, when it comes to source and destination,
            #           from a Subversion perspective.  From a TeamForge permissioning
            #           perspective, both the source and the destionation need
            #           recursive 'commit' access.
            if req.method == "COPY":
                accessType = 'view'
            else:
                accessType = METHOD_2_ACCESS_TYPE[req.method]

        if global_write_access:
            # The user has global write access.  No need to authorize.
            response = 0

            if DEBUG:
                req.log_error("[scm debug] [from global]: 'commit'")
        elif global_read_access and accessType == 'view':
            # If the user has global read and this is a read request
            response = 0
            
            if DEBUG:
                req.log_error("[scm debug] [from global]: 'read'")
        elif req.method == 'OPTIONS':
            # Always allow 'OPTIONS' requests
            response = 0
        elif req.method == 'PROPPATCH' and req.uri.startswith("%s/%s/%s/%s" % (options['svn.root.uri'], repo_name, SVN_SPECIAL_URI, "bln")):
            # We need to handle authorization of revision property changes specially
            repository = "%s/%s" % (options['svn.root.path'], repo_name)
            revision = req.uri.split('/')[-1]

            try:
                revision = int(revision)
            except ValueError:
                req.log_error("%s is not a valid revision number" % revision)
                return apache.HTTP_INTERNAL_SERVER_ERROR

            if DEBUG:
                req.log_error("User %s is attempting to change a revision property for revision %s" % (req.user, revision))

            # For authorizing a revision property change, all we need to do is
            # get a list of changed paths for the revision, regardless of change,
            # and verify that the changing user has write permission for the path.
            svnlook = SubversionUtil.createSVNLook(repository, rev=revision)
            changes = SubversionUtil.generalizeChanges(svnlook.changed())

            # Authorize each path modified in the revision
            for change in changes:
                response = authorize_request(req, authorized_paths, systemId, repo_name, change[2], "commit")

                # If there is an authorization failure, break since revision property changes require
                # 'commit' access to all changed paths for the revision.
                if response != 0:
                    break
        else:
            response = authorize_request(req, authorized_paths, systemId, repo_name, repo_path, accessType)

        # Check the destination path if the method is 'COPY' or 'MOVE' if
        # the previous check, which is the source, was permitted.
        if response == 0 and req.method in ["COPY", "MOVE"]:
            if not req.headers_in.has_key('Destination'):
                req.log_error("Invalid request: %s does not have a destination" % req.method)
                return apache.HTTP_INTERNAL_SERVER_ERROR

            dest_url = req.headers_in['Destination']
            dest_uri = urlparse(dest_url)[2] # Get the path, which is the destination

            # If this is a subrequest and the source/destination uris are the same,
            # skip this request.  This scenario can be caused by mod_dav_svn doing some
            # uri lookup.
            if req.main and (dest_url == req.parsed_uri[apache.URI_PATH]):
                if DEBUG:
                    req.log_error("%s subrequest has matching source and destination (%s)" % (req.method, dest_uri))
                    
            # Decode the uri
            dest_uri = urllib.unquote(dest_uri)

            try:
                dest_repo_name, dest_repo_path = __dav_svn_parse_uri(dest_uri, options['svn.root.uri'])
            except RuntimeError, re:
                req.log_error(re)

            if DEBUG:
                req.log_error("[scm debug] Destination repository name: %s" % repr(dest_repo_name))
                req.log_error("[scm debug] Destination repository path: %s" % repr(dest_repo_path))

            response = authorize_request(req, authorized_paths, systemId, dest_repo_name, dest_repo_path, "commit")

    if response == 0:
        if DEBUG:
            req.log_error("[scm debug]: Response 200")

        return apache.OK
    elif response == 1:
        if DEBUG:
            req.log_error("[scm debug]: Response 403")

        return apache.HTTP_FORBIDDEN
    else:
        req.log_error("[scm debug]: Unexpected response: %s.  Check the integration server logs." % response)
        req.log_error("[scm debug]: Response 500")

        return apache.HTTP_INTERNAL_SERVER_ERROR

def doAuthenhandler(req):
    """ This method queries against the sourceforge server to check is the username/password is valid """
    authenticated_users = _get_from_notes(req, 'authn_cache')
    is_valid_user = False

    # As documented in mod_python, before you can successfully call req.user you must call
    # req.get_basic_auth_pw().
    # http://modpython.org/live/current/doc-html/pyapi-mprequest-mem.html#l2h-124
    password = req.get_basic_auth_pw()
    username = req.user

    # Quick return for anonymous user, since it's not supported right now.
    if username is None or password is None:
        return apache.HTTP_UNAUTHORIZED

    uname_pwd_hash = SourceForge.getSha1Hash('%s:%s' % (username, password))

    if authenticated_users is not None and uname_pwd_hash in authenticated_users:
        is_valid_user = True

    # If the user isn't in the authenticated users cache, attempt to authenticate by querying
    # the application server.
    if not is_valid_user:
        scm = SOAPpy.SOAPProxy(SourceForge.getSOAPServiceUrl("ScmListener"))

        try:
            key = SourceForge.createScmRequestKey()
            response = scm.isValidUser(key, username, password)

            if response == 0:
                is_valid_user = True
            elif response == 1:
                is_valid_user = False
            else:
                req.log_error("[scm debug]: Unexpected response '%s'" % str(response))

                return apache.HTTP_INTERNAL_SERVER_ERROR
        except Exception, inst:
            req.log_error("Failed to authenticate user '%s': %s" % (username, inst.__str__()))

            return apache.HTTP_INTERNAL_SERVER_ERROR

    if is_valid_user:
        if authenticated_users is None:
            authenticated_users = []

        # Only update the cache if the user is missing from it
        if not uname_pwd_hash in authenticated_users:
            authenticated_users.append(uname_pwd_hash)

            # Cache the authenticated users in the connection's notes table
            _add_to_notes(req, 'authn_cache', authenticated_users)

        req.headers_out.add('CtfUserName',username)
        # Now authorize the user
        return authzhandler(req);
    else:
        return apache.HTTP_UNAUTHORIZED

def _update_environment(options):
    """ For the given mod_python notes, check for sourceforge.properties.path and if present
    create the SOURCEFORGE_PROPERTIES_PATH from its value. """
    if options.has_key('sourceforge.properties.path'):
        os.environ['SOURCEFORGE_PROPERTIES_PATH'] = options['sourceforge.properties.path']

def _get_from_notes(req, key, user_specific=False):
    """ Return the value stored in Apache's connection notes for the given key
        or None if the key isn't present.  If user_specific=True, the key will
        be prepended with the username so that any retrieval of the value will
        be user specific. """
    real_key = key

    if user_specific:
        real_key = "%s:%s" % (req.user, key)

    if req.connection.notes.has_key(real_key):
        return eval(req.connection.notes[real_key])
    else:
        return None

def _add_to_notes(req, key, value, user_specific=False):
    """ Adds the value to the Apache connection notes for the given key.  If
        user_specific=True, the key will be prepended with the username so that
        any retrieval of the value will be user specific. """
    real_key = key

    if user_specific:
        real_key = "%s:%s" % (req.user, key)

    req.connection.notes[real_key] = repr(value)

def _get_authz_from_cache(authorized_paths, repo_name, repo_path, accessType, method):
    """ Attempts to take a dictionary of authorized paths and make an authorization answer. """
    authz = None
    read_paths = []
    write_paths = []
    needs_all = False
    needs_any = False
    full_repo_path = repo_name + '/'

    if repo_path is not None:
        full_repo_path = full_repo_path + repo_path

    # Handle special cases for COPY/MOVE/DELETE
    if method in ['COPY', 'DELETE', 'MOVE']:
        needs_all = True

    if repo_path is None:
        needs_any = True

    # Create a list of paths, based on access type, for easier use
    for k, v in authorized_paths.iteritems():
        p, t = k.split(":")

        if t == 'view' and p not in read_paths:
            read_paths.append(p)
        elif t == 'commit' and p not in write_paths:
            write_paths.append(p)

    authz = _process_path_in_cache(authorized_paths, read_paths, write_paths, full_repo_path, accessType, False,
                                   needs_all, needs_any, method)

    # Path in question has never been authorized.  Have one of its parents that can help?
    if authz is None:
        path_elements = full_repo_path.split("/")

        # For each parent path, check for its existence in the read/write paths and return
        # authz accordingly.
        for n in range(1, len(path_elements)):
            p_path = "/".join(path_elements[:0 - n])

            if len(path_elements) == 2 and n == 1:
                p_path = p_path + "/"

            if p_path not in read_paths and p_path not in write_paths:
                continue

            p_authz = _process_path_in_cache(authorized_paths, read_paths, write_paths, p_path, accessType, True,
                                             needs_all, needs_any, method)

            if p_authz is not None:
                authz = p_authz

                break

    return authz

def _process_path_in_cache(authorized_paths, read_paths, write_paths, path, accessType, is_parent, needs_all, needs_any,
                           method):
    """Process the path in the cache to see if we can answer an authz question from cache"""
    authz = None
    cached_read = path in read_paths
    cached_write = path in write_paths
    read_path = read_path_all = read_path_any = None
    write_path = write_path_all = write_path_any = None
    
    if cached_read:
        read_path, read_path_all, read_path_any = authorized_paths[path + ":view"]

    if cached_write:
        write_path, write_path_all, write_path_any = authorized_paths[path + ":commit"]

    # Handle view short-circuit (Handles view, view all and view any)
    if accessType == 'view' and (read_path_all == 0 or write_path_all == 0):
        return 0

    # Handle write short-circuit (Handle commit, commit all and commit any)
    if accessType == 'commit' and write_path_all == 0:
        return 0
    
    # Handle view all denied from cache
    if needs_all and accessType == 'view':
        if (read_path_all is not None and read_path_all != 0):
            authz = 1

    # Handle view any from cache
    if needs_any and accessType == 'view':
        if read_path_any == 0 or write_path_any == 0:
            authz = 0

    # Handle view any denied from cache
    if needs_any and accessType == 'view':
        if (read_path_any is not None and read_path_any != 0):
            authz = 1

    # Handle view from cache
    if not needs_all and not needs_any and accessType == 'view':
        if not is_parent and (read_path == 0 or write_path == 0):
            authz = 0

    # Handle view denied from cache
    if not needs_all and not needs_any and accessType == 'view':
        if not is_parent and (read_path is not None and read_path != 0):
            authz = 1
        elif is_parent and (read_path_any is not None and read_path_any != 0):
            authz = 1

    # Handle commit all denied from cache
    if needs_all and accessType == 'commit':
        if (read_path_any is not None and read_path_any != 0) or \
           (write_path_any is not None and write_path_any != 0):
            authz = 1

    # Handle commit any from cache
    if needs_any and accessType == 'commit':
        if write_path_any == 0:
            authz = 0

    # Handle commit any denied from cache
    if needs_any and accessType == 'commit':
        if (read_path_any is not None and read_path_any != 0) or \
           (write_path_any is not None and write_path_any != 0):
            authz = 1

    # Handle commit from cache
    if not needs_all and not needs_any and accessType == 'commit':
        if not is_parent and write_path == 0:
            authz = 0

    # Handle commit denied from cache
    if not needs_all and not needs_any and accessType == 'commit':
        if not is_parent and ((write_path is not None and write_path != 0) or \
                              (read_path is not None and read_path != 0)):
            authz = 1
        elif is_parent and ((read_path_any is not None and read_path_any != 0) or \
                            (write_path_any is not None and write_path_any != 0)):
            authz = 1
    
    return authz

def __dav_svn_parse_uri(uri_to_split, svn_uri_root):
    """ Takes a request uri and does some magic to return the repository and relative repository path

        Returns:
          svn_repo_name - Either the repository name or None to signify there was a problem with the uri.
          svn_repo_path - Either the relative (no leading slash) repository path, '' to signify the root
                          or None to signify there is no repository root.
    """

    svn_repo_name = None
    svn_repo_path = None

    relative_uri = None

    # Copy the uri
    uri = uri_to_split

    # Remove double slashes
    uri = uri.replace("//", "/")

    # Remove the trailing slash
    if uri[-1] == '/':
        uri = uri[:-1]

    # Find the relative uri
    uri_parts = uri.split(svn_uri_root, 1)

    # Only trim the svn_uri_root from the front if there is more to the uri than the svn_uri_root
    relative = uri.split(svn_uri_root, 1)[1]

    # We want a leading slash
    if relative[0] != '/':
        relative = '/' + relative

    # Get the repository name
    svn_repo_name = relative.split('/')[1]

    # Strip the repo name, and leading slash
    relative = relative.split(svn_repo_name, 1)[1]

    # Remove the leading slash, if there is one
    if len(relative) > 0 and relative[0] == '/':
        relative = relative[1:]

    # Get the repository path
    if relative.find(SVN_SPECIAL_URI) == -1:
        svn_repo_path = relative
    else:
        # Remove the !svn/ part
        relative = relative.split(SVN_SPECIAL_URI + '/', 1)[1]

        # Invalid uri
        if len(relative) == 0:
            raise RuntimeError("Nothing follows the svn special_uri.")

        # Special folder name
        special_folder = relative.split('/', 1)[0]

        if SVN_SPECIAL_DIRS.has_key(special_folder):
            # Validate that the special uri
            if len(relative.split(special_folder, 1)[1]) == 0:
                if not SVN_SPECIAL_DIRS[special_folder][0] != 0:
                    raise RuntimeError("Missing info after special_uri.")
            elif relative.split(special_folder, 1)[1][0] == '/':
                min_paths = SVN_SPECIAL_DIRS[special_folder][0] + 1 # We need to account for the special folder
                has_repo_path = SVN_SPECIAL_DIRS[special_folder][1]
                rel_parts = relative.split('/')

                if len(rel_parts) < min_paths:
                    raise RuntimeError("Not enough components after special_uri.")
                elif len(rel_parts) == min_paths:
                    # Either there is no repo path or it points to root
                    if has_repo_path:
                        svn_repo_path = ''
                else:
                    # Get repo path
                    svn_repo_path = '/'.join(rel_parts[min_paths:])
            else:
                raise RuntimeError("Missing info after special_uri.")
        else:
            # Invalid uri
            raise RuntimeError("Unknown data after special_uri.")

    return (svn_repo_name, svn_repo_path)

def authorize_request(req, authorized_paths, systemId, repo_name, repo_path, accessType):
    """ For the given request, authorize from cache if available or call the servlet
        to get the authorization information. """
    response = 1
    cached_answer = False
    full_repo_path = repo_name + '/'

    # Append the repository path if present.
    if repo_path is not None:
        full_repo_path = full_repo_path + repo_path

    path_key = full_repo_path + ":" + accessType

    # Retrieve from cache, if availble
    if authorized_paths is not None and len(authorized_paths) > 0:
        # Check the cache to see if there is a way to determine an answer
        t_response = _get_authz_from_cache(authorized_paths, repo_name, repo_path, accessType, req.method)

        if DEBUG:
            req.log_error("[scm debug] [from cache]: (%s, %s, %s, %s) -> %s" % (repr(authorized_paths),
                                                                                full_repo_path,
                                                                                accessType, req.method,
                                                                                t_response))

        if t_response is not None:
            cached_answer = True

            response = t_response

    if not cached_answer:
        # Query for permissions
        response, all_response, any_response = SourceForge.getScmPermissionForPath(req.user, systemId,
                                                                                   full_repo_path,
                                                                                   accessType)

        if DEBUG:
            req.log_error("[scm debug] [ScmPermissionsProxy]: (%s, %s, %s, %s) -> %s" % (req.user, systemId,
                                                                                         full_repo_path,
                                                                                         accessType,
                                                                                         repr((response,
                                                                                               all_response,
                                                                                               any_response))))

        # Cache the successful result
        authorized_paths[path_key] = (response, all_response, any_response)

        # For COPY, DELETE and MOVE, we need the 'all' permission
        if req.method in ['COPY','DELETE','MOVE']:
            response = all_response
        elif repo_path is None:
            response = any_response

    _add_to_notes(req, 'authz_paths', authorized_paths, True)    

    return response
