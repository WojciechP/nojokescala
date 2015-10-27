---
layout: post
title: "Futures and Either - asynchronous error handling"
date: "2015-09-11 16:38"
snippet: >
  Design your asynchronous Scala API so that clients can choose
  the level of error handling appropriate to their needs.

meta:
  description: "Scala asynchronous error handling with Future and Either"
  keywords: "Scala, Future, Either, error handling, concurrent, asynchronous"
permalink: /post/future-either
---

This article is mostly about designing your Scala API so that it allows
to control the level of error handling. The source code for the included
snippets is available at the [examples][njs-examples] branch of
No Joke Scala [GitHub repo][njs-repo].

The introductory part shortly describes the following aspects of programming
in Scala:

 * failure handling: exceptions, `Try`, `Either`, `Option`
 * asynchronous programming with `Futures`

If you are familiar with those, feel free to jump directly to the main
part that covers the dilemma of when to use which of those constructs.

  [njs-examples]: https://github.com/WojciechP/nojokescala/tree/examples "No Joke Scala examples"
  [njs-repo]: https://github.com/WojciechP/nojokescala/ "No Joke Scala on GitHub"

# Necessary recap

## Error handling

Error handling in scala can be done in one of the following ways:

* as in Java, with exceptions
* using `Try[A]` in place of `A` to indicate the method might fail with a `Throwable`
* returning an `Option[A]` when one would like to return `A`, but might fail to provide it
* using `Either[A, B]` for methods that might fail with reason `A` or succeed with result `B`

All those are nicely introduced in [error handling in Scala][terse errors]
blog post. Nice thing about `Try`, `Option` and `Either` is their ability
to fit into standard collection-style invocations - they support
`.map`, `.flatMap`, can be used in for-comprehensions etc.

There is a single most important piece of advise regarding throwing exceptions in Scala:

> Don't throw exceptions for indicating business errors.

This is a no-exception rule (pun intended). When somebody describes
a system behavior, they might say something like that: "anybody can paint
a *Thing* to one of three colors, but if the *Thing* is *Blue*
and someone tries to paint it *Red*, the system rejects him with an error".
This is the business logic, therefore no `Exception` should be raised.
It is actually *expected* system behavior for the user to be rejected.
On the contrary, when the painting backend fails because a rogue admin
pulled the hard drive out - then yout have a reason to throw an exception.

  [terse errors]: https://tersesystems.com/2012/12/27/error-handling-in-scala/ "Error handling in Scala"

## Futures

Futures are an abstraction to represent calculations that are expected to return
some values. For example, if you have an object that fetches blog posts
from your favorite blogger, it could have a method `fetchRecentPosts(newerThan: DateTime): Future[List[Post]]`.
When you have a `Future` bound to a variable, it behaves a little bit like an `Option`:
you can use some monadic transformations on it to produce another `Future`.
Check out the [Futures and Promises official guide][futures] for description.

Note that `Futures` come with a built-in way of error-handling, as they can
either succeed with a value or fail with an error. This will be crucial a little later.

  [futures]: http://docs.scala-lang.org/overviews/core/futures.html "Scala Futures and Promises"

# Errors in the Future

The main dilemma I want to address here comes as follows:

> I'm building a service that takes a record and stores it in a database,
> returning the timestamp of the writing.
> It should obviously be asynchronous. Moreover it can fail, for example
> because the database explodes, the record to save is malformed, or
> a record with given id or title already exists (I want them both to be unique).
> How should I design the service API?

For simplicity's sake let's fix the `Record` as follows:

```scala
final case class Record(
    id: UniqueID,
    name: String,
    data: WeirdDataType)
```
Let's also say that for a record to be well-formed it needs
a name that is two to twenty characters long, and the other two fields
can have any non-null values. What do we do next?

At some point we will surely have a `Future`, because it has to be async.
Let's consider the most straightforward approach first, then:

```scala
trait StorageService {
  def store(record: Record): Future[DateTime]
}
```

They said "keep it simple, stupid," right? Well, this particular design will
take us nowhere. As the *no-exception* rule states, using exceptions to handle
business errors is prohibited. The only way for `StorageService` terminate
without returning a timestamp is via an exception (either directly, by throwing,
  or indirectly by returning a failed service), so we're out of options.
We'll come up with a solution by examining what kinds of errors can we
actually get.


## Business errors

What are our possible business errors, anyway? From the description,
there are three types of them:

1. Malformed record
2. Duplicate ID
3. Duplicate title

