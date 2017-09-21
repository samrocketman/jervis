# Job DSL Scripts

This folder contains scripts meant to be run by the [Job DSL Plugin][jdp].  The
primary calling script is [`firstjob_dsl.groovy`](firstjob_dsl.groovy).  It
loads all other scripts via the groovy script binding.

The scripts in this directory have been broken out to use Groovy bindings.

# How does it work

If a variable has an undeclared type [it goes into the script
binding][groovy-vars]. The binding is visible to all methods which means data is
shared.

[`evaluate()` is a built-in helper method][eval] to allow the dynamic
evaluation of Groovy expressions using parent scripts binding as the variable
scope of the expression evaluated.  Therefore, bindings set inside of the
evaluated Groovy expression propagate to the parent script.

In a variable binding, you can [declare a closure][closure-impl-it] which
accepts no argument and must be restricted to calls without arguments.  The
following is an example using the concepts discussed so far.

```groovy
//set a binding inside of evalute()
evaluate ('test = { -> println "Test is successful!" }')
//call the binding
test()
```

Additionally, bindings can have [closures declared with
parameters][closure-params] much like methods can take arguments.  If recursion
in a binding is desired, then it must initially be declared before setting a
closure with parameters.  Here's an example of a binding which takes advantage
of having a parameter and is recursive.

```groovy
evaluate('''
factorial = null
factorial = { int n -> (n == 1)? n : n * factorial(n - 1) }
'''.trim())
assert factorial(4) == 24
```

[closure-impl-it]: http://groovy-lang.org/closures.html#implicit-it
[closure-params]: http://groovy-lang.org/closures.html#_normal_parameters
[eval]: http://docs.groovy-lang.org/2.4.7/html/gapi/groovy/lang/Script.html#evaluate(java.lang.String)
[groovy-vars]: http://groovy-lang.org/structure.html#_variables
[jdp]: https://wiki.jenkins.io/display/JENKINS/Job+DSL+Plugin
