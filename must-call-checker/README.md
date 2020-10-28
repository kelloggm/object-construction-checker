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

The MustCall Checker does **not** check that
these methods have actually been called; rather, it is responsible only for tracking which methods
might need to be called.

### Explanation of qualifiers and type hierarchy

The MustCall Checker supports two qualifiers: `@MustCall` and `@MustCallTop`. The `@MustCall` qualifier
takes a `String[]` as its argument; the members of the array are the methods that the annotated type
might be responsible for calling. It is important to note the type `@MustCall({"a"}) Object obj` means
that at run time `obj` cannot have an obligation to call any method other than `a()` (but it may or 
may not actually have an obligation to call `a()`). This definition naturally leads to a type hierarchy:

                     @MustCallTop
                           |
                          ...
                           |
               @MustCall({"foo", "bar"})     ...
                    /             \
       @MustCall({"foo"})     @MustCall({"bar"})  ...
                    \            /
                    @MustCall({})

The default type in unannotated code is `@MustCall({})`, meaning that the checker assumes that there are no
methods that an unannotated type is obligated to call. This default is unsound in the sense that a priori the checker
has no knowledge of what methods an object might need to call. A sound default would be
`@MustCallTop`, which represents all objects, regardless of what methods they might have an obligation to call.
Such a default, however, would not be **useful**, because it is desirable to track objects that have obligations
to call specific methods, not all objects generally.
The checker therefore uses the unsound default `@MustCall({})` for unannotated types,
but permits a user to annotate a type with another `@MustCall` type to express that a particular type has
a particular obligation. Such an annotation is then tracked soundly. 

In other words, the checker assumes
that objects have no obligations unless the user provides a specification that says otherwise.

Another way to think about this is that `@MustCall({"m1", "m2"}) Object` represents all objects that need to 
call `m1`, `m2`, both, or neither. It should not represent an object that needs to call some *other* method. 
So, for example, `@MustCall({"m1", "m2"}) Object` represents more possible values than `@MustCall({"m1"}) Object` 
does, because `@MustCall({"m1"}) Object` can only contain an object that needs to call `m1` or one that needs 
to call nothing - an object that needs to call `m2` (or both `m1` and `m2`) is not permitted. This view 
makes the need for `@MustCallTop` more apparent, because there has to be some type that represents all
possible objects, whatever methods they are obligated to call.

### Example of how this type system is intended to be used

To use the type system, a user usually annotates a class
with an appropriate `@MustCall` annotation, which causes the type of any object of that class to default to
that `@MustCall` type. For example, to enforce that `close()` is always called on a `Socket`, the following stub
could be used:

    package java.net;
    
    import org.checkerframework.checker.mustcall.qual.MustCall;
    
    @MustCall({"close"}) class Socket { }
    
Supplying this stub file to the checker via the `-Astubs=` command-line option will cause the checker to
default any `Socket` in the source code that it processes as `@MustCall({"close"})`. Note that the `@MustCall`
annotation is not inherited (TODO: make this automatic), so all subclasses of `Socket` will also need an 
`@MustCall({"close"})` annotation, or a warning will be issued at their definition.

Actually checking that the obligation expressed by an `@MustCall` annotation is handled by the Object
Construction Checker's `-AcheckMustCall` command-line option. This option uses a dataflow algorithm to
compare `@MustCall` obligations to inferred `@CalledMethods` facts, and reports cases where required
methods might not have been called.

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
