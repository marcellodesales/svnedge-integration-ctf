#!/usr/bin/env python2

# SourceForge(r) Enterprise Edition
# Copyright 2007-2011 CollabNet, Inc.  All rights reserved.
# http://www.collab.net
#
# This file contains an authentication and an authorization handler
# for Apache to be used via mod_python.  Right now, our mod_python
# configuration only utilizes the authentication directly with the
# authorization handler being invoked when the authentication
# handler succeeds.

import datetime
import os
import sys
import SOAPpy
import SourceForge
import SubversionUtil
import urllib

from mod_python import apache
from urlparse import urlparse

DEBUG = False

SVN_SPECIAL_URI = "!svn"

# Having a cache is imperative for performance. The following settings
# can be configured to optimize throughput and performance for Subversion
# on your site.
CACHE = dict()
CACHE_ENABLED = True
# Apart from generally having a time out for a cache entry, the following
# translates to the amount of time a user who has already been cached and is
# using a Subversion client will wait before its affected by permission
# changes applied on the application that pertain to it.
# On the other hand, increasing this timeout has a direct impact on performance
# and scalability because more users can be cached for longer.
# This is in seconds.
CACHE_TIMEOUT = 180 
# This setting has a significant impact on the performance of Subversion.
# The higher its value the more time and effort is saved fetching for permissions.  
# Beware, increasing this also increases (as a mulitple) the amount of memory
# Apache threads will be holding on to until items are taken out of the cache.
MAX_CACHE_ENTRIES = 100


ENTRY_NOT_FOUND = '!!!NOTFOUND!!!'

# Below is a tuple of known access types.
ACCESS_TYPES = (
    'commit', # This access type is for operations that will change the repository
    'view', # This access type is for operations that will view the repository
)

# Below is a tuple of known access type modifiers
#
# (Access type modifiers are used to make handle situations where just
#  checking access at a particular path is more complex.  A perfect
#  example of this is MOVE.  For a DELETE operation, the user needs to
#  have 'commit' access at the path being moved, and 'all' paths below
#  that path, in the event it's a directory of course.  Modifiers let
#  us ask two special questions about paths that may be a directory:
#    [1]: Does a user have the ability to commit/view at all paths below the path in question
#    [2]: Does a user have the ability to commit/view at any path below the path in question
#
# Access modifiers are usually handled by the _has_permission() function
# which means you won't find many places where we explicitly call
# _has_permission() with a modifier.  Places where it makes sense to
# explicitly use a modifier is when initially checking for global
# read and global write access for early return if possible.
ACCESS_TYPE_MODIFIERS = (
    'all', # This modifier means access at the specified path *and* everywhere below
    'any', # This modifier means access at the specified path *or* anywhere below
)

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
        return doAuthzhandler(req)
    except Exception, e:
        import traceback

        exception = sys.exc_info()
        traceLines = traceback.format_exception(exception[0], exception[1], exception[2])

        for line in traceLines:
            req.log_error(line)

        return apache.HTTP_INTERNAL_SERVER_ERROR

def authenhandler(req):
    """ This method gets called by mod_python to perform authentication """
    try:
        return doAuthenhandler(req)
    except Exception, e:
        import traceback

        exception = sys.exc_info()
        traceLines = traceback.format_exception(exception[0], exception[1], exception[2])

        for line in traceLines:
            req.log_error(line)

        return apache.HTTP_INTERNAL_SERVER_ERROR

