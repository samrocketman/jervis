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
package net.gleske.jervis.remotes

import net.gleske.jervis.remotes.interfaces.JervisRemote
import net.gleske.jervis.remotes.interfaces.TokenCredential
import net.gleske.jervis.tools.SecurityIO

/**
   A simple class to interact with the GitHub v3 API for only the parts I need.

   <h2>Sample usage</h2>
   <p>To run this example, clone Jervis and execute <tt>./gradlew console</tt>
   to bring up a <a href="http://groovy-lang.org/groovyconsole.html"
   target="_blank">Groovy Console</a> with the classpath set up.</p>

<pre><code>
import net.gleske.jervis.remotes.GitHub

def x = new GitHub()
println 'Print each branch.'
x.branches('samrocketman/jervis').each{ println it }
println 'Print the contents of .travis.yml from the main branch.'
println x.getFile('samrocketman/jervis','.travis.yml','main')</code></pre><br>
 */
class GitHub implements JervisRemote, SimpleRestServiceSupport {

    private static final String DEFAULT_URL = 'https://api.github.com/'
    private static final String DEFAULT_WEB_URL = 'https://github.com/'
    private static final String DEFAULT_GHE = 'https://github.com/api/v3/'

    /**
      Optional HTTP headers that can be added to every request.
      */
    Map headers = [:]

    @Override
    String baseUrl() {
        this.gh_api
    }

    @Override
    Map header(Map headers = [:]) {
        Map tempHeaders = this.headers + headers
        if(!('Accept' in tempHeaders.keySet())) {
            tempHeaders['Accept'] = 'application/vnd.github.v3+json'
        }
        if(!('X-GitHub-Api-Version' in tempHeaders.keySet())) {
            tempHeaders['X-GitHub-Api-Version'] = '2022-11-28'
        }
        if(this.getGh_token()) {
            tempHeaders['Authorization'] = "Bearer ${this.getGh_token()}".toString()
        }
        tempHeaders
    }

    /**
      URL to the GitHub web interface. Default: <tt>https://github.com/</tt>
     */
    String gh_web = DEFAULT_WEB_URL

    /**
      URL to the <a href="https://developer.github.com/v3/" target="_blank">GitHub v3 API</a>. For GitHub Enterprise it should be <tt>{@link #gh_web} + 'api/v3/'</tt>.  Default: <tt>https://api.github.com/</tt>
     */
    String gh_api = DEFAULT_URL

    /**
      The base clone URI in which repositories will be cloned.  Default: <tt>https://github.com/</tt>
     */
    String gh_clone = DEFAULT_WEB_URL

    /**
      The <a href="https://github.com/blog/1509-personal-api-tokens" target="_blank">API token</a>, which can be used to communicate with GitHub using authentication.  Default: <tt>null</tt>
     */
    String gh_token

    /**
       A credential for interacting with an external credential store.  If this
       is defined or set, then <tt>{@link #gh_token}</tt> is ignored and not used.
       Default: <tt>null</tt>
      */
    TokenCredential credential
    void setCredential(TokenCredential c) {
        this.credential = c
    }

    /**
       Retrieves the token used to authenticate with GitHub.  If
       <tt>{@link #credential}</tt> is set, then this will get the credential
       token, instead of <tt>{@link #gh_token}</tt>.

       @return A personal access token or an OAuth access token typically.
      */
    String getGh_token() {
        (this.credential) ? this.credential.getToken() : this.gh_token
    }

    /**
       Sets the token to be used by GitHub.  If <tt>{@link #credential}</tt> is
       set, then this will set the credential token, instead of
       <tt>{@link #gh_token}</tt>.

       @param token A personal access token or an OAuth access token typically.
      */
    void setGh_token(String token) {
        if(this.credential) {
            this.credential.setToken(token)
        }
        else {
            this.gh_token = token
        }
    }

    /*
     * Setters for internal variables
     */

