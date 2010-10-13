import difflib, os, itertools, StringIO, SourceForge, sys

from svn import core, fs, delta, repos

# Provides access to a subset of the data available from Subversion's
# svnlook executable.
#
# Available functions:
#   author:     Get the author.
#   changed:    Get the full change summary: all dirs & files changed.
#   changelist: Get the changelist
#   diff:       Get the GNU-style diffs of changed files and properties.
#   log:        Get the log message.
class SVNLook:
  """A class that wrappers Subversion's svnlook command line executable.  This
     class provides only a subset of what svnlook can do."""
  def __init__(self, path, rev, txn):
    self.repo_root_path = core.svn_path_canonicalize(path)
    
    root_ptr = repos.open(self.repo_root_path)
    self.fs_ptr = repos.fs(root_ptr)

    # Set the revision/transaction
    if txn:
      self.txn_ptr = fs.open_txn(self.fs_ptr, txn)
    else:
      self.txn_ptr = None
      if rev is None:
          rev = fs.youngest_rev(self.fs_ptr)
    self.rev = rev

    # Set the root
    if self.txn_ptr:
      self.root = fs.txn_root(self.txn_ptr)
    else:
      self.root = fs.revision_root(self.fs_ptr, self.rev)

    # Set the base revision/transaction
    if self.txn_ptr:
      self.base_rev = fs.txn_base_revision(self.txn_ptr)
    else:
      self.base_rev = self.rev - 1

    # Set the base root of the comparison
    self.base_root = fs.revision_root(self.fs_ptr, self.base_rev)

    # Get all the changes and sort by path
    editor = repos.ChangeCollector(self.fs_ptr, self.root)
    e_ptr, e_baton = delta.make_editor(editor)
    repos.replay(self.root, e_ptr, e_baton)

    self.changelist = editor.get_changes().items()

    self.changelist.sort()

  # __init__()

  def author(self):
    """Get the author property, or empty string if the property is not
       present."""
    return self._get_property(core.SVN_PROP_REVISION_AUTHOR) or ''

  # author()

  def changed(self):
    """Get the summary of changes.

       self.changelist is a list of svn.repos.ChangedPath objects.  Each
       ChangedPath object has the following attributes:
          action:       Integer corresponding with action performed on the path
          added:        Whether the path was added
          base_path:    Path the path was copied/moved/replaced from or None if
                          if there is no base path
          base_rev:     The revision the path was copied/moved/replaced from
                          or -1 if there was no base path
          item_kind:    Integer corresponds with the type of item it is
          path:         The actual path
          prop_changes: Whether there was a property change
          text_changed: Whether there was a text change
"""
    change_map = ('M', 'A', 'D', 'R')
    changed_summary = StringIO.StringIO()

    for path, change in self.changelist:
      if change_map[change.action] == 'M':
        # Write text changes
        if change.text_changed:
          changed_summary.write('U')
        else:
          changed_summary.write('_')

        # Write property changes
        if change.prop_changes:
          changed_summary.write('U')
        else:
          changed_summary.write(' ')
      elif change_map[change.action] == 'A':
        changed_summary.write('A')
      elif change_map[change.action] == 'D':
        changed_summary.write('D')
      elif change_map[change.action] == 'R':
        changed_summary.write('R')

      if change_map[change.action] == 'M':
        changed_summary.write('  ')
      else:
        changed_summary.write('   ')

      # Add trailing slash for directories
      if change.item_kind == core.svn_node_dir:
        if path:
          path = path + '/'

        if change.path:
          change.path = change.path + '/'

        if change.base_path:
          change.base_path = change.base_path + '/'

      # Write path
      changed_summary.write(path.strip())

      if (change_map[change.action] in ['A', 'R']) and change.base_path:
        changed_summary.write(' (from %s:%s)' % (change.base_path, change.base_rev))

      changed_summary.write('\n')

    changed_content = changed_summary.getvalue()

    changed_summary.close()

    return changed_content

  # changed()

  def diff(self, max_length=0):
    """Get the GNU-style diff of all changed files and properties.  Pass a
       max_length greater than 0 to get limit the diff to that many
       characters."""
    diff = StringIO.StringIO()

    for path, change in self.changelist:
      if change.text_changed:
        diff.write(self._get_text_diff(change.base_path, change.path))

      if change.prop_changes:
        diff.write(self._get_prop_diff(change.path))

      if max_length > 0 and len(diff.getvalue()) >= max_length:
        break

    diff_content = diff.getvalue()

    diff.close()

    return diff_content[0:max_length]

  def log(self, print_size=0):
    """Get the log property, or empty string if the property is not present."""
    return self._get_property(core.SVN_PROP_REVISION_LOG) or ''

  # log()

  def changelist(self):
    """Get the changelist, which is a list of tuples.  Each entry has the path
       that was changed and the corresponding svn.repos.ChangedPath object."""
    return self.changelist

  def _get_text_diff(self, base_path, path):
    """Get file changes as a GNU-style diff."""
    diff = StringIO.StringIO()

    if base_path is None:
      diff.write("Added: %s\n" % path)
      label = path
    elif path is None:
      diff.write("Removed: %s\n" % base_path)
      label = base_path
    else:
      diff.write("Modified: %s\n" % path)
      label = path

    diff.write("===========================================================" + \
               "===================\n")

    args = []
    args.append("-L")
    args.append(label + "\t(original)")
    args.append("-L")
    args.append(label + "\t(new)")
    args.append("-u")
    differ = fs.FileDiff(self.base_root, base_path, self.root,
                         path, diffoptions=args)

    if differ.either_binary():
        diff.write("(Binary file differs)\n")
    else:
        # not using differ.get_pipe() as that delegates to the native diff
        # binary.  Solaris diff does not support labels.
        file1_path, file2_path = differ.get_files()
        file1 = open(file1_path, "r")
        file2 = open(file2_path, "r")
        diff_lines = difflib.unified_diff(file1.readlines(), file2.readlines(),
             label, label, "\t(original)", "\t(new)")

        file1.close()
        file2.close()
        # File removal is handled by repos.FileDiff

        for line in diff_lines:
            diff.write(line)

    diff.write("\n")

    diff_content = diff.getvalue()

    diff.close()

    return diff_content

  # _get_text_diff()

  def _get_prop_diff(self, path):
    """Get property changes as a GNU-style diff."""
    try:
      root_props = fs.node_proplist(self.root, path)
    except:
      root_props = []

    try:
      base_props = fs.node_proplist(self.base_root, path)
    except:
      base_props = []

    file_props = list(itertools.chain(base_props, root_props))
    diff_content = ''

    file_props.sort()

    for prop_name in file_props:
      try:
        old_prop_val = fs.node_prop(self.base_root, path, prop_name)
      except core.SubversionException:
        old_prop_val = None
        pass # Must be a new path

      try:
        new_prop_val = fs.node_prop(self.root, path, prop_name)
      except core.SubversionException:
        new_prop_val = None
        pass # Must be a deleted path

      if not old_prop_val == new_prop_val:
        diff = StringIO.StringIO()

        diff.write("Property changes on: %s\n" % path)
        diff.write("_______________________________________________________" + \
                   "_______________________\n")

        if old_prop_val:
          if new_prop_val:
            diff.write("Modified: %s\n" % prop_name)
            diff.write("   - %s\n" % str(old_prop_val))
            diff.write("   + %s\n" % str(new_prop_val))
          else:
            diff.write("Deleted: %s\n" % prop_name)
            diff.write("   - %s\n" % str(old_prop_val))
        else:
          diff.write("Added: %s\n" % prop_name)
          diff.write("   + %s\n" % str(new_prop_val))

        diff.write("\n")

        diff_content = diff_content + diff.getvalue()

        diff.close()

    return diff_content

  # _get_prop_diff()

  def _get_property(self, name):
    """Returns the revision/transaction property for the given name."""
    if self.txn_ptr:
      return fs.txn_prop(self.txn_ptr, name)
    return fs.revision_prop(self.fs_ptr, self.rev, name)

  # _get_property()

