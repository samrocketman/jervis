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
package net.gleske.jervis.exceptions;

/**
  A simple class, whose only purpose is to house static strings referencing the <a href="https://github.com/samrocketman/jervis/wiki" target="_blank">Jervis wiki</a>,
  to be used as helpful hints when throwing exceptions.

  <h2>Overriding URLs</h2>
  Each of the <tt>WikiPages</tt> class URLs can be overridden by making use of
  Groovy meta programming.  This is a technique which modifies static classes
  during the runtime.  Groovy automatically generates a camel cased setter and
  getter methods for class properties.  Here's a list of the generated getter
  methods for this class.

  <ul>
    <li><tt>{@link #SUPPORTED_LANGUAGES}</tt> has the getter <tt>getSupportedLanguages()</tt></li>
    <li><tt>{@link #SUPPORTED_TOOLS}</tt> has the getter <tt>getSupportedTools()</tt></li>
    <li><tt>{@link #LIFECYCLES_SPEC}</tt> has the getter <tt>getLifecyclesSpec()</tt></li>
    <li><tt>{@link #TOOLCHAINS_SPEC}</tt> has the getter <tt>getToolchainsSpec()</tt></li>
    <li><tt>{@link #PLATFORMS_SPEC}</tt> has the getter <tt>getPlatformsSpec()</tt></li>
  </ul>

  <h2>Why bother overridding URLs?</h2>

  Why would one bother to override these URLs?  Because this library is meant to
  be used with Jenkins Job DSL scripts, it is assumed that users would host their
  own DSL scripts internally.  If there's a contributing workflow then it makes
  sense to refer contributors to internally hosted documentation when displaying
  error messages.  That's the intention of this explanation.

  <h2>Sample usage</h2>

  <p>To run this example, clone Jervis and execute <tt>./gradlew console</tt>
  to bring up a <a href="http://groovy-lang.org/groovyconsole.html" target="_blank">Groovy Console</a>
  with the classpath set up.</p>

  <p>When overriding the URLs you must override the getter method using meta
  programming.  The following is an example of overriding the
  <tt>{@link #LIFECYCLES_SPEC}</tt>.</p>

<pre><code class="language-groovy">
import net.gleske.jervis.exceptions.WikiPages
WikiPages.metaClass.static.getLifecyclesSpec = {-&gt; 'https://wiki.example.com/lifecycle_explanation.html'}

import net.gleske.jervis.lang.LifecycleValidator

LifecycleValidator lifecycles = new LifecycleValidator()
lifecycles.loadYamlString('''
ruby:
  defaultKey: rake1
  rake1:
    fileExistsCondition: Gemfile.lock
    fallbackKey: rake2
    env: export BUNDLE_GEMFILE=$PWD/Gemfile
    install: bundle install --jobs=3 --retry=3 --deployment
    script: bundle exec rake
  rake2:
    env: export BUNDLE_GEMFILE=$PWD/Gemfile
    install: bundle install --jobs=3 --retry=3
    script: bundle exec rake
''')
lifecycles.validate()
</code></pre>

  The important part of the above example is the following excerpt.

<pre><code class="language-groovy">
import net.gleske.jervis.exceptions.WikiPages
WikiPages.metaClass.static.getLifecyclesSpec = {-&gt; 'https://wiki.example.com/lifecycle_explanation.html'}

import net.gleske.jervis.lang.LifecycleValidator
</code></pre>

  What is important is that we modified the <tt>WikiPages</tt> class
  <strong>before</strong> we imported the <tt>{@link net.gleske.jervis.lang.LifecycleValidator}</tt> class.
  This is important because the class can't be statically modified from within the
  <tt>LifecycleValidator</tt> after it is imported.

  Here's an example error message from the above sample.

 <pre><tt>net.gleske.jervis.exceptions.LifecycleMissingKeyException:
ERROR: Lifecycle validation failed.  Missing key: ruby.friendlyName

See wiki page:
https://wiki.example.com/lifecycle_explanation.html


    at net.gleske.jervis.lang.LifecycleValidator$_validate_closure1.doCall(LifecycleValidator.groovy:118)
    at net.gleske.jervis.lang.LifecycleValidator.validate(LifecycleValidator.groovy:112)
    at net.gleske.jervis.lang.LifecycleValidator$validate$0.call(Unknown Source)</tt></pre>
 */
public class WikiPages {

    /**
      Throw an error if a user attempts to instantiate an instance of <tt>WikiPages</tt>.
      */
    private WikiPages() {
        throw new IllegalStateException("This utility class is only meant for referencing static properties.  This is not meant to be instantiated.");
    }

    /**
      A static reference to the <a href="https://github.com/samrocketman/jervis/wiki/Supported-Languages" target="_blank">supported languages wiki page</a>.
     */
    public static final String SUPPORTED_LANGUAGES = "https://github.com/samrocketman/jervis/wiki/Supported-Languages";

    /**
      A static reference to the <a href="https://github.com/samrocketman/jervis/wiki/Supported-Tools" target="_blank">supported tools wiki page</a>.
     */
    public static final String SUPPORTED_TOOLS = "https://github.com/samrocketman/jervis/wiki/Supported-Tools";

    /**
      A static reference to the <a href="https://github.com/samrocketman/jervis/wiki/Specification-for-lifecycles-file" target="_blank">lifecycles file spec wiki page</a>.
     */
    public static final String LIFECYCLES_SPEC = "https://github.com/samrocketman/jervis/wiki/Specification-for-lifecycles-file";

    /**
      A static reference to the <a href="https://github.com/samrocketman/jervis/wiki/Specification-for-toolchains-file" target="_blank">toolchains file spec wiki page</a>.
     */
    public static final String TOOLCHAINS_SPEC = "https://github.com/samrocketman/jervis/wiki/Specification-for-toolchains-file";

    /**
      A static reference to the <a href="https://github.com/samrocketman/jervis/wiki/Specification-for-platforms-file" target="_blank">platforms file spec wiki page</a>.
     */
    public static final String PLATFORMS_SPEC = "https://github.com/samrocketman/jervis/wiki/Specification-for-platforms-file";

    /**
      A static reference to the <a href="https://github.com/samrocketman/jervis/wiki/Secure-secrets-in-repositories" target="_blank">secure secrets in repositories wiki page</a>.
     */
    public static final String SECURE_SECRETS = "https://github.com/samrocketman/jervis/wiki/Secure-secrets-in-repositories";

    /**
      A static reference to the <a href="https://github.com/samrocketman/jervis/wiki/Pipeline-support" target="_blank">pipeline support wiki page</a>.
     */
    public static final String PIPELINE_SUPPORT = "https://github.com/samrocketman/jervis/wiki/Pipeline-support";
}