    /**
      Sets the <tt>{@link #gh_web}</tt> and <tt>{@link #gh_api}</tt> properties.  This automatically sets <tt>gh_api</tt> based on <tt>gh_web</tt>.
     */
    //gh_web will always end with a trailing slash
    void setGh_web(String gh_web) {
        gh_web = (gh_web[-1] == '/')? gh_web : gh_web + '/'
        this.gh_web = (gh_web == DEFAULT_WEB_URL)? DEFAULT_WEB_URL : gh_web
        this.setGh_api(this.gh_web + 'api/v3/')
    }

    /**
      Sets the <tt>{@link #gh_api}</tt> property.
     */
    //gh_api will always end with a trailing slash
    void setGh_api(String gh_api) {
        gh_api = (gh_api[-1] == '/')? gh_api : gh_api + '/'
        this.gh_api = (DEFAULT_GHE == gh_api)? DEFAULT_URL : gh_api
    }

    /**
      Sets the <tt>{@link #gh_clone}</tt> property.
     */
    //gh_clone will always end with a trailing slash
    void setGh_clone(String gh_clone) {
        this.gh_clone = (gh_clone[-1] == '/')? gh_clone : gh_clone + '/'
    }

    /*
     * private functions
     */

    /**
      Fetches a <tt>{@link URL}</tt> from GitHub API.  This is mostly used by other
      functions to provide minimum functionality defined in
      <tt>{@link JervisRemote}</tt>.  It can be used for general GitHub API
      communication.
      @param path A GitHub API path to fetch.  The URL must return JSON content.
                  e.g. <tt>user/repos</tt>.
      @return     A <tt>Map</tt> or <tt>List</tt> from the parsed JSON response.
    */
    public def fetch(String path) {
        apiFetch(path)
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
        List parsed = ['']
        int count = 1
        while(parsed.size() > 0) {
            parsed = this.fetch("repos/${project}/branches?page=${count}")
            parsed.each { list << it.name }
            count++
        }
        return list
    }

    /**
      Get the contents of a file from a project.  This is meant to be a standard function for Jervis to interact with remotes.  All remotes are required to have this function.

      @param   project    A GitHub project including the org.  e.g. <tt>"samrocketman/jervis"</tt>
      @param   file_path  A path to a file relative to the root of the Git repository.  e.g. <tt>".travis.yml"</tt>
      @param   ref        A git reference such as a branch, tag, or SHA1 hash.  e.g. <tt>"main"</tt>.  This option is optional.  If not specified the default branch is selected.
      @returns            A <tt>String</tt> which contains the contents of the file requested.
    */
    public String getFile(String project, String file_path, String ref = '') {
        String path
        if(ref) {
            path = "repos/${project}/contents/${file_path}?ref=${java.net.URLEncoder.encode(ref)}"
        }
        else {
            path = "repos/${project}/contents/${file_path}"
        }
        def response = this.fetch(path)
        def security = new SecurityIO()
        return security.decodeBase64String(response['content'])
    }

    /**
      Get the directory listing of a path from a project.  This is meant to be a standard function for Jervis to interact with remotes.  All remotes are required to have this function.

      @param   project    A GitHub project including the org.  e.g. <tt>samrocketman/jervis</tt>
      @param   dir_path   A path to a directory relative to the root of the Git repository.  This is optional.  By default is <tt>/</tt> (the repository root).
      @param   ref        A git reference such as a branch, tag, or SHA1 hash.  e.g. <tt>main</tt>.  This option is optional.
      @returns            An <tt>ArrayList</tt> which contains the contents of the file requested.
    */
    public ArrayList getFolderListing(String project, String dir_path = '/', String ref = '') {
        if(!dir_path?.startsWith('/')) {
            dir_path = '/' + dir_path
        }
        String path
        if(ref) {
            path = "repos/${project}/contents${dir_path}?ref=${java.net.URLEncoder.encode(ref)}"
        }
        else {
            path = "repos/${project}/contents${dir_path}"
        }
        ArrayList listing = []
        def response = this.fetch(path)
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

    /**
      Check with the GitHub API and determine if the passed in <tt>user</tt> is a User or an Organization.
      @param user A user name or organization name to test if it is a user.
      @returns <tt>true</tt> if it is a user or <tt>false</tt> if it is not a user.
     */
    public boolean isUser(String user) {
        return 'User'.equals(this.fetch("users/${user}")['type'])
    }
}
