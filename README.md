# hstack
Short for heterogeneous stack.

## What is a heterogeneous collection?
It is a cross between an typed Object with a fixed set of typed fields and
`java.util.Collection` with a variable number of a single type. A heterogeneous
typed collection is like a stack where each element can have its own type or an
Object with a variable number of typed fields. Either way I've been fascinated
with the construct so I've put this library together.

## Why would I want to use it?
Honestly I'm not sure if there is a good use case for it. For most use cases
that I can think of it would just add memory overhead with to something that
could be implemented normally if you wanted to maintain the types. For
situations where you could get away with loosing the static typing you
might as well use generic object stack.

