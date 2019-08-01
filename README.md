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


## Using the checker

There are [separate instructions](README-LOMBOK.md) if your project uses Lombok.

1. Build the checker by running the following commands from your shell:

  ```bash
  git clone https://github.com/msridhar/returnsrecv-checker.git
  ./gradlew -p returnsrecv-checker build install
  git clone https://github.com/kelloggm/object-construction-checker.git
  ./gradlew -p object-construction-checker build install
  ```

2. Make your Maven/Gradle project depend on `net.sridharan.objectconstruction:object-construction-checker:0.1-SNAPSHOT`.
Build systems other than Maven and Gradle are not yet supported.

3. Run `javac` normally.
The checker includes a manifest file defining an annotation processor, meaning that `javac` will run it
automatically if it is on your compile classpath (as long as no annotation processors are explicitly specified).

## Specifying your code

The Object Construction Checker works as follows:
 * It reads method specifications or contracts:  what they require when they are called.
 * It keeps track of which methods have been called on each object.
 * It warns if method arguments do not satisfy the method's specification.

Most specifications are automatically inferred by the Object Construction
Checker.  For example, it determines the specification of `build()` from
`@Nullable` annotations, among other sources.

In some cases, you may need to specify your code.  You do so by
writing type annotations.  A type annotation is written before a type.  For
example, in `@NonEmpty List<@Regex String>`, `@NonEmpty` is a type
annotation on `List`, and `@Regex` is a type annotation on `String`.

## Type annotations

The two most important type annotations are:
<dl>
<dt>`@CalledMethods(<em>methodname</em>...)`</dt>
<dd>specifies that a value must have had all the given methods called on it.
(Other methods might also have been called.)

Suppose that method `build` is annotated as
```
class MyBuilder {
  MyObject build(@CalledMethods({"setX", "setY"}) MyBuilder this) { ... }
}
```
Then the receiver for any call to build() must have had `setX` and `setY` called on it.
</dd>

<dt>`@CalledMethodsPredicate(<em>logical-expression</em>)`</dt>
<dd>specifies the required method calls using Java boolean syntax.

For example, the annotation `@CalledMethodsPredicate("x && y || z")` on a type represents
objects such that:
* both the `x()` and `y()` methods have been called on the object, **or**
* the `z()` method has been called on the object.
</dd>
</dl>

The typechecker also supports (and depends on) the
[Returns Receiver Checker](https://github.com/msridhar/returnsrecv-checker), which provides the
`@This` annotation. `@This` on a method return type means that the method returns its receiver;
this checker uses that information to persist sets of known method calls in fluent APIs.

## Type hierarchy (subtyping)

In `@CalledMethods`, larger sets induce types that are lower in the type hierarchy.
More formally, let &#8849; represent subtyping.  Then
`@CalledMethods(`*set1*`) T1` &#8849; `@CalledMethods(`*set2*`) T2` if  *set1 &supe; set2* and T1 $#8849; T2.

No `@CalledMethodsPredicate` annotation is ever a subtype of another, or of
any `@CalledMethods` annotation. For this reason,
users should only write `@CalledMethodsPredicate` annotations on the parameters of
methods (usually in stub files).

To determine whether `@CalledMethods(`*M*`)` &&#8849; `@CalledMethodsPredicate(`*P*`)`,
use the following procedure:

1. For each *x* in *M*, replace all instances of *x* in *P* with *true*.
2. For every other literal *y* in *P*, replace *y* with *false*.
3. Evaluate *P* and use its result.

## The `@CalledMethodsPredicate` NOT operator (`!`)

The boolean syntax accepted by `@CalledMethodsPredicate` includes a NOT operator (`!`).
The annotation `@CalledMethodsPredicate("!x")` means: "it isn't the case that x was
definitely called" or "x was not called on every possible path" rather than
"x is not called" or "x was not called on any path." This means that the `!` operator
is primarily useful for unsound bug-finding rather than verification: it can be used
to prove that a method was definitely called when it should not have been.
Similarly, the `!` operator can be used to prove that two methods that ought to be
mutually exclusive were definitely called together by writing
`@CalledMethodsPredicate("!(a && b)")`. However, it cannot be used to prove the
absence of paths on which two mutually-exclusive methods were called.

For more details
on the syntax accepted by `@CalledMethodsPredicate`, see the documentation for
the
[Spring Expression Language (SPEL)](https://docs.spring.io/spring/docs/3.0.x/reference/expressions.html),
whose parser it uses.


## More information

The Object Construction Checker is built upon the [Checker
Framework](https://checkerframework.org/).  The [Checker Framework
Manual](https://checkerframework.org/manual/) gives more information about
using pluggable type-checkers.
