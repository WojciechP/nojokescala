---
layout: post
title: "Before you build a web app - pre-development REST checklist"
date: "2015-10-27 15:34"
snippet: >
  Try not to fail before you write a single line of code
meta:
  description: "A few things worth considering before building a web app"
  keywords: "REST, API, learn, read, documentation"
permalink: /post/before-you-build-a-web-app
---

The abundance of all the tools and resources makes building web applications
seemingly effortless. The framework will handle the routing, the library
will process the requests and the template engine will take care of
any UI you need. This is all very convenient, but it doesn't deal
with the greatest risk of all: *having your web service destroyed
before you start coding*.

# Embrace your protocol
You are most likely building a REST service.
Even if you don't want your API to be public, it's a good idea
to have REST API and build a web app independently. Therefore spend
some time learning how the REST works.

## Write a request yourself
You need to understand the HTTP. Go `telnet example.com 80` and
write a `GET` request to fetch the main page. Keep playing around until
you receive a `200 OK` response.

## HTTP Methods and Status Codes
There are three concepts that are absolutely fundamental: *resources*, *methods* and
*status codes*. Make sure everybody has a good understanding of those.
There is, for example, a nice cheat sheet with the status codes available
at [restapitutorial.com](http://www.restapitutorial.com/httpstatuscodes.html) -
pay special attention to the starred status codes as you probably will use
them often.

# Set up API documentation
You need a place to document your API. For small projects it might be
perfectly sufficient to write everything in Markdown and put in a repository.
For bigger projects you might want to employ some specific tools like
[API blueprint](http://apiblueprint.org) or [swagger](http://swagger.io). The choice of the
tool is secondary as long as the following conditions are met:

1. All the API documentation for a single project is in one place
2. All the developers have access to that place
3. The API documentation is complete and correct
4. There is clear indication of what parts are already implemented and
    what is only a draft.

The first two points are quite self-explainatory. The third point,
about docs being complete and correct is tricky: depending on your
desired project quality it might mean different things. An example
of a *complete API doc* definition might be as follows:

> All the endpoints have documented url params, payloads and response bodies.

A nice endpoint description could then look like this:

```
# Search for posts [GET /posts]

Searches for posts matching criteria.

Params
* tag: string, optional
* author: string, optional

At least one of `string` and `author` must be specified.
A `200 OK` response is returned with array of posts as response body:

    HTTP/1.1 200 OK
    Content-Type: application/json

    [
      {
        "author": "Jimmy",
        "tags": ["a", "b"]
        "content": "..."
      }
    ]  
```


This works and may be enough for your projects. But someone might
find it too sloppy and suggest an appendix:

> Moreover, all endpoints have all their possible response codes documented.

This requires extending your API docs with statements like the following:

```
Possible response codes:
* 200 OK - on successful response, when some posts are found.
* 204 No Content - when no post matches query.
* 400 Bad Request - when no search criteria is specified or `author` parameter
    is not a valid username.
* 424 Payment Required - when the requesting user's subscription has expired.
```

Which of the approaches is better? I don't know, it depends on your project
and the team. Just make sure the policy is shared among all the developers.

# Use the tools, don't fight them
You really don't have to reinvent the wheel - there are lots
of useful tools out there. Just make yourself a favor and use them reasonably.

## Using the right tools
Before you start using a new tool, answer there questions:

1. What problem does it solve?
2. Can't we use a tool that is already a part of our toolchain to solve the problem?
3. How much complexity does the tool add?

Make a cost-balance analysis. Is it worth to include another npm package just
in order to use a single function from it? It might be (e.g. if `when.js` is
the package in question), but it might also be an overkill (e.g. if
we're talking about `lodash`).

## Using the tools right
This part is actually simple: just **read the docs.**
You need a custom solution for your test runner to behave differently at
CI servers than on your local machine? Someone has probably already done that.
This point applies to all sorts of tools, including your run-time dependencies,
such as datastores. In fact you should have read the docs before you
decided on incorporating the tool into your project, because you
wouldn't be able to do the cost analysis without it.

# Conclusion
Before your team starts to code, you should have a good understanding of
what's going on. If you start coding without knowing how your protocol works,
*the project will fail*. If everyone pulls in new libraries without
reading their docs (and e.g. discovering they are deprecated) *the project
will fail*. If the team has no overall overview of the expected API,
*the project will...* You get the idea. Be kind and
share the knowledge, it will make your life easier.
