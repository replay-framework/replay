# Welcome to RePlay framework

[![travis-ci](https://travis-ci.org/codeborne/replay.svg?branch=master)](https://travis-ci.org/codeborne/replay) [![gitter chat](https://badges.gitter.im/codeborne/replay.svg)](https://gitter.im/codeborne/replay)


## Introduction

RePlay is a fork of the [Play1](https://github.com/playframework/play1) framework, made and maintained by [Codeborne](https://codeborne.com).
The fork was needed to make some breaking changes that would otherwise not be possible.
RePlay originally forked Play 1.5.0 but regularly pulls in improvements made to Play1 when applicable to RePlay.

Advantages of RePlay over Play1:

* Uses the [Gradle build tool](https://gradle.org/) for dependency management and builds (no verdor code in your project's version control).
* Removes all built-in Play modules (console, docviewer, grizzly, secure, testrunner).
* Improved compile times.
* New, faster [Groovy templating engine](https://github.com/mbknor/gt-engine) (makes use of Groovy 3).
* Allows using [Kotlin out of the box](/codeborne/replay/tree/master/replay-tests/helloworld-kotlin).
* Less magic (like enhancers and creative use of exceptions).
* Follows OO best practices (removes all static fields/methods from Play itself).
* More actively maintained.
* Promotes [dependency injection](/codeborne/replay/tree/master/replay-tests/dependency-injection) for decoupling concerns (using Google's [Guice](https://github.com/google/guice) as a DI provider).


## Getting started

**TODO:** Instructions on how to get a RePlay project started from scratch. How to get some "auto-compile on save" developer workflow going. Maybe a reference to an example project is enough (the projects in `replay-tests` do not count as they do not contain a developer workflow and dont work stand-alone).


## Deploying

**TODO:** How to run a RePlay app in production mode?


## Porting a Play1 codebase over to RePlay

Move your dependencies over from `dependencies.yml` to `build.gradle`.

Make controllers methods (actions) non-static.

Since `play.result.Result` no longer extends `Exception`, your controllers should return `Result` instead of throwing it.


## Documentation

For a large part the [documentation of Play1](https://www.playframework.com/documentation/1.5.x/home) may be used as a reference. 

API docs for the RePlay `framework` package are generated with `./gradlew :framework:javadoc` after which they are found in `/framework/build/docs/javadoc/` 


## Licence

Play framework is distributed under [Apache 2 licence](http://www.apache.org/licenses/LICENSE-2.0.html).

