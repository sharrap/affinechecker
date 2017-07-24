# Affine Pointer Analysis for Checker

This is the beginnings of a Rust-like borrow checker as a pluggable type system for Java, done as a final project for a type systems course. It is implemented using the [Checker Framework](https://github.com/typetools/checker-framework).

This was largely intended as a proof-of-concept and to see how hard doing this would be in Checker, and it suffers from some very notable issues at present:

* Everything must be annotated or else it is marked @Unusable by default. This was a hack to fix an issue with Checker. This would be annoying to use in practice, but is fine for our purposes since we are only testing.
* Fields are currently unsupported.
* Only local variables can be borrowed, and null can be "borrowed" to deinitialize a reference. That is to say that arbitrary memory cannot be borrowed unlike in Rust. This would be difficult and would require more complicated lifetime analysis than is practical (barring a large amount of work) in Checker right now.
* Variables must be unborrowed explicitly. Unborrowing implicitly would require a liveness analysis which Checker is ill-equipped to perform and would be an unreasonable amount of work for a proof-of-concept.