def createSVNLook(path, rev = None, txn = None):
  "SVNLook object factory"
  return SVNLook(path, rev, txn)

def isSVNRepository(repository):
  return os.path.isdir(repository) and \
         os.path.isdir(os.path.join(repository, "db")) and \
         os.path.isdir(os.path.join(repository, "hooks")) and \
         os.path.isfile(os.path.join(repository, "format"))

def generalizeChanges(changes):
    """Process low-level changes and infer higher-level operations like 
    copies/moves from them.  Returns a list of tuples identifying the
    changes:
      [0]: If the content of the file changed, return a single character description
           describing the change.  (A = added, C = copied, D = deleted, M = modified and U = updated)
      [1]: If there was a property change, return a single character description
           describing the change.  (A = added, D = deleted and U = updated)
      [2]: The path that was changed
      [3]: Optional path describing the source of a copy or move.  (Only used when [0] is C or M.)
      [4]: Optional revision describing the source revision of the copy or move. (Only used when [0] is C or M.)
    """
    
    # Convert changes string to list format used by this method
    change_entries = []
    for change in changes.splitlines():
        change = change.rstrip()

        if not change: continue

        changedata, changeprop, path = None, None, None

        if change[0] != "_":
            # Replace isn't supported right now but it is a copy so treat it as such
            if change[0] == 'R':
                changedata = 'A'
            else:
                changedata = change[0]

        if change[1] != " ":
            changeprop = change[1]
        
        path = change[4:]

        if path.find("(from ") > -1:
            parts = path.split(" (from ")
            path = parts[0]
            ref_parts = parts[1][:-1].split(":")
            ref_path = ref_parts[0]
            ref_rev = ref_parts[1]

            change_entries.append((changedata, changeprop, path, ref_path, ref_rev))
        else:
            change_entries.append((changedata, changeprop, path))
    
    result = []
    previousChange = [None, None]
    previousChangeIdx = -1

    for i in change_entries:
        if i[0] == 'A' and previousChange[0] == 'D' and i[2] == previousChange[1]:
            # This scenario represents a Subversion move.  Instead of having a
            # record of an add of the new path and a delete of the old path,
            # display as a move.
            result.pop()
            result.append(('M',) + i[1:])
        elif i[0] == 'A' and len(i) > 3:
            # This scenario represents a Subversion copy.  Instead of showing an
            # add, display as a copy.
            result.append(('C',) + i[1:])
        else:
            result.append(i)

        previousChange = i
        previousChangeIdx += 1
    
    return result

# This script can be ran as a script for testing reasons
# arg 1: Full path to a local Subversion repository root
# arg 2: The revision number you're interested in investigating
if __name__ == "__main__":
  if len(sys.argv) != 3:
    print("""Not enough information to run.
usage: python SubversionUtil.py REPO_PATH REV
""")

    sys.exit(0)

  look = SVNLook(sys.argv[1], int(sys.argv[2]), None)

  print("Author: %s" % look.author())
  print("Log:    %s" % look.log())
  print("Changed:\n%s" % look.changed())
  print(look.diff())
