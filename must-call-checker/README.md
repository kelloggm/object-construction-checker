### MustCall Checker

The MustCall Checker tracks the methods that an object should call before it is deallocated.
To enforce that the methods are actually called, use
the `-AcheckMustCall` flag to the Object Construction Checker. You should use the Object
Construction Checker rather than using the MustCall Checker alone.

The MustCall Checker produces a conservative overapproximation of the set of methods that might
actually need to be called on an object. The Object Construction Checker then conservatively assumes
that this approximation (the `@MustCall` type) must be fulfilled.

For example, an object whose type is `java.io.OutputStream` might
have an obligation to call the `close()` method, to release an underlying file resource. Or,
such an `OutputStream` might not have such an obligation, if the underlying resource is, for
example, a byte array. Either of these obligations can be represented by the static type
`@MustCall({"close"}) OutputStream`, which can be read as "an OutputStream that might need
to call close before it is deallocated". The Object Construction Checker would enforce that the
type of such an object in its hierarchy is a subtype of `@CalledMethods({"close"})` at each
point it may become unreachable.

### Explanation of qualifiers and type hierarchy

The MustCall Checker supports two qualifiers: `@MustCall` and `@MustCallUnknown`. The `@MustCall`
qualifier's arguments are the methods that the annotated type
should call. The type `@MustCall({"a"}) Object obj` means
that before `obj` is deallocated, `obj.a()` should be called.
The type hierarchy is:

                     @MustCallUnknown
                           |
                          ...
                           |
               @MustCall({"m1", "m2"})     ...
                    /             \
       @MustCall({"m1"})     @MustCall({"m2"})  ...
                    \            /
                    @MustCall({})

Note that `@MustCall({"m1", "m2"}) Object` represents more possible values than
`@MustCall({"m1"}) Object` does, because `@MustCall({"m1"}) Object` can only
contain an object that needs to call `m1` or one that needs to call nothing - an
object that needs to call `m2` (or both `m1` and `m2`) is not permitted.
`@MustCall({"m1", "m2"}) Object` represents all objects that need to
call `m1`, `m2`, both, or neither; it cannot not represent an object that needs
to call some *other* method.

The default type in unannotated code is `@MustCall({})`.
A completely unannotated program will always type-check without warnings.
For the MustCall Checker to be useful, the programmer should supply at least one non-empty
`@MustCall` annotation, either in source code or in a stub file.

The programmer should rarely (if ever) write the top annotation (`@MustCallUnknown`), because
the Object Construction Checker cannot verify it by matching with a corresponding `@CalledMethods`
annotation. `@MustCallUnknown` theoretically represents a value with an unknowable or infinite set
of must-call methods.

### Example use

If a user annotates a class declaration with a `@MustCall(...)` annotation, any use of the class's
type has that `@MustCall` annotation by default. For example, given the following class annotation:

    package java.net;
    
    import org.checkerframework.checker.mustcall.qual.MustCall;
    
    @MustCall({"close"}) class Socket { }
    
any use of `Socket` defaults to `@MustCall({"close"}) Socket`.

### InheritableMustCall annotation

A declaration annotation `@InheritedMustCall(String[])` is also supported. This annotation can only
be written on a class declaration. It adds an `@MustCall` annotation with the same arguments to
the class declaration on which it is written, and also to all subclasses, freeing the user of the need
to write the `@MustCall` annotation on every subclass.

### Ownership transfer

The MustCall Checker respects the same rules and annotations as the Object Construction with respect to ownership 
transfer. See its documentation for an explanation of these rules.

### Implementation details and caveats

This type system should not be used to track a must-call obligation for a `String`, because it treats all
String concatenations as having the type `@MustCall({})`. 
Normally it is the case that a newly-constructed String (whether created via new String or a string literal 
or concatenation) has no must-call obligation, just like any other object whose type declaration isn't annotated with a 
non-empty `@MustCall` type. `java.lang.String` is an exception: even if the declaration of `java.lang.String` were 
annotated with a non-empty `@MustCall` annotation, the result of a string concatenation would still be `@MustCall({})`. 

This special-casing is to avoid cases where
an object with a must-call obligation is implicitly converted to a `String`, creating a must-call obligation
that could never be fulfilled (this is not unsound, but it does reduce precision). For example, suppose that
all `Socket` objects are `@MustCall({"close"})`. Without special-casing string concatenations, the type of
`str` in the following code would be `@MustCall({"close"})`, even though `str`'s dynamic type is `String`, not
`Socket`:

    Socket sock = ...;
    String str = "this is the result of my socket's toString() function, which is invoked implicitly: " + sock;

#### Type parameters with implicit vs explicit bounds sometimes cause (fixable) false positives

The defaulting rules for `@MustCall` sometimes produce unexpected errors in code
that uses type parameters with implicit upper bounds (i.e. without an `extends` clause).
The errors can be fixed by adding an explicit bound. For example, consider the following
example of a class with a type parameter and a field of an interface type that uses that
type parameter (from [plume-lib/plume-util](https://github.com/plume-lib/plume-util)):

```java
    class MultiRandSelector<T> {
        private Partition<T, T> eq;
    }

    interface Partition<ELEMENT extends @Nullable Object, CLASS extends @Nullable Object> {}
```

Running the Must Call Checker on this code produces two unexpected errors, at each use
of `T` in `MultiRandSelector`:

```
must-call-checker/tests/mustcall/PlumeUtilRequiredAnnotations.java:19: error: [type.argument.type.incompatible] incompatible type argument for type parameter ELEMENT of Partitioner.
        private Partitioner<T, T> eq;
                            ^
  found   : T extends @MustCallUnknown Object
  required: @MustCall Object
must-call-checker/tests/mustcall/PlumeUtilRequiredAnnotations.java:19: error: [type.argument.type.incompatible] incompatible type argument for type parameter CLASS of Partitioner.
        private Partitioner<T, T> eq;
                               ^
  found   : T extends @MustCallUnknown Object
  required: @MustCall Object
```

The important mismatch here is that `Partitioner` has explicit bounds, but `MultiRandSelector`
does not. You could eliminate this false positive by either:
* adding an explicit bound to `MultiRandSelector`: i.e. changing its declaration to `class MultiRandSelector<T extends Object>`, or
* removing the explicit bound from `Partition`: i.e. changing its declaration to `interface Partition<ELEMENT, CLASS>`.

These two changes are semantically equivalent, but you might prefer one over the other (in this case, for example,
we preferred the former, since changing to the latter would remove the explicit `@Nullable` annotations on
`ELEMENT` and `CLASS`).
