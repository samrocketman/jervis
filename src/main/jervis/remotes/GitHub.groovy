/**
   A simple class to interact with the GitHub API for only the parts I need.

   <h2>Sample usage</h2>

<pre><code>import jervis.remotes.GitHub
def x = new GitHub()
println "Print each branch."
x.branches('samrocketman/jervis').each{ println it }
println "Print the contents of .travis.yml from the master branch."
println x.getFile('samrocketman/jervis','.travis.yml','master')
</code></pre>
 */

package jervis.remotes

import groovy.json.JsonSlurper
import jervis.tools.securityIO

class GitHub {
    /**
      URL to the GitHub web interface. Default: <code>https://github.com/</code>
     */
    String gh_web = "https://github.com/"
    /**
      URL to the GitHub API. For GitHub Enterprise it should be <code>gh_web + "api/v3/"</code>.  Default: <code>https://api.github.com/</code>

      @see https://developer.github.com/v3/
      @see <a href="http://google.com">http://google.com</a>
     */
    String gh_api = "https://api.github.com/"
    /**
      The base clone URI in which repositories will be cloned.  Default: <code>git://github.com/</code>
     */
    String gh_clone = "git://github.com/"
    /**
      The API token which can be used to communicate with GitHub using authentication.  Default: <code>null</code>

      @see <a href="https://github.com/blog/1509-personal-api-tokens" target="_blank">API token</a>
     */
    String gh_token

    /*
     * Setters for internal variables
     */
    /**
      Sets the <code>gh_web</code> and <code>gh_api</code> properties.  This automatically sets <code>gh_api</code> based on <code>gh_web</code>.
     */
    //gh_web will always end with a trailing slash
    void setGh_web(gh_web) {
        this.gh_web = (gh_web[-1] == '/')? gh_web : gh_web + '/'
        this.setGh_api(this.gh_web + 'api/v3/')
    }
    /**
      Sets the <code>gh_api</code> property.
     */
    //gh_api will always end with a trailing slash
    void setGh_api(gh_api) {
        this.gh_api = (gh_api[-1] == '/')? gh_api : gh_api + '/'
    }
    /**
      Sets the <code>gh_clone</code> property.
     */
    //gh_clone will always end with a trailing slash
    void setGh_clone(gh_clone) {
        this.gh_clone = (gh_clone[-1] == '/')? gh_clone : gh_clone + '/'
    }
    /**
      Sets the <code>gh_token</code> property.
     */
    //gh_token should be null if it is a zero length string.
    void setGh_token(gh_token) {
        this.gh_token = (gh_token.toString().length() > 0)? gh_token : null
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
            return json.parse(addr.toURL().newReader(requestProperties: ["Authorization": "token ${this.gh_token}".toString(), "Accept": "application/json"]))
        }
        else {
            return json.parse(addr.toURL().newReader())
        }
    }

    /*
     * public functions *
     */
    /**
      Get the contents of <code>gh_web</code>.  This is meant to be a standard function for Jervis to interact with remotes.  All remotes are guaranteed to have this function.

      @return The contents of <code>gh_web</code>.
     */
    public String getWebUrl() {
        gh_web
    }
    /**
      Get the contents of <code>gh_clone</code>.  This is meant to be a standard function for Jervis to interact with remotes.  All remotes are guaranteed to have this function.

      @return The contents of <code>gh_clone</code>.
     */
    public String getCloneUrl() {
        gh_clone
    }
    /**
      Get a list of branches for a project.  This is meant to be a standard function for Jervis to interact with remotes.  All remotes are guaranteed to have this function.

      @param  project  A GitHub project including the org.  e.g. <code>"samrocketman/jervis"</code>
      @return          A <code>List</code> where each element is a branch in the project.
     */
    public List branches(String project) {
        def list = []
        this.fetch(this.gh_api + "repos/${project}/branches").each { list << it.name }
        return list
    }
    /**
      Get the contents of a file from a project.  This is meant to be a standard function for Jervis to interact with remotes.  All remotes are guaranteed to have this function.

      @param   project    A GitHub project including the org.  e.g. <code>"samrocketman/jervis"</code>
      @param   file_path  A path to a file relative to the root of the Git repository.  e.g. <code>".travis.yml"</code>
      @param   ref        A git reference such as a branch, tag, or SHA1 hash.  e.g. <code>"master"</code>
      @returns            A <code>String</code> which contains the contents of the file requested.
    */
    public String getFile(String project, String file_path, String ref) {
        def response = this.fetch(this.gh_api + "repos/${project}/contents/${file_path}?ref=${ref}")
        def security = new securityIO()
        return security.decodeBase64String(response['content'])
    }
    /**
      Get a human readable string for this type of remote.  This is meant to be a standard function for Jervis to interact with remotes.  All remotes are guaranteed to have this function.

      @return A human readable <code>String</code> for this type of remote.
     */
    public String type() {
        if(gh_web == "https://github.com/") {
            return "GitHub"
        }
        else {
            return "GitHub Enterprise"
        }
    }
}
