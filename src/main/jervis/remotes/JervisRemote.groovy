package jervis.remotes

/**
  Provides a minimum standard interface a Jervis remote requires.  Throughout
  Jervis these functions will be called on the remote.
 */
interface JervisRemote {

    /**
      A web interface to browse repository code.

      @return A base web URL in which to browse code.
     */
    String getWebUrl();

    /**
      Repositories will be cloned from this base Git URI.

      @return A base git URI in which a repository can be cloned.
     */
    String getCloneUrl();

    /**
      Query a remote project for a list of branches.

      @return A <tt>List</tt> containing all of the branches listed on the remote project.
     */
    List branches(String project);

    /**
      Get the contents of a file in a remote git project for a given reference.  Most
      commonly the file requested will be the Jervis YAML file.

      @return A <tt>String</tt> which contains the contents of the file requested.
     */
    String getFile(String project, String file_path, String ref);

    /**
      Get list a file path in a project for the given reference.  This will typically
      be used to list the contents of the root directory for the reference so that
      build scripts can be generated from the list.

      @return A list of files in the requested file path.
     */
    ArrayList getFolderListing(String project, String dir_path, String ref);

    /**
      This method is used by Jervis to output friendly messages including the remote.

      @return A human readable <tt>String</tt> for this type of remote.
     */
    String toString();
}
