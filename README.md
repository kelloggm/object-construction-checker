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

If your project uses Lombok, also see the further Lombok-specific instructions below.

1. Add the [org.checkerframework](https://github.com/kelloggm/checkerframework-gradle-plugin) Gradle plugin to the `plugins` block of your `build.gradle` file:

  ```groovy
  plugins {
      ...
      id "org.checkerframework" version "0.4.0"
  }
  ```

Note that the Gradle plugin is updated frequently.  We recommend you use the latest version shown [here](https://plugins.gradle.org/plugin/org.checkerframework).

2. For a vanilla Gradle project, add the following to your `build.gradle` file (adding the entries to the extant `repositories` and `dependencies` blocks if present).
If your project has subprojects or you need other customizations, see the documentation for the
[org.checkerframework](https://github.com/kelloggm/checkerframework-gradle-plugin) plugin.

  ```groovy
  repositories {
      mavenCentral()
  }
  checkerFramework {
      skipVersionCheck = true
      checkers = ['org.checkerframework.checker.objectconstruction.ObjectConstructionChecker']
      extraJavacArgs = ['-AsuppressWarnings=type.anno.before']
  }
  dependencies {
      checkerFramework 'net.sridharan.objectconstruction:object-construction-checker:0.1.7'
      implementation 'net.sridharan.objectconstruction:object-construction-qual:0.1.7'
  }
  ```

3. Build your project normally, such as by running `./gradlew build`.  The checker will report an error if any required properties have not been set.

### For Lombok users

The Object Construction Checker supports projects that use Lombok via the [io.freefair.lombok](https://plugins.gradle.org/plugin/io.freefair.lombok) Gradle plugin.  For such projects, the above instructions should work unmodified for running the checker.  However, note that to fix issues, you should edit your original source code, **not** the files in the checker's error messages.  The checker's error messages refer to Lombok's output, which is a variant of your source code that appears in a `delombok` directory.


## Specifying your code

The Object Construction Checker works as follows:
 * It reads method specifications or contracts:  what a method requires when it is called.
 * It keeps track of which methods have been called on each object.
 * It warns if method arguments do not satisfy the method's specification.

If you use AutoValue or Lombok, most specifications are automatically
inferred by the Object Construction Checker, from field annotations such as
`@Nullable` and field types such as `Optional`. See the
 [section on defaulting rules for Lombok and AutoValue for more details](#default-handling-for-lombok-and-autovalue).

In some cases, you may need to specify your code.  You do so by writing
type annotations.  A type annotation is written before a type.  For
example, in `@NonEmpty List<@Regex String>`, `@NonEmpty` is a type
annotation on `List`, and `@Regex` is a type annotation on `String`.

### Type annotations

The most important type annotations are:
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

<dt><code>@EnsureCalledMethods(<em>expression, method-list</em>)</code></dt>
<dd>specifies a post-condition on a method, indicating the methods it guarantees to be called on some
input expression.  The expression is specified [as documented in the Checker Framework manual](https://checkerframework.org/manual/#java-expressions-as-arguments).


For example, the annotation `@EnsuresCalledMethods(value = "#1", methods = {"x","y"})` on a method
`void m(Param p)` guarantees that `p.x()` and `p.y()` will always be called before `m` returns.
</dd>

<dt><code>@This</code></dt>
<dd>may only be written on a method return type, and means that the method returns its receiver.
This is helpful when type-checking fluent APIs.
</dd>
</dl>

The fully-qualified names of the annotations are:\
`org.checkerframework.checker.objectconstruction.qual.CalledMethods`\
`org.checkerframework.checker.objectconstruction.qual.CalledMethodsPredicate`\
`org.checkerframework.checker.objectconstruction.qual.EnsuresCalledMethods`\
`org.checkerframework.checker.returnsrcvr.qual.This`


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

### Default handling for Lombok and AutoValue

The checker automatically inserts default annotations for code that uses builders generated
by Lombok and AutoValue. There are three places annotations are usually inserted:
* A `@CalledMethods` annotation is placed on the receiver of the `build()` method, capturing the
setter methods that must be invoked on the builder before calling `build()`. For Lombok,
this annotation's argument is the set of `@lombok.NonNull` fields that do not have default values.
For AutoValue, it is the set of fields that are not `@Nullable`, `Optional`, or a Guava Immutable
Collection.
* If the object has a `toBuilder()` method (for example, if the `toBuilder = true` option is
passed to Lombok's `@Builder` annotation), then the return type of that method is annotated with
the same `@CalledMethods` annotation as the receiver of `build()`, using the same rules as above.
* A `@This` annotation is placed on the return type of each setter in the builder's implementation.

You can disable the framework supports by specifying them in a comma-separated list to the 
command-line flag `disableFrameworkSupports`.  For example, to disable both Lombok and AutoValue supports,
use `-AdisableFrameworkSupports=AutoValue,Lombok` . 
 
If you overwrite the definition of any of these methods (for example, by adding your own setters to
a Lombok builder), you may need to write the annotations manually.

Minor notes/caveats on these rules:
* Lombok fields annotated with `@Singular` will be treated as defaulted (i.e. not required), because
Lombok will set them to empty collections if the appropriate setter is not called.
* If you manually provide defaults to a Lombok builder (for example by defining the builder yourself,
and assigning a default value to the builder's field), the checker will treat that field as defaulted
*most of the time*. In particular, it will not treat it as defaulted across module boundaries (because
the checker needs access to the source code to determine that the defaulting is occurring).

## More information

The Object Construction Checker is built upon the [Checker
Framework](https://checkerframework.org/).  The [Checker Framework
Manual](https://checkerframework.org/manual/) gives more information about
using pluggable type-checkers.
