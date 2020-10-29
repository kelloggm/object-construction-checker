### MustCall Checker

The MustCall Checker tracks the methods that an object should call before it is deallocated.
The checker is informational only; the fact that the methods are actually called is enforced
by the `-AcheckMustCall` flag to the Object Construction Checker. You should use the Object
Construction Checker directly rather than using the MustCall Checker alone.

The MustCall Checker produces a conservative overapproximation of the set of methods that might
actually need to be called on an object. The Object Construction Checker then conservatively assumes
that this approximation (the `@MustCall` type) must be fulfilled.

For example, an object whose type is `java.io.OutputStream` might
have an obligation to call the `close()` method, to release an underlying file resource. Or,
such an `OutputStream` might not have such an obligation, if the underlying resource is, for
example, a byte array. Either of these dynamic types can be represented by the static type
`@MustCall({"close"}) OutputStream`, which can be read as "an OutputStream that might need
to call close before it is deallocated". The Object Construction Checker would enforce that the
type of such an object in its hierarchy is a subtype of `@CalledMethods({"close"})` at each
point it is deallocated.

### Explanation of qualifiers and type hierarchy

The MustCall Checker supports two qualifiers: `@MustCall` and `@MustCallAny`. The `@MustCall`
qualifier's arguments are the methods that the annotated type
should call. The type `@MustCall({"a"}) Object obj` means
that before `obj` is deallocated, `obj.a()` should be called.
The type hierarchy is:

                     @MustCallAny
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

The default type in unannotated code is `@MustCall({})`.  Given an expression of
unannotated type, its value is not known to be required to call any methods.
The user must provide a specification for any type whose must-call obligations
they want to enforce.

The programmer should rarely (if ever) write the top annotation (`@MustCallAny`), because
the Object Construction Checker cannot verify it by matching with a corresponding `@CalledMethods`
annotation. `@MustCallAny` theoretically represents a value with an unknowable or infinite set
of must-call methods.

### Example use

If a user annotates a class declaration with a `@MustCall(...)` annotation, any use of the class's
type has that `@MustCall` annotation by default. For example, given the following class annotation:

    package java.net;
    
    import org.checkerframework.checker.mustcall.qual.MustCall;
    
    @MustCall({"close"}) class Socket { }
    
any use of `Socket` defaults to `@MustCall({"close"}) Socket`. For the MustCall Checker to be useful,
it should always be used with at least one such `@MustCall` annotation, either in source code
or in a stub file.

To enforce the property, the user should not run the MustCall Checker directly. Rather, the user should
run the Object Construction Checker using the `-AcheckMustCall` flag, which informs that checker to compare
the obligations in `@MustCall` annotations to the `@CalledMethods` facts that it infers, and report an error
whenever a method in an `@MustCall` annotation is not present in the `@CalledMethods` type.

### NotOwning method parameters

A user might sometimes encounter an `argument.type.incompatible` error from the MustCall checker
when passing a object with a non-empty `@MustCall` type to an unannotated library method. For example,
consider a case where all `Socket` objects are `@MustCall({"close"})` and the following code is typechecked:

```
Socket s = ...;
myLogger.log("this is my socket {}", s);
s.close();
```

In this case, `log` is "borrowing" the reference to `s`; the code here is responsible for fulfilling
`s`'s must-call obligation, not `log`. In fact, `log` has no knowledge that `s` might even have a must-call
obligation: its signature permits any object:

```
void log(String msg, Object... args);
```

For these kind of generic methods, the user should annotate `log` (either in its source code or in a stub file)
with a `org.checkerframework.checker.objectconstruction.qual.NotOwning` annotation:

```
void log(String msg, @NotOwning Object... args);
```

Both the MustCall checker and the Object Construction Checker will respect this `@NotOwning` annotation, and the
spurious error described above will no longer be raised on any such calls to `log`.

### Implementation details and caveats

This type system should not be used to track a must-call obligation for a `String`, because it treats all
String concatenations as having the type `@MustCall({})`. This special-casing is to avoid cases where
an object with a must-call obligation is implicitly converted to a `String`, creating a must-call obligation
that could never be fulfilled (this is not unsound, but it does reduce precision). For example, suppose that
all `Socket` objects are `@MustCall({"close"})`. Without special-casing string concatenations, the type of
`str` in the following code would be `@MustCall({"close"})`, even though `str`'s dynamic type is `String`, not
`Socket`:

    Socket sock = ...;
    String str = "this is the result of my socket's toString() function, which is invoked implicitly: " + sock;
