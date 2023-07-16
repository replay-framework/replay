# The RePlay Framework &nbsp; [![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.codeborne.replay/framework/badge.svg?style=flat-square)](https://mvnrepository.com/artifact/com.codeborne.replay/framework)
```
    ______  ______                 _            _
   /     / /     /  _ __ ___ _ __ | | __ _ _  _| |
  /     / /     /  | '_ / -_) '_ \| |/ _' | || |_|
 /     / /     /   |_/  \___|  __/|_|\____|\__ (_)
/_____/ /_____/             |_|            |__/
                   RePlay Framework, https://github.com/codeborne/replay
```

RePlay is a fork of the [Play1](https://github.com/playframework/play1) framework, made and maintained by [Codeborne](https://codeborne.com).
Forking was needed to make some breaking changes (detailed below) that would not be acceptable on Play1.
Compared to Play1, RePlay is a simpler and more standard/modern framework with greatly improved developer ergonomics.
The main differences between Play1 and RePlay are outlined below.

RePlay originally forked Play v1.5.0. Improvements made in the Play1 project since, are regularly ported to RePlay when applicable.
[Version 2 of the Play Framework](https://github.com/playframework/playframework) (Play2) is significantly different from Play1.
It caters for Scala web application projects, and uses Scala internally. 
Porting a Play1 application to Play2 is really hard and has [questionable benefits](https://groups.google.com/g/play-framework/c/AcZs8GXNWUc).
RePlay aims to provide a more sensible upgrade path for Play1 applications.


#### Main differences between RePlay and Play1

* It uses the [Gradle build tool](https://gradle.org) for dependency management and builds:
  * better compile times by incremental builds,
  * no vendor code in your RePlay application's version control,
  * and no need for [Ivy](https://ant.apache.org/ivy) and Play1's Python scripts to manage dependencies.
* Removes most built-in Play modules (console, docviewer, grizzly, secure, testrunner) and the ability to serve WebSockets.
These were not used at Codeborne, but could be reintroduced if needed.
* The `pdf` and `excel` Play1 contrib modules are part of the RePlay project as plugins in separate libraries.
* It does not use [JBoss Javassist](https://www.javassist.org) for bytecode manipulating "enhancers":
  * shorter application startup times (seriously improves development cycles),
  * and for other JVM languages out of the box, like Kotlin ([example project](/codeborne/replay/tree/main/replay-tests/helloworld-kotlin)).
* Less "magic", like: the before mentioned "enhancers" and creative use of exceptions for redirecting/responding/returning in controller methods.
* No overuse of `static` fields/methods throughout your application code; RePlay uses generally accepted OO best practices.
* More actively maintained.
* Promotes [dependency injection](/codeborne/replay/tree/main/replay-tests/dependency-injection) for decoupling concerns
(using Google's [Guice](https://github.com/google/guice) as a DI provider like Play2).
* Where possible functionality has been refactored into plugins (more on that below) to increase modularity.


## Getting started

RePlay does not come with the `play` command line tool (written in Python 2.7) that it part of Play1.
Hence, the `play new` scaffolding generator is not available in RePlay.
To start a new RePlay application make a copy of [demo application](https://github.com/codeborne/replay/tree/main/replay-tests/criminals) and work your way up from there.

Subprojects in RePlay's `replay-tests/` folder show how to do certain things in RePlay (like using LiquiBase, Kotlin and dependency injection with Guice).

Documentation for RePlay is found in (or referred to from) this README.

**NOTE**: Due to its small community, RePlay is not likely the best choice for a new project.
Same holds true for Play1 and even Play2 (when not using Scala). RePlay primarily caters to maintainers of Play1-based applications.
This README contains an extensive guide for porting Play1 applications to RePlay.


## Documentation

For a large part the [documentation of Play1](https://www.playframework.com/documentation/1.5.x/home) may be used as a reference.
Keep the differences between Play1 and RePlay (outlined above) in mind, in order to know what parts of the Play1 documentation to ignore.

API docs for the RePlay `framework` package are generated with `./gradlew :framework:javadoc` after which they are found in the `/framework/build/docs/javadoc/` folder.
The [javadoc.io](https://javadoc.io) project provides online access to the [*Javadoc* documentation of RePlay](https://javadoc.io/doc/com.codeborne.replay).


## Development flow (hot-swapping)

RePlay does not come with the `play` command line tool, which is used to start a Play1 application in development mode (i.e.: with `play run`)
providing auto-compilation and hot-swapping of code changes.

Developers of RePlay applications need to set up an IDE to get a good development flow.

These steps set up a hot-swapping development flow with IntelliJ IDEA for the [criminals](https://github.com/codeborne/replay/tree/main/replay-tests/criminals) RePlay example application:

0. Clone the [replay](https://github.com/codeborne/replay) repository
1. Use IntelliJ IDEA to `File > Open...` the replay project (**not** `Import`) by selecting the root of this project's repository
2. In `File > Settings... > Build, Excecution, Deployment > Compiler > Java Compiler` fill the *Additional command line parameters*
with the value: `-parameters` (also found in the `build.gradle` file, it put the method argument names in the bytecode by which the framework auto-binds params at run time)
3. In `File > Settings... > Build, Excecution, Deployment > Buitl Tools > Gradle` set both *Build and run using* and
*Run test using* to `IntelliJ IDEA` (makes restarting and hot-swapping much faster)
4. Go to `Run -> Edit Configurations...`, click the `+` (Add New Configuration) in the top-right corner and select "Application"
5. Fill in the following details, and click `OK`:
  * *Name*: `Criminals` (how this run/debug configuration shows up in the IntelliJ UI)
  * *JDK/JRE*: select one you prefer (Java 11 is a minimum requirement, Java 17 seems to work fine)
  * *Use classpath of module*: `criminals.main`
  * *Main class*: `replay.replay-tests.criminals.Application` (the package name, `criminals` should match the package that contains you `Application` class in `app/`)
  * *VM options* (shown with *Modify options* drop-down item *Add VM options*): `-XX:+ShowCodeDetailsInExceptionMessages` (for more helpful errors) (applicable only for Java 14+)

Now a "Run/Debug Configuration" with the name of your app shows up in the top-right of the screen.
You can press the "Run" button (with the green play icon) to start the application from the IDE.

To run the application in debug mode press the "Debug" button (with a little bug icon, next to the "Run" button) and all should work.

When in debug mode you can use `CTRL-SHIFT-F9` to "Reload Changed Classes" (as IntelliJ calls hot-swapping).
This only works when class/method signatures were not changed.
If hot-swapping failed, you will see a notification in IDEA after which you need to restart the application.
To fully restart the project in debug mode use `SHIFT-F9`, or press the bug icon button again.

Finally, in `File -> Settings... -> Build, Execution, Deployment -> Build Tools -> Gradle` set both
*Build and run using* and *Run test using* to `IntelliJ IDEA`.
This should make restarting and hot-swapping much (5-30x) faster!


## Deploying

The `./gradlew jar` command produces the `build/libs/appname.jar` file.

The following should start your application from the command line (possibly adding additional flags):

    java -cp "build/classes/java/main:build/libs/*:build/lib/*" appname.Application

Replace `appname` with the name of the package your `Application` class resides in. The classpath string (after `-cp`) contains three parts:

1. The first bit points to the folder with the application's `.class` files (`build/classes/java/main`) built by the Gradle build script,
as that's what RePlay (and Play1 as well) use instead of the copies of these files as found in the application's JAR file.
2. The second bit (`build/libs/*`) points the application JAR file as build by Gradle (e.g.: `./gradlew jar`).
3. The last bit (`build/lib/*`) points to the dependencies of the project as installed by Gradle
(should be last, or they may overshadow project definitions).


## Troubleshooting

You may find some warnings for "illegal reflective access" when running the application. These are safe to ignore up until JVM version 17.

With this flag `--add-opens java.base/java.lang=ALL-UNNAMED` these warnings from `guice` may be suppressed (this will be fixed in a future version of `guice`).

To suppress the "illegal reflective access" warnings from `netty` you could use `com.codeborne.replay:netty4` instead of `com.codeborne.replay:netty3`.


## Plugins

Play1 [installs some plugins out-of-the-box](https://github.com/playframework/play1/blob/main/framework/src/play.plugins) which you can disable in your project.
The plugins that Play1 enables by default will need to be explicitly added to your RePlay project's `play.plugins` file.
The ability to disable plugins is no longer needed (and has therefor been removed).

Some Play1 plugins do not have a RePlay equivalent, such as:
`play.plugins.EnhancerPlugin` (RePlay does not do byte-code "enhancing" by design),
`play.ConfigurationChangeWatcherPlugin`, `play.db.Evolutions` and `play.plugins.ConfigurablePluginDisablingPlugin`
(no longer needed as just explained).

The RePlay project comes with the following plugins:

* `play.data.parsing.TempFilePlugin`¹ — Creates temporary folders for file parsing and deletes them after request completion.
* `play.data.validation.ValidationPlugin`¹ — Adds validation on controller methods parameters based on annotations.
* `play.db.DBPlugin`¹ — Sets up the Postgres, MySQL or H2 data source based on the configuration values.
* `play.db.jpa.JPAPlugin`¹ — Initialises required JPA EntityManagerFactories. 
* `play.i18n.MessagesPlugin`¹ — The internationalization system for UI strings.
* `play.jobs.JobsPlugin`¹ — Simple cron-style or out-of-request-cycle jobs runner.
* `play.libs.WS`¹ — Simple HTTP client (to make webservices requests).
* `play.modules.excel.Plugin` — Installs the Excel spreadsheet rendering plugin (requires the `com.codeborne.replay:pdf` library).
In Play1 this is available as a community plugin.
* `play.modules.gtengineplugin.GTEnginePlugin`² — Installs the Groovy Templates engine for rendering views (requires the `com.codeborne.replay:fastergt` library).
* `play.modules.logger.RequestLogPlugin` — logs every request with response type+status
* `play.modules.logger.RePlayLogoPlugin` — Shows the RePlay logo at application startup
* `play.plugins.PlayStatusPlugin`¹ — Installs the authenticated `/@status` endpoint.
* `play.plugins.security.AuthenticityTokenPlugin` — Add automatic validation of a form's `authenticityToken`
to mitigate [CSRF attacks](https://en.wikipedia.org/wiki/Cross-site_request_forgery).
In Play1 the `checkAuthenticity()` method is built into the `Controller` class and needs to be explicitly called.

¹) This plugin is installed by default in Play1 (no entry in the `play.plugins` file needed).

²) Built into the Play1 framework (not as a plugin), shipped as a plugin in RePlay.

A community [plugin for creating PDFs](https://github.com/pepite/play--pdf) exists for Play1.
In RePlay this functionality is [part of the main project](https://github.com/codeborne/replay/tree/main/pdf)
and available as a regular library (no longer a plugin) named `com.codeborne.replay.pdf`.

RePlay projects put `play.plugins` file in `conf/`. The syntax of the `play.plugins` file remains the same.

Write your own plugins by extending `play.PlayPlugin` is still possible, porting [one of the many Play1 modules](https://www.playframework.com/modules)
to RePlay should be straightforward or not needed at all.


## Porting a Play1 application to RePlay

Porting a Play1 application to RePlay requires quite some work, depending on the size of the application.
The work will be significantly less than porting the application to [Play2](https://www.playframework.com) or
a currently popular Java MVC framework (like [Spring Boot](https://spring.io/projects/spring-boot)).

A serious part of the work stems from the removal of Play1's "Enhancers", these use JBoss Javassist to
apply runtime bytecode manipulation which add methods and intercept method calls or member field access.
Removing the enhancers gives RePlay many of it's benefits: quick builds, reduce start-up times, allow non-Java JVM language interop,
reduce magic, make mocking easier and results in more idiomatic Java code.

It is advised to perform the porting work as much as possible while still being based on Play1.
This allows you to break-up the effort in smaller "testable" steps
making the effort more incremental and thereby greatly reducing the complexity of actual switch to RePlay.
Where this is possible the guide below points this out with a **TIP**.

The following list breaks down the porting effort into tasks:

* Port the dependency specification from `conf/dependencies.yml` (Ivy2 format) to `build.gradle` (Gradle format).
* Move `app/play.plugins` to `conf/` and add all plugins you need explicitly (see the section on "Plugins").
* Add the `app/<appname>/Application.java` and `app/<appname>/Module.java` (see the
[RePlay example project](/Users/andrei/projects/replay/replay-tests/criminals) for inspiration). 
* Play1's [`PropertiesEnhancer`](https://github.com/playframework/play1/blob/master/framework/src/play/classloading/enhancers/PropertiesEnhancer.java) was removed.
  * This enhancer reduces the boilerplate needed to make classes adhere to the "Java Bean" standard.
  In short: a *bean* is a Java class that (1) implements `java.io.Serializable`, (2) implements public getter/setter methods for accessing the state, and
  (3) implements the default constructor (a public constructor that takes no arguments).
  All `@Entity` annotated classes (e.g. model classes) should adhere to the Bean standard.
  Play1's `PropertiesEnhancer` creates the default constructor in case it is absent, creates getter/setter methods and
  rewrites direct access to Entities' public member fields (e.g.: `obj.memberField;` and `obj.memberField = newValue;`)
  to calls to the corresponding getter/setter methods.
  * In for a large part the model code still works: adherence to the Java Bean standard is not strictly enforced.
  * In some cases the model code does not work without Play1's PropertiesEnhancer:
    * Runtime errors for lacking default constructors: simply implement them for all `@Entity` annotated classes.
    In most cases adding `public ClassName() {}` suffices. IntelliJ can help with that.
    * In some cases a member field's getter needs to be implemented and used (instead of the member field access) for Hibernate to work.
    IntelliJ can do this per file, right-click a file and `Refactor > Encapsulate fields`, where you pick all public non-static fields.
    * Since Groovy maps direct field access to use the getters and setters, your template code should still work.
  * **TIP**: By setting `play.propertiesEnhancer.enabled=false` in `conf/application.conf` of a Play1 project,
  this work required can be performed on the Play1 based version of the application.
  * **TIP2**: Use IntelliJ's `Refactor -> Encapsulate Fields...` on all `@Entity` annotated classes to have them generated for you.
  * **TIP3**: For the `id` field on `play.db.jpa.Model` only encapsulate with a getter.
  To do so copy the `Model` class into your project, string replace all `import play.db.jpa.Model` to point to your copy of the class,
  run `Refactor -> Encapsulate Fields...` on your own class (only generate the getter!),
  finally remove your class and string replace the imports back to what they were.
* Play1's `JPAEnhancer` was removed.
  * In RePlay, classes that extend `Model` have to implement `create`, `count`, `find*`, `all` and `delete*` methods themselves.
    * **TIP**: Reimplementing these methods using the methods found in RePlay's `play.db.jpa.JPARepository`.
  * **TIP**: By adding the following lines to `conf/application.conf` of a Play1 project,
  the work required can be performed on the Play1 based version of the application.

        plugins.disable.0=play.db.jpa.JPAPlugin
        plugins.disable.1=play.modules.jpastats.JPAStatsPlugin

  * **TIP**: Split the files in `app/models/` into entities (e.g. a `User` class with the Hibernate entity definition) and
  repositories (e.g. a `UserRepository` class with the static methods for retrieving `User` entities from the database).

```java
public class UserRepo {

  static findById(final long id) {
    return JPARepository.from(User.class).findById(id);
  }
}
```

* `refresh()` has been removed from `play.db.jpa.GenericModel`; simply replace occurrences with: `JPA.em().refresh(entityYouWantToRefresh)`
* `JPA.setRollbackOnly()` becomes `JPA.em().getTransaction().setRollbackOnly()`
* `play.Logger` has been slimmed down (and renamed to `PlayLoggingSetup`). In RePlay it merely initializes the *slf4j* logger within the framework,
it cannot be used for actual logging statements (e.g. `Logger.warn(...)`).
  * Where the `Logger` of Play1 uses the `String.format` interpolation (with `%s`, `%d`, etc.), the *slf4j* uses `{}` for interpolation (which is a bit faster).
  * **TIP**: You can already replace the use of `play.Logger` with the *slf4j* logger in your Play1 application.
  * In RePlay logging is done as follows (common Java idiom):

```java
import org.slf4j.Logger; // replace `import play.Logger;` with these
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
* `play.libs.Crypto` has been slimmed down and is now called `Crypter`.
  * **TIP**: You can simply copy the `play.libs.Crypto` file from Play1 into your project.
* `play.libs.WS` has been split up into the `play.libs.ws` package containing the classes that have been split out.
* `Router.absolute()` now takes a param. Fix this by passing it `Router.absolute(Http.Request.current())`.
* `IO.readFileAsString()` now needs an additional `Charset` argument (usually `StandardCharsets.UTF_8` suffices).
* `play.cache.Cache.get(...)` only takes one argument, removing additional arguments is usually enough.
* `play.Plugin` changed some of the method signatures.
* `play.data.binding.TypeBinder` changed some of the method signatures.
* `play.jobs.Job` requires overriding of `doJobWithResult()` instead of `doJob()`.
  If the job does no return value the subclass should be parameterized over `Void`, and return `null`. For example:

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

* Port the controller classes over to RePlay. This is likely the biggest task of the porting effort and
cannot be performed from your Play1 application like some other porting tasks.
  * Since `play.result.Result` no longer extends `Exception`, your controllers should return `Result` instead of throwing it.
  In Play1 the controller methods that trigger the request's response (like `render(...)`, `renderText(...)` and `renderJSON(...)`) throw an exception.
  By this exception the rest of the method will not be executed (which may confuse IDEs).
  In RePlay this is considered abuse of the exception system, and the good old `return` statement is used to achieve the same.
  The following changes are needed to adapt Play1 code to this:
    * Make all action methods (the ones pointed at by `conf/routes`) non-static. You may need to remove the `static` keyword from some non-action methods as well.
    * Make all action methods return `play.mvc.Result` instead of `void` (as RePlay does use exceptions for responding to requests). Some examples of what needs to change:
      * `renderJson(...);` becomes `return new RenderJson(...);`.
      * `render(...);` becomes `return new View(...);`, with slightly different arguments e.g.:
        * `render(token);` becomes `return new View("path/to/ControllerName/template.html", Map.of("order", order, "token", token));`,
        or `return new View("...").with(("order", order).with("token", token);`. The second style allows `null` values to be passed to the template.
      * `notFoundIfNull(token);` becomes `if (token == null) return new NotFound("Token missing");`.
      * Triggering **redirects** using the bytecode mingled Play1 idiom `Controller.actionMethod(...);` or just `actionMethod()` (in the same class)
      becomes `return Redirect(...);` where the *path* is provided as first argument.
      Alternatively the `RedirectToAction` class can be used with a string pointing to controller methods (like in Play1). 
    * Methods annotated with `@Before` and `@After` also return `Result`, and should return `null` to signify continuation
    (e.g. to the next `@Before` annotated method or to the controller action method itself).
      * In RePlay the `priority` annotation-argument is removed from that annotation.
    * Sometimes private methods that return a value in Play1 also can trigger a response, because responses are triggered by exceptions in Play1.
    This is no longer allowed using RePlay and thus the code that triggers responses (actual controller code)
    should be separated from code that merely handles values (probably not controller code).
    In these cases it would be nice if Java already had multiple return values (through sum-types).
    * Private methods with a `void ` return type that trigger responses in Play1 (by throwing exceptions) need to return `Result` in replay.
    In case these private methods do not trigger a response they should return `null` in the RePlay scenario.
    The call sites of those methods need the following bit of code to pass through `Result` objects:
    `var result = privateMethod(); if (result != null) return result;`. This ensures the rest of the method is evaluated.
  * `Http.Request.current()` becomes `request` (as in Play1 many things are static that are not in RePlay).
  * `params.flash();` becomes `params.flash(flash);` (this stores the render params in the cookie to survive a redirect).
  * **TIP**: Start by moving your controller classes to an `unported/` folder, and move them one-by-one back to `controller/` while porting.
  Ensure that `git` understand files were moved to allow merging in changes from Play1-based branches of your application.
* `Validation.valid(obj)` needs an additional String as parameter to which the validation results are bound, and thus becomes: `Validation.valid("obj", obj)`
* `Check`, `CheckWith`, `CheckWithCheck` and `Equals` have been removed from the `play.data.validation` package.
  * They have been removed in favour of calling validation methods explicitly from the body of controller methods
  (opposed to configuring validations through annotations). This allows much needed control over your application's response in case of validation errors.
  * **TIP**: Copy those files from Play1 into your project to ease the porting effort (maybe except `Equals` as it has so many dependencies).
* The `params.get()` method now always returns a `String`, use `parseXYZ()` methods (like `Boolean.parseBoolean()`) to convert results.
* Writing directly to the stream `response.out`, e.g. a final call to `ImageIO.write(outputImage, "png", response.out)` with a Play1 codebase,
needs an additional `return new Ok()` with RePlay.
* While porting the controllers you will find some changes to the views (templates) are required too:
  * In some cases the full package path needs to be provided, e.g.: `Play.configuration.getProperty("key")` becomes `play.Play.configuration.getProperty("key")`.
* Due to changed encrypting/signing of `CookieSessionStore` all active sessions are logged out when migrating from Play1 to RePlay.
This means that running the Play1 version of the app side-by-side with the RePlay version is not possible (all users get logged out all the time). 

  
## Licence

The RePlay Framework is distributed under [MIT license](https://github.com/codeborne/replay/blob/main/LICENSE).

The [Play1 Framework](https://github.com/playframework/play1), that RePlay forked, is distributed under [Apache 2 licence](http://www.apache.org/licenses/LICENSE-2.0.html).

