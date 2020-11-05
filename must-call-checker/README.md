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

### NotOwning method parameters

Some method calls transfer a `@MustCall` obligation:  the client depends on the method to make the calls.
For other method calls, the client retains the obligation; the callee only "borrows" the object rather than taking final responsibility for it.
An example of a borrowing call is a logging call.

```
@MustCall({"close}) Socket s = ...;
myLogger.log("this is my socket {}", s);
... // log did not call close(); the client must still do so
```

`log` is annotated with no `@MustCall` obligation:

```
void log(String msg, Object... args);
```

The above call leads to an `argument.type.incompatible` error from the MustCall checker, because
the argument type `@MustCall({"close"})` is not a subtype of the formal parameter type `@MustCall({"close"})`.
To mark `log()` as borriwing an argument, use the `org.checkerframework.checker.objectconstruction.qual.NotOwning`
annotation on the formal parameter (it is not a tye annotation)

```
void log(String msg, @NotOwning Object... args);
```

Now, the Object Construction Checker can soundly verify the client code without
warnings, including both permitting the call `log()` and ensuring that `close()` is
eventually called.

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
