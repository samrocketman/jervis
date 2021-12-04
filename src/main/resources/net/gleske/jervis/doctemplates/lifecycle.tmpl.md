<% if(onlyEntry) { %>Because ${friendlyName} support is basic there's only one default lifecycle.

```yaml
language: ${language}
install: ${buildtool.install ?: '# no command run'}
script: ${buildtool.script ?: '# no command run'}
```
<% } else { %><% if(!lastEntry) { %>
If a file named `${buildtool.fileExistsCondition}` is in the repository root, then the following `install` and `script` defaults will be run.

```yaml
language: ${language}
install: ${buildtool.install ?: '# no command run'}
script: ${buildtool.script ?: '# no command run'}
```<% if(nextFile) { %>

If `${buildtool.fileExistsCondition}` does not exist, then the system will fall back to finding a file named `${nextFile}`.<% } %><% } else { %>
Otherwise, ${friendlyName} falls back to the following default YAML with no further defaults detection.

```yaml
language: ${language}
install: ${buildtool.install ?: '# no command run'}
script: ${buildtool.script ?: '# no command run'}
```<% } %><% } %>
