/**
  Contains all of the exceptions Jervis uses to provide user friendly failures.
  Generally, all exceptions are one of two types: an <tt>exception group</tt> or an <tt>issue exception</tt>.
  The following bullet tree generally describes the intended exception hierarchy.
  <ul>
      <li>
          <tt>JervisException</tt> - The base exception class for Jervis from which all other exceptions derive.
          <ul>
              <li>
              An <tt>exception group</tt> which groups related issue exceptions.
              <ul>
                  <li>
                      An <tt>issue exception</tt> calling out an issue in a user friendly way.
                  </li>
              </ul>
              </li>
          </ul>
      </li>
  </ul>
  <p>
  Whenever working in Jervis the generic <tt>Exception</tt> should never be thrown.
  Instead, the <tt>JervisException</tt> should be thrown for the more generic exceptions.
  This way Jervis exceptions can be distinguished from other types of exceptions which might get thrown during runtime.
  </p>
 */
package jervis.exceptions