def doAuthzhandler(req):
    """ This method queries against the sourceforge server to check if the username can access a repository path """
    # Prepare to handle the request
    _prepare(req)

    has_access = False
    repo_name = req.repo_name
    repo_path = req.repo_path

    # Only proceed if there was a repository name found
    if repo_name and repo_name != "":
        access_type = None # Reasonable default

        # Retrieve the required access type
        if not METHOD_2_ACCESS_TYPE.has_key(req.method):
            req.log_error('Unknown method: ' + req.method)
            return apache.HTTP_INTERNAL_SERVER_ERROR
        else:
            # There are a few scenarios that should be mentioned, since they are special:
            #     COPY: When a Subversion copy request comes in, the actual uri in the
            #           Subversion request is the copy source.  The copy source only needs
            #           to have recursive 'view' access.  The destination is in the
            #           'Destination' request header.  The destination needs to have
            #           recursive 'commit' access.  The destination is handled later.
            #
            #     MOVE: Move is just like copy, when it comes to source and destination,
            #           from a Subversion perspective.  From a TeamForge permissioning
            #           perspective, both the source and the destionation need
            #           recursive 'commit' access.
            if req.method == "COPY":
                access_type = 'view'
            else:
                access_type = METHOD_2_ACCESS_TYPE[req.method]

        # Check for global read and write permission either from cache or by
        # checking against PBPs.
        global_read_access = _get_from_cache(req, 'global_read_access')
        global_write_access = _get_from_cache(req, 'global_write_access')

        if global_read_access == ENTRY_NOT_FOUND:
            global_read_access = _has_permission(req, '/', 'view', access_modifier='all')
        if global_write_access == ENTRY_NOT_FOUND:
            global_write_access = _has_permission(req, '/', 'commit', access_modifier='all')

        # Cache the global read/write answers to avoid the need to reauthorize in subsequent requests
        _add_to_cache(req, 'global_read_access', global_read_access)
        _add_to_cache(req, 'global_write_access', global_write_access)

        if global_write_access:
            # The user has global write access.  No need to authorize.
            has_access = True

            if DEBUG:
                _debug(req, 'User %s has global commit on %s...' % (req.user, repo_name))
        elif global_read_access and access_type == 'view':
            # If the user has global read and this is a read request
            has_access = True

            if DEBUG:
                _debug(req, 'User %s has global read on %s...' % (req.user, repo_name))
        elif req.method == 'OPTIONS':
            # Always allow 'OPTIONS' requests
            has_access = True
        elif req.method == 'PROPPATCH' and req.uri.startswith("%s/%s/%s/%s" % (req.svn_root_uri, repo_name, SVN_SPECIAL_URI, "bln")):
            # We need to handle authorization of revision property changes specially
            repository = "%s/%s" % (req.svn_root_path, repo_name)
            revision = req.uri.split('/')[-1]

            try:
                revision = int(revision)
            except ValueError:
                req.log_error("%s is not a valid revision number" % revision)
                return apache.HTTP_INTERNAL_SERVER_ERROR

            if DEBUG:
                _debug(req, 'User %s is attempting to change a revision property for revision %s' % (req.user, revision))

            # For authorizing a revision property change, all we need to do is
            # get a list of changed paths for the revision, regardless of change,
            # and verify that the changing user has write permission for the path.
            svnlook = SubversionUtil.createSVNLook(repository, rev=revision)
            changes = SubversionUtil.generalizeChanges(svnlook.changed())

            # Authorize each path modified in the revision
            for change in changes:
                has_access = _has_permission(req, change[2], 'commit')

                # If there is an authorization failure, break since revision property changes require
                # 'commit' access to all changed paths for the revision.
                if not has_access:
                    break
        else:
            has_access = _has_permission(req, repo_path, access_type)

        # Check the destination path if the method is 'COPY' or 'MOVE' if
        # the previous check, which is the source, was permitted.
        if has_access and req.method in ["COPY", "MOVE"]:
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
                    _debug(req, '%s subrequest has matching source and destination (%s)' % (req.method, dest_uri))
                    
            # Decode the uri
            dest_uri = urllib.unquote(dest_uri)

            try:
                dest_repo_name, dest_repo_path = __dav_svn_parse_uri(dest_uri, req.svn_root_uri)

                req.repo_name = dest_repo_name
            except RuntimeError, re:
                req.log_error(re)

            if DEBUG:
                _debug(req, 'Destination repository name: %s' % repr(dest_repo_name))
                _debug(req, 'Destination repository path: %s' % repr(dest_repo_path))

            has_access = _has_permission(req, dest_repo_path, 'commit', access_modifier='all')

    if has_access:
        if DEBUG:
            _debug(req, 'Response 200')

        return apache.OK
    else:
        if DEBUG:
            _debug(req, 'Response 403')

        return apache.HTTP_FORBIDDEN

