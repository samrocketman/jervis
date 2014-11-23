/*
   Created by Sam Gleske
   A simple class to interact with the GitHub API for only the parts I need.
 */
//Sample usage
//x = new GitHub()
//x.gh_web = "http://myserver/"
//x.gh_api = "http://myserver/v3/api/"
//x.gh_token = "sometoken"
//x.branches('user/project','master')
//x = new GitHub()
//x.fetch('https://api.github.com/repos/trumant/gerry/contents/.travis.yml')

package jervis.remotes

import groovy.json.JsonSlurper

//following imports are only used by fileExists() function
import groovyx.net.http.HTTPBuilder
import static groovyx.net.http.Method.HEAD
import static groovyx.net.http.ContentType.JSON

class GitHub {
    def gh_web = "https://github.com/"
    def gh_api = "https://api.github.com/"
    def gh_clone = "git://github.com/"
    def gh_token

    /**********************************\
     * Setters for internal variables *
    \**********************************/
    //gh_web will always end with a trailing slash
    void setGh_web(gh_web) {
        this.gh_web = (gh_web[-1] == '/')? gh_web : gh_web << '/'
    }
    //gh_api will always end with a trailing slash
    void setGh_api(gh_api) {
        this.gh_api = (gh_api[-1] == '/')? gh_api : gh_api << '/'
    }
    //gh_clone will always end with a trailing slash
    void setGh_clone(gh_clone) {
        this.gh_clone = (gh_clone[-1] == '/')? gh_clone : gh_clone << '/'
    }
    //gh_token should be null if it is a zero length string.
    void setGh_token(gh_token) {
        this.gh_token = (gh_token.toString().length() > 0)? gh_token : null
    }

    /*********************\
     * private functions *
    \*********************/
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
    //decode base64 strings into decoded strings
    private String decodeBase64(String content) {
        return new String(content.toString().decodeBase64())
    }

    /********************\
     * public functions *
    \********************/
    /*
       getWebEndpoint() returns the contents of gh_web.
    */
    public String getWebEndpoint() {
        gh_web
    }
    /*
       getCloneUrl() returns the contents of gh_clone.
    */
    public String getCloneUrl() {
        gh_clone
    }
    /*
       List branches(String project) - get a list of branches
       Args:
           project - A GitHub project including the org.
    */
    public List branches(String project) {
        def list = []
        this.fetch("https://api.github.com/repos/${project}/branches").each { list << it.name }
        return list
    }
    /*
       String getFile(String project, String file_path, String ref)
       Args:
           project - A GitHub project including the org.  e.g. samrocketman/jervis
           file_path - A path to a file relative to the root of the GitHub project.
           ref - a git reference.  e.g. master
       returns a String
    */
    public String getFile(String project, String file_path, String ref) {
        def response = this.fetch("https://api.github.com/repos/${project}/contents/${file_path}?ref=${ref}")
        return this.decodeBase64(response['content'])
    }
    /*
       bool fileExists(String project, String file_path, String ref)
       Args:
           project - A GitHub project including the org.
           file_path - A path to a file relative to the root of the GitHub project.
           ref - a git reference.  e.g. master
       returns a bool
    */
    def fileExists(String project, String file_path, String ref) {
        //return true if status 200
        //return false if status 404
        //throw exception for all other HTTP statuses
        return new HTTPBuilder("https://api.github.com/repos/${project}/contents/${file_path}?ref=${ref}").request(HEAD,JSON) { req ->
            //github will block request without user agent
            headers.'User-Agent' = 'samrocketman/jervis'
            if(this.gh_token) {
                headers.'Authorization' = "token ${this.gh_token}"
            }
            response.success = { resp ->
                return (resp.status >= 200 && resp.status < 300)
            }

            response.failure = { resp ->
                if(resp.status == 404) {
                    return false
                }
                throw IOException("HTTP response returned was not 200 nor 404.  HTTP response: ${resp.status}")
            }
        }
    }
    /*
        public String type()
        returns a human readable string for this type of remote.
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
