### Unconnected Socket Checker

The Object Construction Checker's must-call mode can enforce that sockets
and other similar structures are always closed.
Warnings about failures to call close on an unconnected socket 
are false positives, because the underlying socket resource 
(i.e. the file descriptor stored in the `fd` field in `SocketImpl`) is 
only created when a connection is established - the socket constructor on 
its own does not allocate a file descriptor.

The Unconnected Socket Checker prevents (some of) these false positives
by tracking which sockets are definitely unconnected. The Object Construction
Checker uses this information to avoid issuing the false positives.

There are two types in this type system, with the following names and subtyping relationship:

`@PossiblyConnected` :> `@Unconnected`

`@PossiblyConnected` is the default type qualifier. 

The value produced by the no-arguments `java.net.Socket` and `java.net.ServerSocket` constructors and the 
return type of `SocketChannel#open` are `@Unconnected`; these are specified in stub files.

Calls to any method that takes an `@Unconnected` object as a parameter (including the receiver parameter)
change the type of that parameter to `@PossiblyConnected` - any code with access to the socket may call
`connect`. Similarly, assigning an `@Unconnected` object into a field will change its type locally to
`@PossiblyConnected`.

It is always illegal to use `@Unconnected` as the type of a field. Doing so will result in an `unconnected.field` error.

A method can be marked as trusted not to call connect on a socket using the declaration annotation `@CannotConnect`.
This annotation is TRUSTED, NOT CHECKED. It is usually only be written in stub files. The file `Sockets.astub` in the
checker's source directory contains a default list of trusted `Socket` methods, all of which are configuration methods
that are often called before connecting the socket.

The unconnected socket checker supports a standard polymorphic annotation: `@PolyConnected`.