We don't want to use exception for them - we'll rather
use `Option` or `Either` (not a `Try`, because it's an exception in disguise).
There are two kinds of significantly different errors because we can detect malformed records
right away, and a duplicate ID or title will not be detected until the very end when
the database tries to store the record. In other words, we can handle (1) synchronously,
on invocation, and (2) or (3) only asynchronously later.

I don't like using `Option` for reporting failure, I always fear it might be overlooked.
It's OK to use `Option` for dictionary lookups or "find-first-matching" actions,
but they aren't really errors. The use case corresponds to someone asking
you to solve a contradiction equation - there is no answer, but's not an error.
For real errors I'd rather use `Either` with custom left type - this way
the API users will have to code against the case of failure. Let's
introduce our error types then:

```scala
sealed trait StorageError

final case class MalformedRecord(record: Record)
  extends StorageError

final case class DuplicateId(record: Record)
  extends StorageError

final case class DuplicateTitle(record: Record)
  extends StorageError
```

Note that the `StorageError` class is `sealed` and the classes are `final` - you
really don't want anybody to extend your API errors,
that would cause a mess in recovery pattern matching.
If you ever need add another exception type, make your API
backwards-incompatible - the users have to guard against the new error
anyway.

This should get us covered in terms of business error types - let's
enrich our service declaration to include them:

```scala
trait StorageService {
  def store(record: Record): Future[Either[StorageError, DateTime]]
}
```

This approach makes it possible to handle the business errors. One might
implement a client that tries to store a record with retries if the ID
is duplicate:

```scala
val service: StorageService = ...
val idGenerator = ...

def storeWithRetries(record: Record, retries: Int):
    Future[Either[StorageError, DateTime]] = {
  service.store(record).recoverWith {
    case DuplicateId(_) =>
      if (retries > 0)
        storeWithRetries(record.cloneWithNewId(idGenerator.next()))
      else
        throw new RetryLimitExceededException(
          "All retries used up, I have no idea what to do next")
  }
}
```

Note that the `recoverWith` function accepts a `PartialFunction` as a parameter -
in this example only `DuplicateId` will be handled, two other errors will pass
through and get returned as the method result.
This while snippet should do the job, but it is already clumsy - let's clean it a little.

## Handling malformed data

I hate validation. It's always ugly. That is why we don't want to have it
inside our service. Recall the requirement that "name has to be a string
which has 2-20 characters". If you think about it, you'll realize
that a part of this requirement is expressed as a type restriction
("the title has to be a string"), and part results in `MalformedRecord`
("2-20 characters"). Isn't it inconsistent?

I remember when I came to programming class I heard something similar to
what follows:

> A function of type X => Y accepts parameters of type X and produces Y
> as a result.

This might sound obvious, but it contains a seed of wisdom:
the method should *accept* X's, not *reject* them. This can be rephrased to
something more explicit:

> If your API input parameter has type X, you should guarantee the service
> behaves nicely on all values of type X.

Sometimes it's not possible. But most of the time you know exactly what
kind of data to accept, so you can design your types to be more restrictive:

```scala
final case class Record(
    id: UniqueID,
    name: String,
    data: WeirdDataType) {
  require(name.length >= 2 && name.length <= 20,
    "name has to be 2-20 characters long")
  require(id != null)
  require(data != null)
}
```

*This is NOT validation,* just a dirty patch to the Scala type system - since we don't
know how to check string length statically, we'll do it in the runtime,
but as soon as possible to obey the "fail fast" principle.
Now the service users will have to worry about input validation, but they
would have to validate their input anyway, so no harm done.

To sum up: by slightly modifying the input type we are now certain that
the service itself will receive only well-formed data, which means we don't
have to handle malformed data inside the service. We can remove the
`MalformedRecord` case class and have one layer less.

## Unexpected failures

Apart from the business errors, there are possible external (infrastructure) failures, both
local on invocation and remote in background:
the JVM may always run out of heap space or the DB might get thrown into the Atlantic.
These unexpected errors should be handled with an exception or a failed
`Future` (which is the exceptional way of completing `Futures`):


1. **`StorageService.store(rec)` throws an exception.**
    This is pretty much equivalent to saying "Hey, I wanted to start a computation
    that would be expected to return a `Date`, but I failed in doing so for
    some totally unexpected reason." Do note that throwing instead of returning a future
    doesn't mean the computation failed - it means that it even couldn't be started.
    This is our way of signaling an unexpected error on the caller side.
2. **`StorageService.store(rec)` returns a future which is the failed with an exception.**
    This means that the service accepted the parameter and tried to reach the db,
    but storing failed to some unexpected reason. More often than not it will be
    due to connection issues.

# Wrapping it up

We can finally settle on our service interface as follows:

```scala
trait Service {
  def store(record: Record): Future[Either[StorageError, DateTime]]
}

object Service {
  sealed abstract class StorageError(message: String) extends
    Exception(message)

  final case class DuplicateRecordId(record: Record) extends
    StorageError("Duplicate ID in record " + record)

  final case class DuplicateTitle(record: Record) extends
    StorageError("Duplicate title in record " + record)
}
```
This gives the client a fair choice on how much errors to track. One might,
for example, handle only business errors and let the other ones bubble up:

```scala
val service: Service = ...
def simplyStore(record: Record): Future[String] =
  service.store(record) map (_ fold (
    {
      case DuplicateId(_) => "Ehh, duplicate record id"
      case DuplicateTitle(_) => "This title is already taken"
    },
    time => "Yey, stored on " + time))
```

The above function will take care of business errors, returning
a `Future[String]`, regardless of whether duplicate entries are found.
It does not deal with infrastructure levels, though - if the service future
fails, so will this one.

# The Verdict

This pretty much covers idiomatic asynchronous error handling in pure Scala.
The described approach leaves much control for the clients - they might
only handle business errors by unwrapping `Either`, or check for other
errors (using `catch` and checking for failed future). As a downside,
the code is still quite noisy, and most of the time requires double
unwrapping (from `Future` and from `Either`). This can be simplified
using Scalaz monad transformers, which are a good topic on their own.
The most important pieces of advice are:

* Don't raise exceptions for no good reason
* Use monads where you expect business failures (`Either` or `Option`)
* Design your types so that you know their values present valid data

I hope you've got some new insights on the matter by now. If you have
encountered any other patterns for handling errors please share them in the
comments. Special thanks to Artur Bańkowski for reviewing the early version
of the post.


Tu NowaQ dopisał coś mądrego.

A tutaj Nowaq pisze mądre rzeczy na komórce :P
