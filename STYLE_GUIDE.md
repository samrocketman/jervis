This is the style guide to keep consistency in this project.  All pull requests
should adhere to it in order to be accepted.

# Style Guide

1.  Use consistent indenting.  Indent using four spaces.
2.  `print` and `println` statements should never use parenthesis.
3.  Use single quotes `'` when quoting string literals.  Use double quotes `"`
    when quoting strings that require string interpolation.
4.  Every conditional and loop requires curly brackets.  Even if it is only
    enclosing one statement.
5.  Opening curly brackets must be on the same line as the conditional or loop.
    Closing curly brackets must be alone on the line.
6.  Opening curly brackets must have a single space between the conditional and
    the curly bracket.
7.  Comma separated values must have a single space before the next value.
8.  When setting variables there must be a space on either side of the equal
    sign.
9.  Use specific type definitions as much as possible and only use generic type
    definitions where appropriate.
10. No semicolons at the end of code lines.  Keep lack of using semicolons
    consistent.

# Style guide examples.

Point 1. consistent indenting example.

```groovy
if(x) {
    println "Value of x: ${x}"  //<--- indented using 4 spaces
}
else {
    println 'False'             //<--- indented using 4 spaces
}
```

Point 2. Print statements should never use parenthesis.

```groovy
println 'This is a print statement without parens'
```

Point 3. Single quotes when quoting string literals and double quotes when
quoting strings that need string interpolation.

```groovy
if(x) {
    println "Value of x: ${x}"  //<--- double quoted string interpolation
}
else {
    println 'False'             //<--- single quoted literal string
}
```

Point 4. Every conditional loop requires curly brackets.

```groovy
if(x) {                         //<--- using curly brackets
    println "Value of x: ${x}"
}
else {                          //<--- using curly brackets
    println 'False'
}
```

Point 5. Opening curly brackets must be on same line as conditional or loop.
Closing curly brackets must be alone on the line.

```groovy
if(x) {                         //<--- curly bracket on the same line
    println "Value of x: ${x}"
}                               //<--- lone curly bracket
else {
    println 'False'
}
```

Point 6. Opening curly brackets must have a single space between the conditional
and the curly bracket.

```groovy
//  notice the space
//   |
//   |
//   V
if(x) {
    println "Value of x: ${x}"
}
else {
    println 'False'
}
```

Point 7. Comma separated values must have a single space before the next value.

```groovy
//               notice the space after comma
//                |
//                |
//                V
somefunction(arg1, arg2, arg3)
```

Point 8. When setting variables there must be a space on either side of the
equal sign.

```groovy
//            space before
//             |
//             |
//             V
String mystring = ""
//               ^
//               |
//               |
//            space after
```

Point 9. Use specific type definitions as much as possible and only use generic
type definitions where appropriate.  i.e. use `int mynum = 3` instead of `def
mynum = 3` if mynum will always be an integer.
