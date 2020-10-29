### MustCall Checker

The MustCall Checker tracks the obligations that an object might have to call particular methods
before it is deallocated. For example, an object whose type is `java.net.Socket` might
have an obligation to call the `close()` method, to release the socket. Or, a builder object
might have an obligation to call the `build()` method. We say that an object `obj` has a
*must-call obligation* to call some method `foo` if there exists an execution path on which
it is an error for `obj.foo()` not to be invoked before `obj` is deallocated. As a shorthand,
the documentation for this checker sometimes shortens this to "`obj` must call `foo`" or similar,
to refer to the worst case in which `obj` is actually an object that has to call `foo` (even if
`obj` sometimes dynamically does not actually need to call `foo`, such as when `obj` is `null`).

The MustCall Checker tracks which methods might need to be called, but does
**not** check that these methods have actually been called.  The Object
Construction Checker's `-AcheckMustCall` command-line option checks the
obligation expressed by an `@MustCall` annotation. This option uses a dataflow
algorithm to compare `@MustCall` obligations to inferred `@CalledMethods` facts,
and reports cases where required methods might not have been called.


### Explanation of qualifiers and type hierarchy

The MustCall Checker supports two qualifiers: `@MustCall` and `@MustCallTop`. The `@MustCall`
qualifier's arguments are the methods that the annotated type
might be responsible for calling. The type `@MustCall({"a"}) Object obj` means
that at run time `obj` cannot have an obligation to call any method other than `a()` (but it may or 
may not actually have an obligation to call `a()`).
Likewise, `@MustCall({"m1", "m2"}) Object` represents all objects that need to
call `m1`, `m2`, both, or neither. It should not represent an object that needs
to call some *other* method.  The type hierarchy is:

                     @MustCallTop
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

The default type in unannotated code is `@MustCall({})`.  Given an expression of
unannotated type, its value definitely has no obligations to call any methods.

This default is unsound in the sense that a priori the checker
has no knowledge of what methods an object might need to call. A sound default would be
`@MustCallTop`, which represents all objects, regardless of what methods they might have an obligation to call.
Such a default, however, would not be **useful**, because it is desirable to track objects that have obligations
to call specific methods, not all objects generally.
The checker therefore uses the unsound default `@MustCall({})` for unannotated types,
but permits a user to annotate a type with another `@MustCall` type to express that a particular type has
a particular obligation. Such an annotation is then tracked soundly. 


### Example use

If a user annotates a class declaration with a `@MustCall(...)` annotation, any use of the class's
type has that `@MustCall` annotation by default. For example, given the following class annotation:

    package java.net;
    
    import org.checkerframework.checker.mustcall.qual.MustCall;
    
    @MustCall({"close"}) class Socket { }
    
any use of `Socket` defaults to `@MustCall({"close"}) Socket`.

TODO: Move the following implementation comment out of the user documentation.
TODO: Make the `@MustCall` annotation inherited.


### Implementation details and caveats

This type system cannot be used to track a must-call obligation for a `String`, because it treats all
String concatenations as having the type `@MustCall({})`. This special-casing is to avoid cases where
an object with a must-call obligation is implicitly converted to a `String`, creating a must-call obligation
that could never be fulfilled (this is not unsound, but it does reduce precision). For example, suppose that
all `Socket` objects are `@MustCall({"close"})`. Without special-casing string concatenations, the type of
`str` in the following code would be `@MustCall({"close"})`, even though `str`'s dynamic type is `String`, not
`Socket`:

    Socket sock = ...;
    String str = "this is the result of my socket's toString() function, which is invoked implicitly: " + sock;