def doAuthenhandler(req):
    """ This method queries against the TeamForge server to check is the username/password is valid """
    is_valid_user = False

    # As documented in mod_python, before you can successfully call req.user you must call
    # req.get_basic_auth_pw().
    # http://modpython.org/live/current/doc-html/pyapi-mprequest-mem.html#l2h-124
    password = req.get_basic_auth_pw()
    username = req.user

    if DEBUG:
        _debug(req, 'Authenticating %s' % username)

    # Prepare to handle the request
    _prepare(req)

    # Get the authenticated users from cache
    authenticated_users = _get_from_cache(req, 'authenticated_users')

    # If there is no entry for 'authenticated_users', initialize to None.  (This can happen
    # when there is no cache in place for the given system:repo:user:conn.)
    if authenticated_users == ENTRY_NOT_FOUND:
        authenticated_users = None

    uname_pwd_hash = SourceForge.getSha1Hash('%s:%s' % (username, password))

    if authenticated_users is not None and uname_pwd_hash in authenticated_users:
        is_valid_user = True

    # If the user isn't in the authenticated users cache, attempt to authenticate by querying
    # the application server.
    if not is_valid_user:
        scm = SOAPpy.SOAPProxy(SourceForge.getSOAPServiceUrl("ScmListener"))

        try:
            key = SourceForge.createScmRequestKey()
            if int(SourceForge.getDefaultSoapVersion()) < 60:
                response = scm.isValidUser(key, username, password)
            else:
                response = scm.isValidUser(key, username, password, req.system_id, req.repo_name)

            if response == 0:
                is_valid_user = True
            elif response == 1:
                is_valid_user = False
            else:
                req.log_error("Unexpected response '%s'" % str(response))

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

            # Persist the authenticated users
            _add_to_cache(req, 'authenticated_users', authenticated_users)

        req.headers_out.add('CtfUserName',username)

        req.has_authenticated = True

        # Now authorize the user
        return authzhandler(req);
    else:
        return apache.HTTP_UNAUTHORIZED

def _update_environment(options):
    """ For the given mod_python notes, check for sourceforge.properties.path and if present
    create the SOURCEFORGE_PROPERTIES_PATH from its value. """
    if options.has_key('sourceforge.properties.path'):
        os.environ['SOURCEFORGE_PROPERTIES_PATH'] = options['sourceforge.properties.path']

# _update_environment()

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

# __dav_svn_parse_uri()

def _has_permission(req, repo_path, access_type, access_modifier=None):
    """ Returns whether or not the requested access is available for the given
    repository name and path. """

    if access_type not in ACCESS_TYPES:
        raise ValueError("'access_type' must be one of the following: %s" % ', '.join(ACCESS_TYPES))

    if access_modifier is not None and access_modifier not in ACCESS_TYPE_MODIFIERS:
        raise ValueError("'access_modifier' must be one of the following: %s" % ', '.join(ACCESS_TYPE_MODIFIERS))

    if req.repo_name is None:
        raise ValueError("'repo_name' cannot be None and must be a valid repository name")

    if repo_path is None:
        repo_path = '/'
        access_modifier = 'any'
    elif req.method in ('COPY', 'DELETE', 'MOVE'):
        access_modifier = 'all'

    if not repo_path.startswith('/'):
        repo_path = '/' + repo_path

    if DEBUG:
        access_modifier_str = ''

        if access_modifier is not None:
            if access_modifier == 'all':
                access_modifier_str = ' (all)'
            else:
                access_modifier_str = ' (any)'

        _debug(req, 'Checking %s%s permissions on %s%s for %s' % (access_type, access_modifier_str, req.repo_name, repo_path, req.user))

    if not hasattr(req, 'pbps'):
        pbps = _get_pbps(req)

        _add_to_cache(req, 'paths', pbps)

        req.pbps = pbps
    else:
        pbps = req.pbps

    if DEBUG:
        _debug(req, 'PBPS: %s' % repr(pbps))

    # If there are no PBPs, there is no way to authorize the user so return False
    if pbps is None or len(pbps.keys()) == 0:
        return False

    path_parts = repo_path.split('/')
    has_access = False # Reasonable default

    # To avoid the problem where '/' splits into two null strings, which results
    # in double checking '/', just remove the duplicate entry.  This will not
    # have any impact on the actual path being tested but will instead stop the
    # duplicate checking of '/'.
    if repo_path == '/':
        path_parts = ['']

    # Get permission at path by checking explicit permisions at the requested path
    # and walking up the path tree one path at a time until a PBP is found.
    for x in range(len(path_parts)):
        path = '/' + '/'.join(path_parts[1:len(path_parts) - x])
        pbp_access = 'none' # Reasonable default

        if not pbps.has_key(path):
            if DEBUG:
                _debug(req, '  [path] %s (No PBP)' % path)

            # If there is no PBP at the path, continue
            continue
        else:
            pbp_access = pbps[path]

            if DEBUG:
                _debug(req, '  [path] %s->%s' % (path, pbp_access))

        if access_type == 'commit' and pbp_access in ('commit'):
            has_access = True
        elif access_type == 'view' and pbp_access in ('commit', 'view'):
            has_access = True

        break

    if access_modifier is not None:
        if access_modifier == 'any' and not has_access:
            # The only reason to check the 'any' modifier is if there was not previously
            # access given.
            for path in pbps.keys():
                pbp_access = pbps[path]

                if DEBUG:
                    _debug(req, '  [any] %s->%s' % (path, pbp_access))

                if path.startswith(repo_path):
                    if access_type == 'commit' and pbp_access == 'commit':
                        has_access = True

                        break
                    elif access_type == 'view' and pbp_access in ('commit', 'view'):
                        has_access = True

                        break
        elif access_modifier == 'all' and has_access:
            # We always have to check the 'all' modifier unless there was not previously
            # access given.
            for path in pbps.keys():
                pbp_access = pbps[path]

                if DEBUG:
                    _debug(req, '  [all] %s->%s' % (path, pbp_access))

                if path.startswith(repo_path):
                    if access_type == 'commit' and pbp_access != 'commit':
                        has_access = False

                        break
                    elif access_type == 'view' and pbp_access not in ('commit', 'view'):
                        has_access = False

                        break

    if DEBUG:
        _debug(req, 'Has permission: %s' % repr(has_access))

    return has_access

