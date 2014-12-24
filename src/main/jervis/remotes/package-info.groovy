/**
  Contains <tt>remotes</tt> for Jervis to communicate to different sources hosting Git repositories.
  If writing a remote so Jervis can communicate to remotes other than <tt>GitHub</tt> then it is required to
  have at a minimum the following functions.

  <ul>
      <li>
          <tt>String getWebUrl()</tt> returns a base web URL in which to browse code.
      </li>
      <li>
          <tt>String getCloneUrl()</tt> returns a base git URI in which a repository can be cloned.
      </li>
      <li>
          <tt>List branches(String project)</tt> returns a <tt>List</tt> where each element is a branch in the project.
      </li>
      <li>
          <tt>String getFile(String project, String file_path, String ref)</tt> returns a <tt>String</tt> which contains the contents of the file requested.
      </li>
      <li>
          <tt>String toString()</tt> returns a human readable <tt>String</tt> for this type of remote.
      </li>
  </ul>
  <p>
  This allows remotes to be interchangeable.  Jervis will call only those functions to interact with a remote.
  </p>
 */
package jervis.remotes
