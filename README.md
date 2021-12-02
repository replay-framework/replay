# Welcome to RePlay framework


## Introduction

RePlay is a fork of the [Play1](https://github.com/playframework/play1) framework, made and maintained by [Codeborne](https://codeborne.com).
The fork was needed to make some breaking changes (detailed below) that would otherwise not be possible.

RePlay originally forked Play 1.5.0, applicable improvements made in the Play1 project are regularly copied into RePlay.


#### Main differences between RePlay and Play1

* It uses the [Gradle build tool](https://gradle.org/) for dependency management and builds (no vendor code in the projects version control, no need for Ivy2 and Python scripts to manage dependencies).
* Removes most built-in Play modules (console, docviewer, grizzly, secure, testrunner) that were not used at Codeborne.
* Improved compile times, mostly by avoiding reliance on [JBoss Javassist](https://www.javassist.org) for bytecode manipulating *enhancers*.
* Allows using [Kotlin out of the box](/codeborne/replay/tree/master/replay-tests/helloworld-kotlin).
* Less magic (like *enhancers* and creative use of exceptions for redirecting/responding/returning from controller methods).
* Follows OO best practices (removes most of static fields/methods from Play itself).
* More actively maintained.
* Promotes [dependency injection](/codeborne/replay/tree/master/replay-tests/dependency-injection) for decoupling concerns (using Google's [Guice](https://github.com/google/guice) as a DI provider).


## Getting started

Unlike with Play1, RePlay does not make use of the `play` command line tool (written in Python 2.7).

Therefore, there `play new` scaffolding is not available. It is advised to simply start with RePlay's [up-to-date demo application](https://github.com/asolntsev/criminals), and work from there.
The projects in RePLay's `replay-tests/` folder also show how to do certain things in RePlay (like the use of Kotlin and dependency injection with Guice).


## Documentation

For a large part the [documentation of Play1](https://www.playframework.com/documentation/1.5.x/home) may be used as a reference. It is important te keep the main differences between Play1 and RePlay in mind, to know what parts of Play1's documentation to ignore.

API docs for the RePlay `framework` package are generated with `./gradlew :framework:javadoc` after which they are found in the `/framework/build/docs/javadoc/` folder.


## Development flow (auto-compilation and hot-swapping)

Since RePlay does not use the `play` CLI command, we cannot use that method to start the application in development mode (i.e.: with `play run`).

To get your changes automatically compiled and hot-swapped into a running application you need to set up your IDE to take care of it.

These steps describe how to achieve just that with IntelliJ IDEA and the [criminals](https://github.com/asolntsev/criminals) RePlay example application:

0. Clone the [criminals](https://github.com/asolntsev/criminals) repository 
1. Use IntelliJ IDEA to `File > Open...` the criminals project (not "import") by selecting the root of this project's repository
2. In `File > Settings... > Build, Excecution, Deployment > Compiler > Java Compiler` fille the `Additional command line parameters` with the value: `-parameters` (also found in the `build.gradle` file, it put the method argument names in the bytecode by which the framework auto-binds params at run time)
3. Go to `Run -> Edit Configurations...`, click the `+` (Add New Configuration) in the top-right corner and select "Application"
4. Fill in the following details, and click `OK`:
  * Name: *put the name of your application here*
  * Main class: `appname.Application` (replace `appname` with the package name that matches your application name in `app/`)
  * Use classpath of module: `appname.main`
  * JRE: select one you prefer (Java 11 is a minimum requirement), e.g. `11`

Now a run configuration with the name of your app shows up in the top-right of the screen.
You can press the "Run" button (with the green play icon) to start the application from the IDE.

Code changes are automatically compiled and hot-swapped into the running application. In many cases it works immediately (until you change signatures of some methods).  
You do not even need to save the file you are working on: simply reload the page in your browser to see the changes in effect.

If hot-swapping failed for some reason, you will see a notification in IDEA after which you simply need to restart the application.  


## Deploying

The `./gradlew jar` command produces the `build/libs/appname.jar` file.

The following should start your application from the command line:

    java -cp "build/lib/*:build/libs/*:build/classes/java/main" appname.Application

The classpath string (after `-cp`) contains three parts:

1. The first bit (`build/lib/*`) points to the dependencies of the project as installed by Gradle.
2. The second bit (`build/libs/*`) points the application JAR file as build Gradle.
3. The last bit points to the folder with the application's `.class` files (`build/classes/java/main`) built by the Gradle build script, as that's what RePlay (and Play1 as well) use instead of the copies of these files as found in the application's JAR file.


## Troubleshooting

You may find some `WARNING` blocks in the logs when running the application. These are safe to ignore, below some further explanation on what causes then and how to possibly fix them.

Add this flag `--add-opens java.base/java.lang=ALL-UNNAMED` to reduce "illegal reflective access" warnings from `guice` (this will be fixed in a future version of `guice`).

The "illegal reflective access" warnings from `netty` are harder to fix: RePlay should upgrade it's `netty` dependency from `3.10.6.Final` to `io.netty:netty-all:4.1.43.Final` or greater.


## Porting a Play1 application over to RePlay

Porting a Play1 application to RePlay requires quite a bit of work, depending on the size of the application.
This amount of work will be significantly less than porting the application to [Play2](https://www.playframework.com) or the currently very popular [Spring Boot](https://spring.io/projects/spring-boot).

* Move your dependencies over from `conf/dependencies.yml` (Ivy2 format) to `build.gradle` (Gradle format).
* Make controllers methods (actions) non-static, and subclass from `RebelController`, while doing so you need to mind the following:
  * TODO: explain how to deal with redirects, returning `Result` (or `null`), etc.
  * Since `play.result.Result` no longer extends `Exception`, your controllers should return `Result` instead of throwing it.
* `play.libs.Crypto` has been slimmed down and is now called `Crypter`.
  * **TIP**: You can simply copy the `play.libs.Crypto` file from Play1 into your project.
* `Check` and `CheckWith` have been removed from the `play.data.validation` package.
  * **TIP**: Copy those files from Play1 into your project, but using validation methods explicitly from the body of controller methods (opposed to configuring validations through annotations) allows more control over error responses.
* The `params.get()` method now always returns a `String`, use `parseXYZ()` methods (like: `Boolean.parseBoolean()`) to convert results.
* `play.libs.WS` has been split up into the `play.libs.ws` package containing the classes that have been split out.
* `Router.absolute()` now takes a param, simple pass it: `Http.Request.current()`.
* `IO.readFileAsString()` now needs an additional `Charset` argument (usually `StandardCharsets.UTF_8` suffices).
* The `play.mvc.Mailer` class was dropped, use the `play.libs.Mail` class instead.
  * **TIP**: Copy RePlay's `play.libs.Mail` class into your Play1 project and port over the mail logic while your application is still running on top of Play1.
  * Here an example of a simple text mail:

```java
public class TextMails extends Mail {

  public static void welcomeNewUSer(final User user, final Organization organization) {
    TextEmail email = new TextEmail(); // subclass of a `org.apache.commons.mail.*` class
    email.setFrom("info@example.org");
    email.addRecipient(user.getEmailAddress());
    email.setSubject(
      String.format("Welcome %s! Follow the link in this mail to active your account.",
        organization.name));
    email.setMsg(
      TemplateLoader.load("app/mails/text/welcomeNonTrialInternal.txt"))
        .render(Map.of("user", user, "org", organization));
    send(email);
  }
}
```

* Play1's JPAEnhancer (`play.db.jpa.JPAEnhancer`) was removed. This means the classes that extend `Model` have to implement `create`, `count`, `find*`, `all` and `delete*` methods themselves.
  * JPAEnhancer is removed to: speed up builds, allow non-Java JVM languages to use the classes, reduce magic, make mocking easier and use a more standard idiom.
  * **TIP**: By adding the following lines to `conf.application.conf` of a Play1 project, the work required can be performed on the Play1 based version of the application.

        plugins.disable.0=play.db.jpa.JPAPlugin
        plugins.disable.1=play.modules.jpastats.JPAStatsPlugin
  
  * **TIP**: Split the files in `app/models/` into entities and repositories. The repository classes can make use of the `play.db.jpa.JPARepository` class to ease the reimplementation of the removed methods mentioned earlier.

```java
public class UserRepo {

  static findById(final long id) {
    return JPARepository.from(User.class).findById(id);
  }
}
```

* `refresh()` has been removed from `play.db.jpa.GenericModel`; simply replace with: `JPA.em().refresh(entityYouWantToRefresh)`

* `play.Logger` has been slimmed down: in RePlay it just initializes the *slf4j* logger, it cannot be used for actual logging statements.
  * Where the `Logger` of Play1 uses the `String.format` interpolation (with `%s`, `%d`, etc.), the *slf4j* uses `{}` for interpolation (which is a bit faster).
  * **TIP**: You can already replace the use of `play.Logger` with the *slf4j* logger in your Play1 application.
  * In RePlay logging is done as follows (common Java idiom):

```java
import org.slf4j.Logger; // add these
import org.slf4j.LoggerFactory;

public class YourClassThatNeedsLogging {
  // the following line allows quick access to the logger within this class' context
  private static final Logger logger = LoggerFactory.getLogger(YourClassThatNeedsLogging.class);

  public YourClassThatNeedsLogging(int i) {
    logger.debug("Constructor invoked with param: {}", i);  // example logging statement
  }
  // ...
}
```

## Licence

RePlay framework is distributed under [MIT license](https://github.com/codeborne/replay/blob/master/LICENSE).

The original Play1 framework is distributed under [Apache 2 licence](http://www.apache.org/licenses/LICENSE-2.0.html).

