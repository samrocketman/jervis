# ${friendlyName} language lifecycle

Every language has an `install` and `script` key which runs in the `Build Project` stage of ${serviceName}.  In Jervis, this is referred to as the language **lifecycle** and is typically for running tests; However, some projects will disable this by overriding it with `true` (in favor of doing most work in a `Jenkinsfile`).  Jervis is also responsible for environment setup such as installing build tools.  In Jervis, build tool installation and environment setup is collectively referred to as the project **toolchains**.

What is run by default in the environment setup (toolchains) or in the `Build Project` stage (lifecycles) will depend on the language and build tools your project uses.  In pipeline code examples, when you see the `runToolChainsSh` step it is running an arbitrary script of your choosing with the `.jervis.yml` **toolchains** set up as well (but not the **lifecycles**).

> **PLEASE NOTE:** the `install` and `script` key do not run every provisioned build agent in the `Jenkinsfile`.  They **ONLY** run in the `Build Project` stage at the begining of a Jenkins pipeline.  If you want code to run every time a `runToolChainsSh` step is called on a build agent, then you'll want to refer to the `agent_custom_setup` toolchain in our [full list of supported toolchains][toolchains].

Override default behavior of `install` and `script` with the following YAML example.  By calling the `/bin/true` utility or `true` you're basically executing a simple shell command that will always exit "success".

```yaml
language: ${language}
install: /bin/true
script: /bin/true
jenkins:
  pipeline_jenkinsfile: .ci/Jenkinsfile
```

${friendlyName} will run the following commands by default in the `Build Project` stage.

### ${friendlyName} lifecycle defaults
