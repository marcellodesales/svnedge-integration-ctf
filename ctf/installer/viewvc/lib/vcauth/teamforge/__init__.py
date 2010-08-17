# -*-python-*-
#
# This file implements a custom ViewVC authorization module which
# authenticates user access based on TeamForge path-based
# permissions.

__copyright__ = "Copyright (c) 1999-2008 CollabNet, Inc. All rights reserved."

import os
import string
import SourceForge
import vcauth
import vclib

class ViewVCAuthorizer(vcauth.GenericViewVCAuthorizer):
  """Custom ViewVC authorization module for TeamForge."""
  
  def __init__(self, username=None, params={}):
    """Create a GenericViewVCAuthorizer object which will be used to
    validate that USERNAME has the permissions needed to view version
    control repositories (in whole or in part).  PARAMS is a
    dictionary of custom parameters for the authorizer."""
    
    self.username = username # The username being authorized
    self.perm_cache = { } # A cache of the previously authorized paths
    self.global_access = None # Simple boolean to short-circuit authorizations
                              # (TODO: Port Apache performance tweaks here.)
    self.roottype = params['roottype'] # The type of repository being browsed ('cvs' or 'svn')
    self.query_dict = params['query_dict'] # The query parameters

  def check_root_access(self, rootname):
    """Return 1 if the associated username is permitted to read ROOTNAME."""
    return 1
  
  def check_path_access(self, rootname, path_parts, pathtype, rev=None):
    """Return 1 if the associated username is permitted to read
    revision REV of the path PATH_PARTS (of type PATHTYPE) in
    repository ROOTNAME."""
    has_access = 0

    # Quick return for CVS repositories
    if self.roottype == "cvs":
      return 1

    repo_path = "%s/%s" % (rootname, string.join(path_parts, '/'))
    access_type = "view"

    # Call SourceForge.load() manually if a custom sourceforge.properties path is used
    if os.environ.has_key('SOURCEFORGE_PROPERTIES_PATH'):
      SourceForge.load(filename=os.environ['SOURCEFORGE_PROPERTIES_PATH'])

    if self.global_access is None:
      path, all, any = SourceForge.getScmPermissionForPath(self.username,
                                                           self.query_dict['system'],
                                                           rootname + "/",
                                                           access_type)

      if all == 0:
        self.global_access = True
      else:
        self.global_access = False

    # Do not use the servlet authorizer any further, global access granted
    if self.global_access:
      return 1

    # If we have a cached permission for this path, use it
    if self.perm_cache.has_key(repo_path):
      return self.perm_cache[repo_path]

    # No cache, lets get the permission by querying TeamForge
    path, all, any = SourceForge.getScmPermissionForPath(self.username,
                                                         self.query_dict['system'],
                                                         repo_path,
                                                         access_type)

    # Take the servlet response and convert to a ViewVC response
    if any != 0:
      has_access = 0
    else:
      has_access = 1

    self.perm_cache[repo_path] = has_access

    return has_access
