# Object Construction Checker

The builder pattern is a flexible and readable, but error-prone, way to
construct objects.  For example, failing to provide a required argument is
a run-time error that manifests during testing or in the field, instead of
at compile-time as for regular Java constructors.

The Object Construction Checker verifies at compile time that your code
correctly uses the builder pattern, never omitting a required argument.
The checker has built-in support for [Lombok](https://projectlombok.org/)
and
[AutoValue](https://github.com/google/auto/blob/master/value/userguide/index.md).
Programmers can extend it to other builders by writing method
specifications.


## Using the checker with Lombok

There are separate instructions for [using the Object Construction Checker with Lombok](README-LOMBOK.md).


## Using the checker

1. Build the checker by running the following commands from your shell:

```bash
git clone https://github.com/mernst/returnsrecv-checker.git
./gradlew -p returnsrecv-checker build publishToMavenLocal
git clone https://github.com/kelloggm/typesafe-builder-checker.git
./gradlew -p typesafe-builder-checker build publishToMavenLocal
```

2. Make your Maven/Gradle project depend on `org.checkerframework:typesafe-builder:0.1-SNAPSHOT`.
Build systems other than Maven and Gradle are not yet supported.

3. Run `javac` normally.
The checker includes a manifest file defining an annotation processor, meaning that `javac` will run it
automatically if it is on your compile classpath (as long as no annotation processors are explicitly specified).

## Specifying your code

You can specify your code's contracts (what it expects from clients) by writing type annotations.
A type annotation is written before a type.  For example, in `@NonEmpty List<@Regex String>`, `@NonEmpty` is a type annotation on `List`, and `@Regex` is a type annotation on `String`.

The two most relevant annotations are:
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
</dd>permits the
programmer to specify the permitted method calls using Java boolean syntax. 

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


## More information

The Object Construction Checker is built upon the [Checker
Framework](https://checkerframework.org/).  The [Checker Framework
Manual](https://checkerframework.org/manual/) gives more information about
using pluggable type-checkers.