# _has_permission()

def _add_to_cache(req, key, value):
    """ Adds an object to cache. """
    if CACHE_ENABLED:
        global CACHE

        if not CACHE.has_key(req.cache_key):
            CACHE[req.cache_key] = dict()
            CACHE[req.cache_key]['timestamp'] = datetime.datetime.now()

        if DEBUG:
            _debug(req, '(add) Initializing cache entry for %s' % req.cache_key)

        CACHE[req.cache_key][key] = value

    if DEBUG:
        _debug(req, '(add) Cache entries: %d' % len(CACHE.keys()))

# _add_to_cache()

def _get_from_cache(req, key):
    """ Retrieves an object from cache. """
    if DEBUG:
        _debug(req, '(get - %s) Cache entries: %d' % (key, len(CACHE.keys())))

    if CACHE.has_key(req.cache_key) and CACHE[req.cache_key].has_key(key):
        return CACHE[req.cache_key][key]
    else:
        return ENTRY_NOT_FOUND

# _get_from_cache()

def _remove_from_cache(req):
    """ Removes an entry from cache. """
    if DEBUG:
        _debug(req, '(remove-pre) Cache entries: %d' % len(CACHE.keys()))

    if CACHE.has_key(req.cache_key):
        if DEBUG:
            _debug(req, 'Removing cache entry for %s' % repr(req.cache_key))

        del CACHE[req.cache_key]
    else:
        if DEBUG:
            _debug(req, 'Unable to find a cache entry for %s' % repr(req.cache_key))

    if DEBUG:
        _debug(req, '(remove-post) Cache entries: %d' % len(CACHE.keys()))

# _remove_from_cache()

def _remove_oldest_cache_entry(req):
    """ Removes oldest entry in the cache to make way for a new entry. """
    global CACHE

    # The cache entries are in the form: 
    #   {'exsy1017:foobar:user1': {'timestamp': datetime.datetime(2010, 7, 29, 11, 27, 43, 631206), ...
    # The following will generate a list with deail about the oldest cache entry, in the form:
    #   [datetime.datetime(2010, 7, 29, 11, 27, 41, 283748), 'exsy1017:foobar:user1']
    oldest = min([[CACHE[v]['timestamp'], v] for v in CACHE.keys()])
    # Take out the oldest entry in the cache
    del CACHE[oldest[1]]

    if DEBUG:
        _debug(req, 'Removed least used cache entry: [%s, %s]' % (oldest[1],oldest[0]))

# _remove_oldest_cache_entry()

