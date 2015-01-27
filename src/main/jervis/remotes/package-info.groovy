/**
  Contains <tt>remotes</tt> for Jervis to communicate to different sources hosting
  Git repositories.  If writing a remote, be sure to follow the implementation of
  the <tt>JervisRemote</tt> interface.

  <p>
  This allows remotes to be interchangeable.  Jervis will call only those functions to interact with a remote.
  See the <tt>GitHub</tt> class as an example for the implementation of a remote.
  </p>
 */
package jervis.remotes
