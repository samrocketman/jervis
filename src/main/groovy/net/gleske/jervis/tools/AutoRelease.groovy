/**
  This class provides utility functions helpful for continuous automated release.

  @author Sam Gleske (GitHub @samrocketman)
  */

package net.gleske.jervis.tools

import groovy.text.SimpleTemplateEngine
import java.util.regex.Pattern

/**
  This class provides automatic release utility functions such as pattern matching
  */
class AutoRelease {

    private AutoRelease() {
        throw new IllegalStateException('ERROR: This utility class only provides static methods and is not meant for instantiation.  See Java doc for this class for examples.')
    }

    /**
        Provides automatically getting the next version for
        <a href="https://semver.org/" target="_blank">semantic versioning</a>.
        This is utility function can make it easier to provided automated
        continuous release of projects which follow semantic versioning.

        <h2>For NodeJS</h2>
        NodeJS follows a very strict
        <a href="https://docs.npmjs.com/about-semantic-versioning" target="_blank">version format for semantic versioning</a>.
        NodeJS only allows the following two formats for version:
        <tt>W.X.Y</tt> and <tt>W.X.Y-Z</tt> where <tt>W</tt>, <tt>X</tt>, and
        <tt>Y</tt> are integers.  <tt>Z</tt> can be anything.  In the case of
        this method, it treats <tt>Z</tt> as a hotfix number which needs to be
        bumped.
        <table class="example">
            <tr>
                <th>Current version</th>
                <th>Git tags</th>
                <th>Tag prefix</th>
                <th>Next release returned</th>
            </tr>
            <tr>
                <td><tt>0.1.0</tt></td>
                <td><tt>0.1.1</tt>, <tt>0.1.2</tt></td>
                <td>none</td>
                <td><tt>0.1.3</tt></td>
            </tr>
            <tr>
                <td><tt>1.1.5</tt></td>
                <td><tt>1.1.6</tt>, <tt>1.1.5-1</tt>, <tt>1.1.5-2</tt></td>
                <td>none</td>
                <td><tt>1.1.5.3</tt></td>
            </tr>
            <tr>
                <td><tt>2.1.0</tt></td>
                <td><tt>client-2.1.1</tt>, <tt>client-2.1.2</tt></td>
                <td><tt>client-</tt></td>
                <td><tt>client-2.1.3</tt></td>
            </tr>
            <tr>
                <td><tt>1.3.0</tt></td>
                <td><tt>v1.1.1</tt>, <tt>v1.2.1</tt>, <tt>v1.3.1</tt>, <tt>v1.3.2</tt>, <tt>v1.3.3</tt></td>
                <td><tt>v</tt></td>
                <td><tt>v1.3.4</tt></td>
            </tr>
            <tr>
                <td><tt>1.3.0</tt></td>
                <td><tt>v1.1.1</tt>, <tt>v1.2.1</tt>, <tt>v1.3.1</tt>, <tt>v1.3.2</tt>, <tt>v1.3.3</tt></td>
                <td><tt>v</tt></td>
                <td><tt>v1.3.4</tt></td>
            </tr>
        </table>


        @param version  A version number pulled from the current commit of a
                        project (e.g. pom.xml version) which will be used to
                        determine the next release.
        @param git_tags A list of tags pulled from Git.  These will be
                        evaluated against the <tt>version</tt> and used to
                        determined what the next highest version should be.
                        This can be a list of all Git tags.  This method will
                        automatically filter for appropraite tags based on the
                        given <tt>version</tt>.
        @param prefix   A prefix which will be automatically stripped during
                        version bumping.  However, the prefix will
                        automatically be prependend to the next release.
                        <tt>prefix</tt> is optional and is empty by default.

        @return The next semantic release for the given <tt>version</tt> when a
                list of known <tt>git_tags</tt> is provided.  If a
                <tt>prefix</tt> was provided then the next semantic release
                will be prepended with the prefix.  That means if you're
                setting the next version and you don't want the prefix you'll
                have to strip it from the returned result.
      */
    static String getNextSemanticRelease(String version, List<String> git_tags, String prefix = '') {
        String currentVersion = (version -~ '-SNAPSHOT$') -~ "^\\Q${prefix}\\E"
        // nextVersion will be set and returned at the end.
        String nextVersion = ''

        if(currentVersion.contains('-')) {
            def parsed_version = currentVersion.tokenize('-')
            String partial_version = parsed_version[0]
            String hotfix_seperator = '-'
            nextVersion = getNextRelease(partial_version, git_tags, hotfix_seperator, prefix)
        }
        else{
            def parsed_version = currentVersion.tokenize('.')
            if(parsed_version.size() != 3)
            {
                throw new JervisException("ERROR: ${currentVersion} is invalid. NodeJS projects are required to strictly follow sem-ver.  Refer to https://stackoverflow.com/questions/16887993/npm-why-is-a-version-0-1-invalid")
            }
            else if(parsed_version[-1] == '0'){
                // a normal semver release
                String partial_version = parsed_version[0..1].join('.')
                nextVersion = getNextRelease(partial_version, git_tags, '.', prefix)
            }
            else{
                // In this case, version would end up being a patch for a
                // hotfix so we need to return the greatest hotfix tag
                String hotfix_seperator = '-'
                nextVersion = getNextRelease(currentVersion, git_tags, hotfix_seperator, prefix)
            }
        }
   }

