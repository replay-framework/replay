# Welcome to RePlay framework

[![travis-ci](https://travis-ci.org/codeborne/replay.svg?branch=master)](https://travis-ci.org/codeborne/replay) [![gitter chat](https://badges.gitter.im/codeborne/replay.svg)](https://gitter.im/codeborne/replay)


## Introduction

RePlay is a fork of the [Play1](https://github.com/playframework/play1) framework, made and maintained by [Codeborne](https://codeborne.com).
The fork was needed to make some breaking changes that would otherwise not be possible.

RePlay originally forked Play 1.5.0, we regularly pull in applicable improvements made in the Play1 project.


#### Advantages of RePlay over Play1

* Uses the [Gradle build tool](https://gradle.org/) for dependency management and builds (no vendor code in your project's version control).
* Removes most of built-in Play modules (console, docviewer, grizzly, secure, testrunner) that we actually didn't use.
* Improved compile times.
* New, faster [Groovy templating engine](https://github.com/mbknor/gt-engine) (makes use of Groovy 3).
* Allows using [Kotlin out of the box](/codeborne/replay/tree/master/replay-tests/helloworld-kotlin).
* Less magic (like enhancers and creative use of exceptions).
* Follows OO best practices (removes most of static fields/methods from Play itself).
* More actively maintained.
* Promotes [dependency injection](/codeborne/replay/tree/master/replay-tests/dependency-injection) for decoupling concerns (using Google's [Guice](https://github.com/google/guice) as a DI provider).


## Getting started

Unlike with Play1, RePlay does not make use of the `play` command line tool (written in Python 2.7).

This also means there is no `play new` scaffolding available. It is advised to simply start with RePlay's [up-to-date demo application](https://github.com/asolntsev/criminals), and work from there.
The projects in this project's `replay-tests/` folder may also give some ideas on how to set up certain features (like the use of Kotlin).


## Development flow (auto-compilation and hot-swapping)

Since RePlay does not use the `play` CLI command, we cannot use that method to start the application in development mode (i.e.: with `play run`).

To get your changes automatically compiled and hot-swapped into a running application you need to set up your IDE to take care of it.

These steps describe how to do that with IntelliJ IDEA and the [criminals example application](https://github.com/asolntsev/criminals):

0. Clone the [criminals](https://github.com/asolntsev/criminals) repository 
1. Use IntelliJ IDEA to `File > Open...` the criminals project (not "import") by selecting the root of this project's repository
2. In `File > Settings... > Build, Excecution, Deployment > Compiler > Java Compiler` fille the `Additional command line parameters` with the value: `-parameters` (also found in the `build.gradle` file, it put the method argument names in the bytecode by which the framework auto-binds params at run time)
3. Go to `Run -> Edit Configurations...`, click the `+` (Add New Configuration) in the top-right corner and select "Application"
4. Fill in the following details, and click `OK`:
  * Name: *put the name of your application here*
  * Main class: `appname.Application` (replace `appname` with the package name that matches your application name in `app/`)
  * Use classpath of module: `appname.main`
  * JRE: select one you prefer (8+ should work), e.g. `11`

Now a run configuration with the name of your app shows up in the top-right of the screen.
You can press the "Run" button (with the green play icon) to start the application from the IDE.

Code changes are automatically compiled and hot-swapped into the running application. In many cases it works immediately (until you change signatures of some methods).  
You do not even need to save the file you are working on: simply reload the page in your browser to see the changes in effect.

If hot-swapping failed for some reason, you will see a notification in IDEA after which you simply need to restart the application.  


## Deploying

As a result of `./gradlew jar` you will have the JAR file `build/libs/appname.jar`.

The following should start your application from the command line:

    java -cp "build/lib/*:build/libs/*:build/classes/java/main" appname.Application

The classpath string (after `-cp`) contains three parts. The first bit (`build/lib/*`) points to the dependencies of the project as installed by Gradle.
The second bit (`build/libs/*`) points the application JAR file as build Gradle.
The last bit points to the folder with the application's `.class` files (`build/classes/java/main`) built by the Gradle build script, as that's what RePlay (and Play1 as well) use instead of the versions of these files found in the application's JAR file.


## Troubleshooting

You may find some `WARNING` blocks in the logs when running the application. These are safe to ignore, below some further explanation on what causes then and how to possibly fix them.

Add this flag `--add-opens java.base/java.lang=ALL-UNNAMED` to reduce "illegal reflective access" warnings from `guice` (this will be fixed in a future version of `guice`).

The "illegal reflective access" warnings from `netty` are harder to fix: RePlay should upgrade it's `netty` dependency from `3.10.6.Final` to `io.netty:netty-all:4.1.43.Final` or greater.


## Porting a Play1 codebase over to RePlay

It is not automatic, yet we believe it's worth it...

* Move your dependencies over from `dependencies.yml` (Ivy2 format) to `build.gradle` (Gradle format).
* Make controllers methods (actions) non-static, and subclass from `RebelController`, while doing so you need to mind the following:
  * TODO: explain how to deal with redirects, returning `Result` (or `null`), etc.
* Since `play.result.Result` no longer extends `Exception`, your controllers should return `Result` instead of throwing it.
* `play.libs.Crypto` has been slimmed down and is now called `Crypter`, for some methods you need to roll your own.
* `Check` and `CheckWith` have been removed from the `play.data.validation` package, you may copy them locally but using validation methods explicitly (opposed to with annotations) allows more control over error responses.
* The `params.get()` method now always returns a `String`, use `parseXYZ()` methods (like: `Boolean.parseBoolean()`) to convert results.
* `play.libs.WS` has been split up into, the `play.libs.ws` package contains the classes that have been split out.
* `Router.absolute()` now takes a param, simple pass it: `Http.Request.current()`
* `IO.readFileAsString()` now needs an additional `Charset` argument (usually `StandardCharsets.UTF_8` suffices).
* The `play.mvc.Mailer` class was dropped. Use the `play.libs.Mail` class instead.
* Removed Play1's JPA enhancer methods (from `play.db.jpa.JPAEnhancer.java`; like `count()`, `find()`, `findById()`, `delete()`) for performance improvements. Use the `play.db.jpa.JPARepository` class instead (a better design choice: more standard, less magic, no statics and easier mocking).

    JPARepository<User> usersRepository = JPARepository.from(User.class);
    User user = userRepository.findById(id);

* `refresh()` has been removed from `play.db.jpa.GenericModel.java`. Replace with: `JPA.em().refresh(entityYouWantToRefresh)`

* `play.Logger` has been slimmed down, for logging we now use the standard Java idiom:

    import org.slf4j.Logger;  // add these
    import org.slf4j.LoggerFactory;

    public class YourClassThatNeedsLogging {
      // the following line allows quick access to the logger within this class' context
      private static final Logger logger = LoggerFactory.getLogger(YourClassThatNeedsLogging.class);

      YourClassThatNeedsLogging(int i) {
        logger.debug("Constructor invoked with param: {}", i);  // example logging statement
      }
      // ...
    }


## Documentation

For a large part the [documentation of Play1](https://www.playframework.com/documentation/1.5.x/home) may be used as a reference. 

API docs for the RePlay `framework` package are generated with `./gradlew :framework:javadoc` after which they are found in `/framework/build/docs/javadoc/` 


## Licence

RePlay framework is distributed under [MIT license](https://github.com/codeborne/replay/blob/master/LICENSE).

The original Play1 framework is distributed under [Apache 2 licence](http://www.apache.org/licenses/LICENSE-2.0.html).

