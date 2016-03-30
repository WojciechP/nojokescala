---
layout: post
title: "Contract First REST API design"
date: "2015-12-01 16:00"
redirect_to: "http://wojciechp.com/2015/contract-first/"
snippet: >
  Shipping useful software comes down to two major difficulties:
  communication and software engineering. Describing your REST API
  before writing any code will make both these things easier.

meta:
  description: "Benefits of using contract-first approach for REST API design"
  keywords: "API, REST, design, contract first, software development, web services"
permalink: /post/contract-first
---

We're mostly building web applications these days. Even standalone apps have
their UI layer written as a single page app in JS and communicate with
their backend using REST. Let's explore the process of designing the
REST API for such an app.

First, let's see how a single endpoint description could look like:

```
Update a Frob  [ PATCH /frob/:id ]
=============

Sets an array of bars for a given Frob. Can be used only on frobs
that are not fooed.

Query params:
  * id - the unique ID of the Frob to modify.

Payload: array of Bar ids.

The server returns a `200 OK` response with the modified Frob json entity.

Example Request
---------------
  PATCH /frob/aa42 HTTP/1.1
  Content-Type: application/json

  ["abc123", "def456"]


Example Response
----------------
  200 OK

  {
    "id": "aa42",
    "meh": 4,
    "fooed": false,
    "bars": ["abc123", "def456"]
  }

Error Responses
---------------
* 409 Conflict - if the requested Frob is already fooed
* 403 Forbidden - if the requested Frob is a Core Frob and noone can modify it
```

As the application grows you will sooner or later need some similar kind of
API description. In fact, you should write it *before* writing the server-side
implementation, as it gives massive benefits - both in terms of software
quality and team communication.

# Three approaches to writing API docs

I strongly advise the *contract-first* approach, but for completeness
I will include three approaches to writing such docs.

## 1. Waterfall
In this approach the team writes the whole API documentation first.
Then the contract gets frozen and the implementation begins.
In my rather short business experience in IT I have met nobody
who managed to complete a web project this way.

## 2. First do it, then doc it
Here the developers focus on the thing that makes them tick - writing software - and
leave the docs for later. This can work if the team has very high
understanding of the project.

## 3. Contract-first for every feature
This method utilizes agile approach for web apps development. The workflow is
as follows:

1. A developer picks a single, isolated feature to develop
2. The developer writes the API description
3. The API description is a subject to review by other devs (and possibly the client)
4. When the API description is agreed to be done, the dev implements the feature.


# Benefits of using contract-first approach
Using contract-first method will boost your efficiency in developing
web apps. Here are the pros I observed during my works:

## Higher Software quality

Designing API prior to implementing software makes the API more RESTful and robust.
It is easy to change the API when API draft is all you have. Things get
much more difficult when the implementation is already in place and changing
the contract would mean trashing the dev's work. So it's more efficient
to work on API shape without the implementation behind your back.

Moreover, contract-first development shares some benefits with *test-first*
development: as a dev you first put yourself in the position of user,
and design the API to be convenient for the caller. This has proven
to increase the code quality and is generally encouraged.

Later on, when the API draft is accepted, it may be used a skeleton
for the implementation design. Therefore you can smoothly transition to
the *sketch-and-fill* approach, tackling the challenges in an
orderly manner - from the most generic ones down to small implementation details.
It is then useful to have the possible responses documented, as every one
of them is a manifestation of a single usage scenario. More precisely,
the `2xx` response in an endpoint represents the correct, default flow of control,
and every error (`4xx`) response represents an alternate flow. Having them
all in one place makes it easier to structure the server underneath.

## Better Team communication

The contract-first approach also enhances your communication with the team.
First of all, an API design draft serves as a nice description of the desired
functionality. Bear in mind that programming in general is a process of transforming
short, but abstract and unclear system specs (in a natural language)
to very verbose, but clear and exact system specs (in a programming language)
and having API design as another intermediate step helps a lot.

Publish your API design to your team and ask for a review. Spotting
badly designed architecture or business logic is easier this way than
contemplating unclear user stories in your issue tracker. If your
team approves your API, you will know that everyone has a similar
understanding of how the system should work.

Writing API design first makes it also easier to estimate the work that
needs to be done. When the API design is in place one can count the number
of required endpoints, url params, or anything - it's still better than
estimating the story based just on a natural description. Moreover,
the API designs are usually easy to review and tag as "doable" or "not doable",
so in case you write an API doc which is impossible to implement, someone
should spot it soon.

If the front-end and back-end tasks are separated in your team,
preparing the API doc first will also make it possible to parallelize the works.
Having the contract written decouples server-side development from the web app.
