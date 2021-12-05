# The RePlay framework

RePlay is a fork of the [Play1](https://github.com/playframework/play1) framework, made and maintained by [Codeborne](https://codeborne.com).
Forking was needed to make some breaking changes (detailed below) that would otherwise not be possible.

RePlay originally forked Play 1.5.0, applicable improvements made in the Play1 project are regularly copied into RePlay.


#### Main differences between RePlay and Play1

* It uses the [Gradle build tool](https://gradle.org) for dependency management and builds (better incremental builds, no vendor code in the framework project's version control, no need for [Ivy](https://ant.apache.org/ivy) and Play1's Python scripts to manage dependencies).
* Removes most built-in Play modules (console, docviewer, grizzly, secure, testrunner) that were not used at Codeborne.
* Improved compile times, mostly by avoiding reliance on [JBoss Javassist](https://www.javassist.org) for bytecode manipulating "enhancers".
* Support for [Kotlin out of the box](/codeborne/replay/tree/master/replay-tests/helloworld-kotlin) (impossible in Play1 due to runtime bytecode manipulation).
* Less magic (like "enhancers" and creative use of exceptions for redirecting/responding/returning from controller methods).
* Follows OO best practices: no need for using static fields/methods throughout your application code like in Play1.
* More actively maintained.
* Promotes [dependency injection](/codeborne/replay/tree/master/replay-tests/dependency-injection) for decoupling concerns (using Google's [Guice](https://github.com/google/guice) as a DI provider).
* More modular: where possible functionality has been refactored into plugins (more on that below).
* Much improved development flow by greatly improved build and start-up times (due to Gradle and lack of runtime bytecode manipulation).


## Getting started

Unlike with Play1, RePlay does not make use of the `play` command line tool (written in Python 2.7).

Therefore, there `play new` scaffolding is not available. It is advised to simply start with RePlay's [up-to-date demo application](https://github.com/asolntsev/criminals), and work from there.
The projects in RePLay's `replay-tests/` folder also show how to do certain things in RePlay (like the use of Kotlin and dependency injection with Guice).

**NOTE**: Due to its small community, RePlay is not likely the best choice for a new project. Same holds true for Play1 and even Play2. RePlay primarily caters to Play1 codebases. It provides a simpler, more standard framework with greatly improved developer ergonomics.


## Documentation

For a large part the [documentation of Play1](https://www.playframework.com/documentation/1.5.x/home) may be used as a reference. Keep the main differences between Play1 and RePlay (outlined above) in mind, to know what parts of Play1's documentation to ignore.

API docs for the RePlay `framework` package are generated with `./gradlew :framework:javadoc` after which they are found in the `/framework/build/docs/javadoc/` folder. The [javadoc.io](https://javadoc.io) project provides online access to the javadocs for RePlay's [framework](https://javadoc.io/doc/com.codeborne.replay/framework), [fastergt](https://javadoc.io/doc/com.codeborne.replay/fastergt), [guice](https://javadoc.io/doc/com.codeborne.replay/guice), [excel](https://javadoc.io/doc/com.codeborne.replay/excel), [pdf](https://javadoc.io/doc/com.codeborne.replay/pdf) and [liquibase](https://javadoc.io/doc/com.codeborne.replay/liquibase) packages.


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

The following should start your application from the command line (possibly adding additional flags):

    java -cp "build/lib/*:build/libs/*:build/classes/java/main" appname.Application

Replace `appname` with the name of the package your `Application` class resides in. The classpath string (after `-cp`) contains three parts:

1. The first bit (`build/lib/*`) points to the dependencies of the project as installed by Gradle.
2. The second bit (`build/libs/*`) points the application JAR file as build Gradle.
3. The last bit points to the folder with the application's `.class` files (`build/classes/java/main`) built by the Gradle build script, as that's what RePlay (and Play1 as well) use instead of the copies of these files as found in the application's JAR file.


## Troubleshooting

You may find some `WARNING` blocks in the logs when running the application. These are safe to ignore, below some further explanation on what causes then and how to possibly fix them.

Add this flag `--add-opens java.base/java.lang=ALL-UNNAMED` to reduce "illegal reflective access" warnings from `guice` (this will be fixed in a future version of `guice`).

The "illegal reflective access" warnings from `netty` are harder to fix: RePlay should upgrade it's `netty` dependency from `3.10.6.Final` to `io.netty:netty-all:4.1.43.Final` or greater.


## Plugins

Play1 [installs some plugins out-of-the-box](https://github.com/playframework/play1/blob/master/framework/src/play.plugins) which you can then disable in your project.
The plugins that Play1 setup by default will need to be explicitly added to your RePlay project's `play.plugins` file. The ability to disable plugins is no longer needed.

Some Play1 plugins do not have a RePlay equivalent, such as: `play.plugins.EnhancerPlugin` (RePlay does not do byte-code "enhancing" by design),
`play.ConfigurationChangeWatcherPlugin`, `play.db.Evolutions`, `play.plugins.ConfigurablePluginDisablingPlugin` (no longer needed as you pick the plugins you need in your own project).

The RePlay project comes with the following plugins:

* `play.data.parsing.TempFilePlugin` 🟊 — Creates temporary folders for file parsing and deletes them after request completion.
* `play.data.validation.ValidationPlugin` 🟊 — Adds validation on controller methods parameters based on annotations.
* `play.db.DBBrowserPlugin` 🟊 — Mounts the H2 Console on `/@db` in development mode. Only works for the in-memory H2 database. 
* `play.db.DBPlugin` 🟊 — Sets up the Postgres, MySQL or H2 data source based on the configuration values.
* `play.db.jpa.JPAPlugin` 🟊 — Initialises required JPA EntityManagerFactories. 
* `play.i18n.MessagesPlugin` 🟊 — The internationalization system for UI strings.
* `play.jobs.JobsPlugin` 🟊 — Simple cron-style or out-of-request-cycle jobs runner.
* `play.libs.WS` 🟊 — Simple HTTP client (to make webservices requests).
* `play.modules.excel.Plugin` — Installs the Excel spreadsheet rendering plugin (requires the `com.codeborne.replay:pdf` library). In Play1 this is available as a community plugin.
* `play.modules.gtengineplugin.GTEnginePlugin` 🟊🟊 — Installs the Groovy Templates engine for rendering views (requires the `com.codeborne.replay:fastergt` library).
* `play.modules.logger.ExceptionsMonitoringPlugin` — Keeps some statistics on which exceptions occured and includes them in the status report.
* `play.plugins.PlayStatusPlugin` 🟊 — Installs the authenticated `/@status` endpoint.
* `play.plugins.security.AuthenticityTokenPlugin` — Add automatic validation of a form's `authenticityToken` to mitigate [CSRF attacks](https://en.wikipedia.org/wiki/Cross-site_request_forgery). In Play1 the `checkAuthenticity()` method is built into the `Controller` class and needs to be explicitly called.

🟊) Installed by default in Play1.

🟊🟊) Built into the Play1 framework (not as a plugin), became a plugin in RePlay.

A community [plugin for creating PDFs](https://github.com/pepite/play--pdf) exists for Play1.
In RePlay this functionality is [part of the main project](https://github.com/codeborne/replay/tree/master/pdf) and available as a regular library (no longer a plugin) named `com.codeborne.replay.pdf`.

RePlay projects put `play.plugins` file in `conf/`. The syntax of the `play.plugins` file remains the same.

Write your own plugins by extending `play.PlayPlugin` is still possible.


## Porting a Play1 application over to RePlay

Porting a Play1 application to RePlay requires quite some work, depending on the size of the application.
The work will be significantly less than porting the application to [Play2](https://www.playframework.com) or a currently popular Java MVC framework (like [Spring Boot](https://spring.io/projects/spring-boot)).

The following list breaks down the porting effort into tasks:

* Port the dependency specification from `conf/dependencies.yml` (Ivy2 format) to `build.gradle` (Gradle format).
* Move `app/play.plugins` to `conf/` and add all plugins you need explicitly (see the section on "Plugins").
* Add the `app/<appname>/Application.java` and `app/<appname>/Module.java` (see the [RePlay example project](https://github.com/asolntsev/criminals/tree/master/app/criminals) for inspiration). 
* Port the controller classes over to RePlay. This is likely the biggest task of the porting effort and cannot be performed from your Play1 application like some other porting tasks.
  * Make all controller classes action methods (the ones pointed at by `conf/routes`) non-static. You may need to remove the `static` keyword from some non-action methods as well.
  * Make all controller classes action methods return `play.mvc.Result` instead of `void` (as RePlay does abuse exceptions for non-exceptional control flow).
  * `renderJson(...);` becomes `return new RenderJson(...);`.
  * `render(...);` becomes `return View(...);`, with slightly different arguments e.g.:
    * `render(token);` becomes `return new View("path/to/ControllerName/template.html", Map.of("token", token));`  
  * Triggering **redirects** using the bytecode mingled Play1 idiom `Controller.actionMethod(...);` or just `actionMethod()` (in the same class) becomes `return Redirect(...);` where the *path* is provided as first argument.
  * `Http.Request.current()` becomes `request` (as in Play1 many things are static that are not in RePlay).
  * `notFoundIfNull(token);` becomes `if (token == null) return new NotFound("Token missing");`.
  * Methods annotated with `@Before` and `@After` also return `Result`, and should return `null` to signify continuation (especially in case of `@Before`).
  * Since `play.result.Result` no longer extends `Exception`, your controllers should return `Result` instead of throwing it. In Play1 the controller methods that trigger the request's response (like `render(...)` and `forbidden()`) throw an exception to ensure the rest of the method will not be executed. In RePlay this is considered abuse of the exception system, and the good old `return` statement is used to achieve the same. The following changes to your controller classes are required:
    * TODO: explain how to deal with redirects, returning `Result` (or `null`), etc.
  * **TIP**: Start by moving your controller classes to an `unported/` folder, and copy them one-by-one back to `controller/` while porting them.
* `play.libs.Crypto` has been slimmed down and is now called `Crypter`.
  * **TIP**: You can simply copy the `play.libs.Crypto` file from Play1 into your project.
* `Check`, `CheckWith` and `CheckWithCheck` have been removed from the `play.data.validation` package.
  * They have been removed in favour of calling validation methods explicitly from the body of controller methods (opposed to configuring validations through annotations). This allows much needed control over your application's response in case of validation errors.
  * **TIP**: Copy those files from Play1 into your project to ease the porting effort.
* The `params.get()` method now always returns a `String`, use `parseXYZ()` methods (like `Boolean.parseBoolean()`) to convert results.
* `play.libs.WS` has been split up into the `play.libs.ws` package containing the classes that have been split out.
* `Router.absolute()` now takes a param. Fix this by passing it `Router.absolute(Http.Request.current())`.
* `IO.readFileAsString()` now needs an additional `Charset` argument (usually `StandardCharsets.UTF_8` suffices).
* RePlay does not use the `${...}` syntax for interpolating environment variables into the `application.conf` file. This [may change](https://github.com/codeborne/replay/issues/29) in a future release.
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

  * **TIP**: The repository methods can make use of RePlay's `play.db.jpa.JPARepository` to ease the reimplementation of the removed *enhancer* methods mentioned earlier.

  * **TIP**: Split the files in `app/models/` into entities (e.g. a `User` class with the Hibernate entity definition) and repositories (e.g. a `UserRepository` class with the static methods for retrieving `User` entities from the database).

```java
public class UserRepo {

  static findById(final long id) {
    return JPARepository.from(User.class).findById(id);
  }
}
```

* `refresh()` has been removed from `play.db.jpa.GenericModel`; simply replace with: `JPA.em().refresh(entityYouWantToRefresh)`
* `play.Logger` has been slimmed down (and renamed to `PlayLoggingSetup`). In RePlay it merely initializes the *slf4j* logger within the framework, it cannot be used for actual logging statements (e.g. `Logger.warn(...)`).
  * Where the `Logger` of Play1 uses the `String.format` interpolation (with `%s`, `%d`, etc.), the *slf4j* uses `{}` for interpolation (which is a bit faster).
  * **TIP**: You can already replace the use of `play.Logger` with the *slf4j* logger in your Play1 application.
  * In RePlay logging is done as follows (common Java idiom):

```java
import org.slf4j.Logger; // replace `import play.Logger;` these
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

* `Play.classloader` is removed; replace it with `CurrentClass.class.getClassLoader()`.
* `play.cache.Cache.get(...)` only takes one argument, removing additional arguments is usually enough.
* `play.Plugin` changed some of the method signatures.
* `play.data.binding.TypeBinder` changed some of the method signatures.
* `play.jobs.Job` requires overriding of `doJobWithResult()` instead of `doJob()`. If the job does not have any return value the subclass should be parameterized over `Void` and `return null;`. For example:

```java
@OnApplicationStart
public class LoadMenuJob extends Job<Void> {

  @Override
  public Void doJobWithResult() {
    // ...
    return null;
  }
}
```


## Licence

RePlay framework is distributed under [MIT license](https://github.com/codeborne/replay/blob/master/LICENSE).

The original Play1 framework is distributed under [Apache 2 licence](http://www.apache.org/licenses/LICENSE-2.0.html).

