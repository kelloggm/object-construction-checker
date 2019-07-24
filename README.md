# Object construction checker

Constructors allow the designers of a class file to exactly specify the legal ways to construct
an object. However, constructors are also inflexible and make reading code difficult:
* it is difficult to express optional arguments to constructors in Java, due to the lack of named arguments a la python
* constructor arguments are positional, so reading a constructor invocation requires the programmer to remember
the order in which arguments are meant to be supplied to the constructor

For these reasons, many programmers turn to alternative ways of constructing objects, such as the builder pattern
or dependency injection. These alternatives improve flexibility and readability, 
but they lack the precision and runtime safety of hand-written constructors. For example, when using the
builder pattern, failing to provide a required argument becomes a run-time rather than compile-time error -
making development more difficult and dangerous.

This repository contains a typechecker that extends the Java type system to catch malformed objects at
compile-time, even when they are constructed via the builder pattern. The checker has built-in support
for Lombok- and AutoValue-generated builders, and programmers can write specifications on their own
builders to indicate which arguments are legal.

## Using the checker

The checker is currently in development, so you'll have to build it locally. Run the following commands
from your shell:

```bash
./gradlew build
./gradlew publishToMavenLocal
```

Then, add a Maven/Gradle dependency to your project on `org.checkerframework:object-construction:0.1-SNAPSHOT`.
Other build systems are unsupported.

The checker includes a manifest file defining an annotation processor, meaning that `javac` will run it
automatically if it is on your compile classpath (as long as no annotation processors are explicitly specified).

## Supported annotations

There are two annotations that you might want to write to specify a builder API:
* `@CalledMethods(String[])` specifies that the given object (usually the receiver object) must have had
methods with the names given in the string arguments called on it
* `@CalledMethodsPredicate(String)` permits the programmer to specify the permitted method calls using
Java boolean syntax. 

For example, the annotation `@CalledMethodsPredicate("x and y or z")` means that
an object must meet one of the following conditions to be assignable to that location:
* both the `x()` and `y()` methods have been called on the object, or
* the `z()` method has been called on the object

The typechecker also supports (and depends on) the 
[Returns Receiver Checker](github.com/msridhar/returnsrcvr-checker), which provides the
`@This` annotation. `@This` on a method return type means that the method returns its receiver;
this checker uses that information to persist sets of known method calls in fluent APIs.
