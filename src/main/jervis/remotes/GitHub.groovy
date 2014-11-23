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

class GitHub {
    def gh_web = "https://github.com/"
    def gh_api = "https://api.github.com/"
    def gh_token
    //gh_web will always end with a trailing slash
    void setGh_web(gh_web) {
        this.gh_web = (gh_web[-1] == '/')? gh_web : gh_web << '/'
    }
    //gh_api will always end with a trailing slash
    void setGh_api(gh_api) {
        this.gh_api = (gh_api[-1] == '/')? gh_api : gh_api << '/'
    }

    void setGh_token(gh_token) {
        this.gh_token = (gh_token.toString().length() > 0)? gh_token : null
    }
    /*
       fetch() - reaches out to a URL which returns json content.
       returns a HashMap from the json object.
    */
    private String fetch(String addr) {
        def json = new JsonSlurper()
        if(this.gh_token) {
            return json.parse(addr.toURL().newReader(requestProperties: ["Authorization": "token ${this.gh_token}".toString(), "Accept": "application/json"]))
        }
        else {
            return json.parse(addr.toURL().newReader())
        }
    }
    /*
       decodeBase64() - decodes base64 strings
       returns a string of the decoded result.
    */
    private String decodeBase64(content) {
        return new String(content.toString().decodeBase64())
    }
    /*
       getWebEndpoint() returns the contents of gh_web.
       gh_web is publicly accessible but this method was provided to keep a generic getting method.
       This is future looking for other possible API endpoints (such as GitLab instead of GitHub).

       Always use obj.getWebEndpoint() instead of obj.gh_web.
    */
    def getWebEndpoint() {
        gh_web
    }
    def branches(project) {
        def list = []
        this.fetch("https://api.github.com/repos/${project}/branches").each { list << it.name }
        return list
    }
    def getFile(project, path, ref) {

    }
}

