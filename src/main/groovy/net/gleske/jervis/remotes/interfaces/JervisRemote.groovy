/*
   Copyright 2014-2023 Sam Gleske - https://github.com/samrocketman/jervis

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
   */
package net.gleske.jervis.remotes.interfaces

/**
  Provides a minimum standard interface a Jervis remote requires.  Throughout
  Jervis these functions will be called on the remote.  It is recommended that
  only the functions in this interface should be used by Job DSL plugin scripts
  when accessing a remote.  Failure to do so risks the flexibility and
  portability of switching remote sources.
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
      Get the contents of a file in a remote git project for a given reference.
      Uses the default branch.  Most commonly the file requested will be the
      Jervis YAML file.

      @return A <tt>String</tt> which contains the contents of the file requested.
     */
    String getFile(String project, String file_path);

    /**
      Get the contents of a file in a remote git project for a given reference.  Most
      commonly the file requested will be the Jervis YAML file.

      @return A <tt>String</tt> which contains the contents of the file requested.
     */
    String getFile(String project, String file_path, String ref);

    /**
      List the contents of the root directory of the default branch.  This will
      typically be used to list the contents of the root directory for the
      reference so that build scripts can be generated from the list.

      @return A list of files in the requested file path.
     */
    ArrayList getFolderListing(String project);

    /**
      List a the contents of a folder path in a project of the default branch.
      This will typically be used to list the contents of the root directory
      for the reference so that build scripts can be generated from the list.

      @return A list of files in the requested file path.
     */
    ArrayList getFolderListing(String project, String dir_path);

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
