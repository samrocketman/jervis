/*
   Copyright 2014-2015 Sam Gleske - https://github.com/samrocketman/jervis

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
package jervis.remotes

import groovy.json.JsonSlurper
import jervis.tools.securityIO

/**
   A simple class to interact with the GitHub API for only the parts I need.

   <h2>Sample usage</h2>

<pre><tt>import jervis.remotes.GitHub
def x = new GitHub()
println 'Print each branch.'
x.branches('samrocketman/jervis').each{ println it }
println 'Print the contents of .travis.yml from the master branch.'
println x.getFile('samrocketman/jervis','.travis.yml','master')</tt></pre><br>
 */
class GitHub implements JervisRemote {

    /**
      URL to the GitHub web interface. Default: <tt>https://github.com/</tt>
     */
    String gh_web = 'https://github.com/'

    /**
      URL to the <a href="https://developer.github.com/v3/" target="_blank">GitHub API</a>. For GitHub Enterprise it should be <tt>{@link #gh_web} + 'api/v3/'</tt>.  Default: <tt>https://api.github.com/</tt>
     */
    String gh_api = 'https://api.github.com/'

    /**
      The base clone URI in which repositories will be cloned.  Default: <tt>git://github.com/</tt>
     */
    String gh_clone = 'git://github.com/'

    /**
      The <a href="https://github.com/blog/1509-personal-api-tokens" target="_blank">API token</a>, which can be used to communicate with GitHub using authentication.  Default: <tt>null</tt>

     */
    String gh_token

    /*
     * Setters for internal variables
     */

    /**
      Sets the <tt>{@link #gh_web}</tt> and <tt>{@link #gh_api}</tt> properties.  This automatically sets <tt>gh_api</tt> based on <tt>gh_web</tt>.
     */
    //gh_web will always end with a trailing slash
    void setGh_web(String gh_web) {
        this.gh_web = (gh_web[-1] == '/')? gh_web : gh_web + '/'
        this.setGh_api(this.gh_web + 'api/v3/')
    }

    /**
      Sets the <tt>{@link #gh_api}</tt> property.
     */
    //gh_api will always end with a trailing slash
    void setGh_api(String gh_api) {
        this.gh_api = (gh_api[-1] == '/')? gh_api : gh_api + '/'
    }

    /**
      Sets the <tt>{@link #gh_clone}</tt> property.
     */
    //gh_clone will always end with a trailing slash
    void setGh_clone(String gh_clone) {
        this.gh_clone = (gh_clone[-1] == '/')? gh_clone : gh_clone + '/'
    }

    /**
      Sets the <tt>{@link #gh_token}</tt> property.
     */
    //gh_token should be null if it is a zero length string.
    void setGh_token(String gh_token) {
        this.gh_token = (gh_token.length() > 0)? gh_token : null
    }

    /*
     * private functions
     */

    /*
       HashMap fetch(String addr) - fetches a URL.
       Args:
           addr - a web address to fetch.  The URL must return json content.
       returns a HashMap
    */
    private fetch(String addr) {
        def json = new JsonSlurper()
        if(this.gh_token) {
            return json.parse(new URL(addr).newReader(requestProperties: ['Authorization': "token ${this.gh_token}".toString(), 'Accept': 'application/vnd.github.v3+json']))
        }
        else {
            return json.parse(new URL(addr).newReader(requestProperties: ['Accept': 'application/vnd.github.v3+json']))
        }
    }

    /*
     * public functions
     */

    /**
      Get the contents of <tt>{@link #gh_web}</tt>.  This is meant to be a standard function for Jervis to interact with remotes.  All remotes are required to have this function.

      @return The contents of <tt>gh_web</tt>.
     */
    public String getWebUrl() {
        gh_web
    }

    /**
      Get the contents of <tt>{@link #gh_clone}</tt>.  This is meant to be a standard function for Jervis to interact with remotes.  All remotes are required to have this function.

      @return The contents of <tt>gh_clone</tt>.
     */
    public String getCloneUrl() {
        gh_clone
    }

    /**
      Get a list of branches for a project.  This is meant to be a standard function for Jervis to interact with remotes.  All remotes are required to have this function.

      @param  project  A GitHub project including the org.  e.g. <tt>"samrocketman/jervis"</tt>
      @return          A <tt>List</tt> where each element is a branch in the project.
     */
    public List branches(String project) {
        List list = []
        this.fetch(this.gh_api + "repos/${project}/branches").each { list << it.name }
        return list
    }

    /**
      Get the contents of a file from a project.  This is meant to be a standard function for Jervis to interact with remotes.  All remotes are required to have this function.

      @param   project    A GitHub project including the org.  e.g. <tt>"samrocketman/jervis"</tt>
      @param   file_path  A path to a file relative to the root of the Git repository.  e.g. <tt>".travis.yml"</tt>
      @param   ref        A git reference such as a branch, tag, or SHA1 hash.  e.g. <tt>"master"</tt>
      @returns            A <tt>String</tt> which contains the contents of the file requested.
    */
    public String getFile(String project, String file_path, String ref) {
        def response = this.fetch(this.gh_api + "repos/${project}/contents/${file_path}?ref=${ref}")
        def security = new securityIO()
        return security.decodeBase64String(response['content'])
    }

    /**
      Get the directory listing of a path from a project.  This is meant to be a standard function for Jervis to interact with remotes.  All remotes are required to have this function.

      @param   project    A GitHub project including the org.  e.g. <tt>samrocketman/jervis</tt>
      @param   dir_path   A path to a directory relative to the root of the Git repository.  e.g. <tt>/</tt>
      @param   ref        A git reference such as a branch, tag, or SHA1 hash.  e.g. <tt>master</tt>
      @returns            An <tt>ArrayList</tt> which contains the contents of the file requested.
    */
    public ArrayList getFolderListing(String project, String dir_path, String ref) {
        if(dir_path.length() > 0 && dir_path[0] != '/') {
            dir_path = '/' + dir_path
        }
        ArrayList listing = []
        def response = this.fetch(this.gh_api + "repos/${project}/contents${dir_path}?ref=${ref}")
        response.each {
            listing << it.name
        }
        return listing
    }

    /**
      Get a human readable string for this type of remote.  This is meant to be a standard function for Jervis to interact with remotes.  All remotes are required to have this function.

      @return A human readable <tt>String</tt> for this type of remote.  Value returned will be either <tt>"GitHub"</tt> or <tt>"GitHub Enterprise"</tt> depending on whether or not <tt>{@link #gh_web}</tt> has been set.
     */
    public String toString() {
        if(gh_web == 'https://github.com/') {
            return 'GitHub'
        }
        else {
            return 'GitHub Enterprise'
        }
    }
}