    /**
        Get bumps the version with a more loosely formed format.  This
        automatic releasing method is similar to semantic versioning but it is
        not as strict.

        <h2>For Java, Python, and other languages</h2>
        <tt>-SNAPSHOT</tt> is automatically stripped from <tt>version</tt> as part of getting the next semantic version release.
        The following table shows what next version bump will be if given a
        <tt>version</tt>, a list of <tt>git_tags</tt>, and sometimes a
        <tt>prefix</tt> for known Git tags.
        <table class="example">
            <tr>
                <th>Current version</th>
                <th>Git tags</th>
                <th>Hotfix separator</tt>
                <th>Tag prefix</th>
                <th>Next release returned</th>
            </tr>
            <tr>
                <td><tt>1.0-SNAPSHOT</tt></td>
                <td><tt>1.1</tt>, <tt>1.2</tt></td>
                <td><tt>.</tt> (period)</td>
                <td>none</td>
                <td><tt>1.3</tt></td>
            </tr>
            <tr>
                <td><tt>1.1.5</tt></td>
                <td><tt>1.1.6</tt>, <tt>1.1.5.1</tt>, <tt>1.1.5.2</tt></td>
                <td><tt>.</tt> (period)</td>
                <td>none</td>
                <td><tt>1.1.5.3</tt></td>
            </tr>
            <tr>
                <td><tt>2.1-SNAPSHOT</tt></td>
                <td><tt>client-2.1.1</tt>, <tt>client-2.1.2</tt></td>
                <td><tt>.</tt> (period)</td>
                <td><tt>client-</tt></td>
                <td><tt>client-2.1.3</tt></td>
            </tr>
            <tr>
                <td><tt>1.3</tt></td>
                <td><tt>v1.1.1</tt>, <tt>v1.2.1</tt>, <tt>v1.3.1</tt>, <tt>v1.3.2</tt>, <tt>v1.3.3</tt></td>
                <td><tt>.</tt> (period)</td>
                <td><tt>v</tt></td>
                <td><tt>v1.3.4</tt></td>
            </tr>
            <tr>
                <td><tt>1.3.0</tt></td>
                <td><tt>v1.1.1</tt>, <tt>v1.2.1</tt>, <tt>v1.3.1</tt>, <tt>v1.3.2</tt>, <tt>v1.3.3</tt></td>
                <td><tt>.</tt> (period)</td>
                <td><tt>v</tt></td>
                <td><tt>v1.3.4</tt></td>
            </tr>
            <tr>
                <td><tt>1.0</tt></td>
                <td><tt>v1.1</tt>, <tt>v1.2</tt>, <tt>v1.3</tt>, <tt>v1.3.2</tt>, <tt>v1.3.3</tt></td>
                <td><tt>.</tt> (period)</td>
                <td><tt>v</tt></td>
                <td><tt>v1.4</tt></td>
            </tr>
            <tr>
                <td><tt>20200101</tt></td>
                <td><tt>20200101-1</tt>, <tt>20200101-2</tt></td>
                <td><tt>-</tt> (hyphen)</td>
                <td>none</td>
                <td><tt>20200101-3</tt></td>
            </tr>
        </table>

        Please note that if the last digit of the version number is <tt>.0</tt>
        then it is treated similarly as <tt>-SNAPSHOT</tt> and trimmed off.
        e.g. <tt>1.0.0</tt> will bump to <tt>1.0.X</tt>.

        @param version  A version number pulled from the current commit of a
                        project (e.g. <tt>pom.xml</tT> version) which will be
                        used to determine the next release.
        @param git_tags A list of tags pulled from Git.  These will be
                        evaluated against the <tt>version</tt> and used to
                        determined what the next highest version should be.
                        This can be a list of all Git tags.  This method will
                        automatically filter for appropraite tags based on the
                        given <tt>version</tt>.  If a <tt>prefix</tt> is
                        defined then tags will besearched for the
                        <tt>version</tt> prepended by the <tt>prefix</tt>.
        @param hotfix_separator
                        The separator used when hotfixes are applied past the
                        first 3 sets of decimal numbers.  By default,
                        <tt>hotfix_separator</tt> is a period (<tt>.</tt>).  A
                        hotfix release would be something like
                        <tt>1.0.3.1</tt>.  If you customize the
                        <tt>hotfix_separator</tt> to be a different character
                        such as a hyphen (<tt>-</tt>), then a hotfix release
                        would be like <tt>1.0.3-1</tt>.
        @param prefix   A prefix which will be automatically stripped during
                        version bumping.  However, the prefix will
                        automatically be prependend to the next release.
                        <tt>prefix</tt> is optional and is empty by default.

        @return The next semantic release for the given <tt>version</tt> when a
                list of known <tt>git_tags</tt> is provided.  If a
                <tt>prefix</tt> was provided then the next semantic release
                will be prepended with the prefix.  That means if you're
                setting the next version and you don't want the prefix you'll
                have to strip it from the returned result.
      */
    static String getNextRelease(String version, List<String> git_tags, String hotfix_seperator = '.', String prefix = '') {
        String major_minor = (version -~ '-SNAPSHOT$') -~ "^\\Q${prefix}\\E"
        if(major_minor.contains('.') && major_minor.tokenize('.')[-1] == '0') {
            major_minor = major_minor.tokenize('.')[0..-2].join('.')
        }
        Integer next_patch = ((git_tags.findAll {
            it =~ /^${prefix}${major_minor}${hotfix_seperator}[0-9]+$/
        }*.tokenize(hotfix_seperator)*.getAt(-1).collect {
            Integer.parseInt(it)
        }.max()) ?: 0) + 1
        "${prefix}${major_minor}${hotfix_seperator}${next_patch}"
    }

    /**
        This method is for applying variables to a groovy template.  It's purpose
        is to simplify escaping required by the Groovy interpreter when doing more
        complicated shell scripting such as using regex and needing to substitute
        Groovy variables.

        @param script A shell script which is a Groovy template meant to
                      contain variables to be replaced.
        @param variables A Map of variables to replace in the script.
      */
    static String getScriptFromTemplate(String script, Map<String, String> variables) {
        SimpleTemplateEngine engine = new SimpleTemplateEngine()
        engine.createTemplate(script).make(variables).toString()
    }

    /**
        Matches a string against an expression.  This is typically used to
        match branch names.

        @param expression If the String starts and ends with a <tt>/</tt>, then
                          it is treated as an expression for regex matching.
                          Otherwise, it is treated as a literal string to
                          match.
        @param value      The value to match against the <tt>expression</tt>.
      */
    static boolean isMatched(String expression, String value) {
        if(expression.startsWith('/') && expression.endsWith('/')) {
            return Pattern.compile(expression[1..-2]).matcher(value).matches()
        }
        else {
            return expression == value
        }
    }
}
