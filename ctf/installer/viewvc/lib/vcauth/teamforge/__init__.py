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
import ctfauthorizer

from suds.client import Client

class ViewVCAuthorizer(vcauth.GenericViewVCAuthorizer):
  """Custom ViewVC authorization module for TeamForge."""
  
  def __init__(self, username=None, params={}):
    """Create a GenericViewVCAuthorizer object which will be used to
    validate that USERNAME has the permissions needed to view version
    control repositories (in whole or in part).  PARAMS is a
    dictionary of custom parameters for the authorizer."""

    self.username = username # The username being authorized
    self.perm_cache = { } # A cache of the previously authorized paths
    self.roottype = params['roottype'] # The type of repository being browsed ('cvs' or 'svn')
    self.query_dict = params['query_dict'] # The query parameters

    # A single-item array, where the item is a universal access
    # determination (1=yes, 0=no, None=dunno); or None if we haven't
    # queried for universal access yet.
    self.global_access = None

    system_id = self.query_dict['system']

    if self.roottype == "svn":
        SOAPServiceUrl = SourceForge.getSOAPServiceUrl("ScmListener")

        # add proxy settings if they exist
        httpProxy = SourceForge.getProxyUrl()
        proxyDict = {}
        if httpProxy and len(httpProxy) > 0:
            proxyDict[SourceForge.getProxyProtocol(SOAPServiceUrl)] = httpProxy

        # Create the client with proxy and location options    
        # We need to override the location from the WSDL which reports localhost

        scm = Client(SOAPServiceUrl + '?wsdl', location=SOAPServiceUrl, proxy=proxyDict)

        key = SourceForge.createScmRequestKey()
        # if usename is empty then set as 'nobody' user
        if username is None or username == '':
            username = 'nobody'
        raw_pbps = scm.service.getRolePaths(key, username, system_id, self.query_dict['root'])
        raw_pbps = map(str, raw_pbps)
        ctfauthorizer.set_pbps(raw_pbps)

  def check_root_access(self, rootname):
    """Return 1 if the associated username is permitted to read ROOTNAME."""
    return 1

  def check_universal_access(self, rootname):
    """Return 1 if the associated username is permitted to read ALL
    paths in ROOTNAME, 0 if username is permitted to read NO paths in
    ROOTNAME, or None if no such universal access determination can be
    made."""

    if self.global_access is not None:
      return self.global_access[0]
    
    if self.roottype == "cvs":
      self.global_access = [1]
    else:
      # Check for global view access
      pbp_access = ctfauthorizer.determine_global_access()
      if pbp_access:
        self.global_access = [1]
      else:
        self.global_access = [None]
    return self.global_access[0]

  def check_path_access(self, rootname, path_parts, pathtype, rev=None):
    """Return 1 if the associated username is permitted to read
    revision REV of the path PATH_PARTS (of type PATHTYPE) in
    repository ROOTNAME."""

    has_access = 0
    repo_path = '%s/%s' % (rootname, string.join(path_parts, '/'))
    path_inside_repo = '/%s' % (string.join(path_parts, '/'))

    # Short circuit the authorization check when the user is known to have
    # a universal access determination (either granting or denying access).
    univ_access = self.check_universal_access(rootname)
    if univ_access is not None:
      return univ_access

    # If we have a cached permission for this path, use it
    if self.perm_cache.has_key(repo_path):
      return self.perm_cache[repo_path]
    
    # The root of the repository should always be readable.
    # For example, 
    #   PBP: 
    #    /: None
    #    /trunk: view
    #    /branches: view
    # Even though the user doesn't have access to the root of the repository,
    # all other paths which he/she has access should be viewable.      
    if path_inside_repo == "/":
      has_access = 1
    else:
      # No cache, lets get the permission using our authorizer.
      has_access = ctfauthorizer.has_non_recursive_access(path_inside_repo, 1)

    self.perm_cache[repo_path] = has_access

    return has_access
