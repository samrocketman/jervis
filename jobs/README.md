# Job DSL Scripts

This folder contains scripts meant to be run by the [Job DSL Plugin][jdp].  This
documentation is not meant to be comprehensive but just give the reader enough
information to understand what's going on in the scripts.

The primary calling script is [`firstjob_dsl.groovy`](firstjob_dsl.groovy).  It
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

# Some other groovy tips

If you already know Java then here's some tips which help you relate to Groovy.

- Parenthesis on methods optional; Same goes for semicolons at the end of lines.
  For example, the following two statements are equivalent.

  ```groovy
  //import java.util.Base64;
  //java (also valid Groovy)
  Base64.getEncoder().encode("encode my string");
  //groovy
  Base64.getEncoder().encode "encode my string"
  ```

- Setters and getters are automatically generated if they don't already exist.
  If you have private variable `MyClass.myvar` then groovy will automatically
  generate `MyClass.getMyvar()` and `MyClass.setMyvar(Object var)`.
- Setters and getters have a short hand syntax.  Let's say you havethe variable
  `MyClass.myvar`.

  ```groovy
  //use automatically generated MyClass.getMyvar()
  MyClass.getMyvar()
  //use automatically generated MyClass.getMyvar()
  MyClass.myvar
  //use automatically generated MyClass.setMyvar()
  MyClass.setMyvar("hello")
  //use automatically generated MyClass.setMyvar()
  MyClass.myvar "hello"
  ```

- Private methods are accessible. Private final variables can be modified via
  reflection API.  Let's say you have a private variable in `MyClass.myvar`.
  Let's see an example.

  ```groovy
  MyClass m = new MyClass()
  //access private variable using automatically generated setter
  m.setMyvar("hello")
  //use automatically generated MyClass.setMyvar()
  m.myvar = "hello"
  //bypass the setter and access the private variable directly
  m.@myvar = "hello"
  ```

- The last executed statement is the return value.  The two following methods
  are the same in Groovy.

  ```groovy
  //java (and also Groovy)
  public String hello() {
      return "hello"
  }
  //groovy
  String hello() {
      "hello"
  }
  ```

If you wish to learn more about Groovy then see the [official Groovy
documentation][gdocs].

[closure-impl-it]: http://groovy-lang.org/closures.html#implicit-it
[closure-params]: http://groovy-lang.org/closures.html#_normal_parameters
[eval]: http://docs.groovy-lang.org/2.4.7/html/gapi/groovy/lang/Script.html#evaluate(java.lang.String)
[gdocs]: http://groovy-lang.org/learn.html
[groovy-vars]: http://groovy-lang.org/structure.html#_variables
[jdp]: https://wiki.jenkins.io/display/JENKINS/Job+DSL+Plugin
