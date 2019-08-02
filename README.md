# Object Construction Checker

The builder pattern is a flexible and readable way to construct objects, but
it is error-prone.  For example, failing to provide a required argument causes
a run-time error that manifests during testing or in the field, instead of
at compile time as for regular Java constructors.

The Object Construction Checker verifies at compile time that your code
correctly uses the builder pattern, never omitting a required argument.
The checker has built-in support for [Lombok](https://projectlombok.org/)
and
[AutoValue](https://github.com/google/auto/blob/master/value/userguide/index.md).
Programmers can extend it to other builders by writing method
specifications.

The checker performs *verification* rather than *bug-finding*.  The checker
might yield a false positive warning when your code is too tricky for it to
verify (please submit an
[issue](https://github.com/kelloggm/object-construction-checker/issues) if
you discover this).  However, if the checker issues no warnings, then you
have a guarantee that your code supplies all the required information to
the builder.


## Requirements

You must use either Maven or Gradle as your build system.

For Gradle, you must use version 4.6 or later.
Using 4.5 or later will result in an error `Could not find method
annotationProcessor() ...`.


## Using the checker

There are [separate instructions](README-LOMBOK.md) if your project uses Lombok.

1. Make your Maven/Gradle project depend on `net.sridharan.objectconstruction:object-construction-checker:0.1.1`.

  For example, for Gradle, add the following to the `build.gradle` file (adding the entries to the extant `repositories` and `dependencies` blocks if present):

  ```
  repositories {
      mavenCentral()
  }
  dependencies {
      annotationProcessor 'net.sridharan.objectconstruction:object-construction-checker:0.1.1'
      implementation 'net.sridharan.objectconstruction:object-construction-qual:0.1.1'
  }
  ```

2. Build your project normally, such as by running `./gradlew build`.
The checker includes a manifest file defining an annotation processor, meaning that `javac` will run it
automatically if it is on your compile classpath (as long as no annotation processors are explicitly specified).

## Specifying your code

The Object Construction Checker works as follows:
 * It reads method specifications or contracts:  what a method requires when it is called.
 * It keeps track of which methods have been called on each object.
 * It warns if method arguments do not satisfy the method's specification.

If you use AutoValue or Lombok, most specifications are automatically
inferred by the Object Construction Checker, from field annotations such as
`@Nullable` and field types such as `Optional`.

In some cases, you may need to specify your code.  You do so by writing
type annotations.  A type annotation is written before a type.  For
example, in `@NonEmpty List<@Regex String>`, `@NonEmpty` is a type
annotation on `List`, and `@Regex` is a type annotation on `String`.

### Type annotations

The two most important type annotations are:
<dl>
<dt><code>@CalledMethods(<em>methodName1, methodName2...</em>)</code></dt>
<dd>the annotated type represents values, on which all the given methods were definitely called.
(Other methods might also have been called.)

Suppose that method `build` is annotated as
```
class MyBuilder {
  MyObject build(@CalledMethods({"setX", "setY"}) MyBuilder this) { ... }
}
```
Then the receiver for any call to `build()` must have had `setX` and `setY` called on it.
</dd>

<dt><code>@CalledMethodsPredicate(<em>logical-expression</em>)</code></dt>
<dd>specifies the required method calls using [Java boolean syntax](https://docs.spring.io/spring/docs/3.0.x/reference/expressions.html).

For example, the annotation `@CalledMethodsPredicate("x && y || z")` on a type represents
objects such that:
* both the `x()` and `y()` methods have been called on the object, **or**
* the `z()` method has been called on the object.
</dd>

<dt><code>@This</code></dt>
<dd>may only be written on a method return type, and means that the method returns its receiver.
This is helpful when type-checking fluent APIs.
</dd>
</dl>


### Type hierarchy (subtyping)

The top element in the hierarchy is `@CalledMethods({})`.
In `@CalledMethods` annotations, larger arguments induce types that are
lower in the type hierarchy.  More formally, let &#8849; represent
subtyping.  Then

`@CalledMethods(`*set1*`) T1` &#8849; `@CalledMethods(`*set2*`) T2` iff  *set1 &supe; set2* and T1 &#8849; T2.

No `@CalledMethodsPredicate` annotation is ever a subtype of another, or of
any `@CalledMethods` annotation.  (This imprecise behavior is a limitation
of the Object Construction Checker.)  For this reason, programmers usually only
write `@CalledMethodsPredicate` annotations on formal parameters.

To determine whether `@CalledMethods(`*M*`)` &#8849; `@CalledMethodsPredicate(`*P*`)`,
use the following procedure:

1. For each *m* in *M*, replace all instances of *m* in *P* with *true*.
2. Replace every other literal in *P* with *false*.
3. Evaluate *P* and use its result.

### The NOT operator (`!`) in `@CalledMethodsPredicate`

The boolean syntax accepted by `@CalledMethodsPredicate` includes a NOT operator (`!`).
The annotation `@CalledMethodsPredicate("!x")` means: "it is not true x was
definitely called", equivalently "there is some path on which x was not called".
The annotation `@CalledMethodsPredicate("!x")` does *not* mean "x was not called".

The Object Construction Checker does not have a way of expressing that a
method must not be called.  You can do unsound bug-finding for such a
property by using the `!` operator.  The Object Construction Checker will
detect if the method was always called, but will silently approve the code
if the method is called on some but not all paths.

For example:

```
Object never, oneBranch, bothBranches;

if (somePredicate) {
  oneBranch.methodA();
  bothBranches.methodA();
} else {
  bothBranches.methodA();
}

@CalledMethodsPredicate("! methodA") Object x;
x = never;        // no warning (methodA was never called)
x = oneBranch;    // no warning (even though methodA might have been called)
x = bothBranches; // warning (methodA was definitely called)
```

Suppose that exactly one (but not both) of two methods should be called.
You can specify that via the type annotation
`@CalledMethodsPredicate("(a && !b) || (!a && b)")`.
The Object Construction Checker will find some errors.
It will soundly verify that at least one method is called.
It will warn if both methods are definitely called.
However, if will not warn if there are some paths on which both methods are called, and some paths on which only one method is called.


## More information

The Object Construction Checker is built upon the [Checker
Framework](https://checkerframework.org/).  The [Checker Framework
Manual](https://checkerframework.org/manual/) gives more information about
using pluggable type-checkers.