def _prepare(req):
    """ This function looks at the request and does some processing to
    prepare the request object for being used by other functions.  When
    this function terminates, the following should be set:

      req.cache_key: Contains the cache key for the system:repo:user:conn
      req.system_id: Contains the system id for this integration server
      req.repo_name: Contains the repository name
      req.repo_path: Contains the repository path
      req.pbps: Contains the path based permissions (This is only set if
                there are pbps in the cache and the user is authenticated.) """
    # If there is no repo_name or repo_path attributes for the request object,
    # that means we need to prepare the request to be handled.
    if not hasattr(req, 'repo_name') or not hasattr(req, 'repo_path'):
        options = req.get_options()

        _update_environment(options)

        # Disable the cache for test mode
        if options.has_key('svn.disable.cache') and options['svn.disable.cache'].lower() in ('on', 'true'):
            global CACHE_ENABLED

            CACHE_ENABLED = False

        try:
            req.svn_root_path = options['svn.root.path']
            req.svn_root_uri = options['svn.root.uri']
            repo_name, repo_path = __dav_svn_parse_uri(req.uri, req.svn_root_uri)

            # Ignore the uri for MERGE requests, with both mod_dav_svn and mod_authz_svn do.
            if req.method == 'MERGE':
                repo_path = None

            req.repo_name = repo_name
            req.repo_path = repo_path
        except RuntimeError, re:
            req.log_error(re)

        if DEBUG:
            _debug(req, 'Request: (%s)%s->%s' % (req.user, req.method, req.uri))
            _debug(req, 'Repository name: %s' % repr(repo_name))
            _debug(req, 'Repository path: %s' % repr(repo_path))

    if not hasattr(req, 'system_id'):
        req.system_id = _get_system_id(req)

    req.cache_key = '%s:%s:%s' % (req.system_id, req.repo_name, req.user)

    # Clear the cache if it has reached the maximum number of entries
    if len(CACHE.keys()) >= MAX_CACHE_ENTRIES:
        if DEBUG:
            _debug(req, 'Clearing cache as the cache has reached the maximum number of entries (%d)' % MAX_CACHE_ENTRIES)

        # The cache is full, but we only need to make room if this is something not already cached.
        if not CACHE.has_key(req.cache_key): 
            _remove_oldest_cache_entry(req)

    # If there is already a cache entry for this system:repo:user:conn, remove it if it's stale.
    cache_entry_timestamp = _get_from_cache(req, 'timestamp')

    if cache_entry_timestamp != ENTRY_NOT_FOUND:
        # Check to see if the cache's timestamp is stale
        if (datetime.datetime.now() - cache_entry_timestamp).seconds > CACHE_TIMEOUT:
            # Remove the cache entry as it is stale
            _remove_from_cache(req)

    # If there is still a cache entry for the cache key, put the pbps into the request object.
    if hasattr(req, 'has_authenticated') and req.has_authenticated and CACHE.has_key(req.cache_key):
        pbps = _get_from_cache(req, 'paths')

        if pbps != ENTRY_NOT_FOUND:
            req.pbps = _get_from_cache(req, 'paths')

# _prepare()

def _get_pbps(req):
    """ Retrieves the path-based permissions for the user:repo by calling the
    ScmListener.getRolePaths() method on the application server. """
    scm = SOAPpy.SOAPProxy(SourceForge.getSOAPServiceUrl("ScmListener"))
    key = SourceForge.createScmRequestKey()
    raw_pbps = scm.getRolePaths(key, req.user, req.system_id, req.repo_name)
    pbps = {}

    if DEBUG:
        _debug(req, 'ScmListener.getRolePaths(%s, %s, %s, %s) -> %s' % (key, req.user, req.system_id, req.repo_name, repr(raw_pbps)))

    # Take the PBPs and turn them into a dictionary where the path is the key
    for pbp in raw_pbps:
        pbp_parts = pbp.split(':')

        pbps[pbp_parts[1]] = pbp_parts[0]

    return pbps

# _get_pbps()

def _get_system_id(req):
    """ Retrieves the system id either from cache, from properties file
    or from the application server if the request is for a branding repository. """
    system_id = None

    # The only caching we do of the system id is at the request scope since there is
    # no way to tell if a cached system id is valid without retrieving the system id.
    # that being said, if there is no system_id attribute for the request object, we
    # will always read it from the <repo_root>/.scm.properties file or call back to
    # the application server if the repositoy is a branding repo
    try:
        branding_uri = SourceForge.get('subversion_branding.repository_uri')
        if branding_uri and req.uri.startswith(branding_uri):
            # This is a branding repo, get the external system id from ScmListener
            scm = SOAPpy.SOAPProxy(SourceForge.getSOAPServiceUrl("ScmListener"))
            key = SourceForge.createScmRequestKey()
            system_id = scm.getBrandingExternalSystemId(key)
        else:
            # This is a regular Subversion repository, load it's .scm.properties file
            repoBase = SourceForge.get('subversion.repository_base')
            SourceForge.load(repoBase + "/.scm.properties")
            system_id = SourceForge.get('external_system_id')
    except Exception, inst:
        raise LookupError('Failed to get external system id: ' + inst.__str__())

    return system_id

# _get_system_id()

def _debug(req, msg):
   """ Write an entry to the Apache error log. """
   req.log_error('[scm debug] %s' % msg)

# _debug()
