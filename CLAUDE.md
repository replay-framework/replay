# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this repo is

RePlay is a fork of the Play1 web framework, maintained by Codeborne. It is a multi-module Gradle project that publishes a set of `io.github.replay-framework:*` libraries to Maven Central. This repo is the framework itself; downstream apps depend on these jars. Demo applications under `replay-tests/` double as the integration test bed.

## Build & test commands

Toolchain: JDK 17 (set via `java.toolchain` in the root `build.gradle`). Tests run on JUnit 5 (`useJUnitPlatform`), with locale forced to `tr_TR` and encoding to UTF-8 — keep that in mind when debugging locale-sensitive output.

```bash
./gradlew check                       # full per-module unit tests + checks
./gradlew :framework:test             # unit tests for one module
./gradlew :framework:test --tests play.mvc.RouterTest          # one test class
./gradlew :framework:test --tests 'play.mvc.RouterTest.*name*' # filter
./gradlew uitest                      # runs all uitest-* variants below
./gradlew uitest-netty3               # UI/integration tests against Netty3 backend
./gradlew uitest-netty4               #   "        "      "        Netty4
./gradlew uitest-javanet              #   "        "      "        java.net (jdk httpserver)
./gradlew uitest -Dselenide.browser=firefox -Dselenide.headless=true
./gradlew check uitest publishToMavenLocal   # what CONTRIBUTING.md prescribes for a full local build
./gradlew precompileTemplates         # CI also runs this
./gradlew googleJavaFormat            # format Java sources (Google style, configured at root)
```

Releases go via `./release.sh X.Y.Z` (tag, publish to Sonatype) — only invoke when explicitly asked. Procedure documented in `CONTRIBUTING.md`.

## Module layout (settings.gradle)

The build is non-standard: every subproject uses `srcDir 'src'` and `srcDir 'test'` (not `src/main/java`). Resources live alongside `.java`/`.kt` and are filtered out of source sets by exclude patterns — keep that mental model when adding new files.

Top-level modules and what they ship:

- `framework` — the core `play.*` packages (controllers, routing, MVC, JPA, jobs, plugins, i18n, validation, cache API, template engine glue). Everything else depends on this.
- `netty3`, `netty4`, `javanet` — three interchangeable HTTP server backends. A downstream app picks exactly one. The CI matrix runs UI tests against all three.
- `guice` — Google Guice dependency-injection integration (optional).
- `fastergt` — the Groovy Templates engine, packaged as a plugin (`play.modules.gtengineplugin.GTEnginePlugin`). Required for any app using `.html`/`.txt` Groovy templates.
- `liquibase` — Liquibase plugin for DB migrations.
- `pdf`, `excel` — rendering plugins (formerly Play1 contrib modules).
- `ehcache`, `memcached` — pluggable cache backends. Apps that want caching pull in exactly one; absent both, caching is a no-op (`DummyCacheImpl`).
- `replay-tests/*` — example/integration apps (`criminals`, `helloworld`, `helloworld-kotlin`, `dependency-injection`, `liquibase-app`, `multi-module-app`). They share `replay-tests/replay-tests.gradle`, which defines `uitest`, `uitest-netty3`, `uitest-netty4`, `uitest-javanet`. UI tests under `test/ui/**` use Selenide; non-UI tests are excluded from `uitest-*` and run via `:test`.

## Architecture notes that span files

- **Plugin system.** The framework is mostly a `PlayPlugin` host. Each app's `conf/play.plugins` lists plugin FQCNs in priority order; `PluginCollection`/`PluginDescriptor` load them; `PlayPlugin` defines the lifecycle hooks (`onApplicationStart`, `beforeInvocation`, `onRoutesLoaded`, etc.). Most "framework features" (DB, JPA, jobs, validation, i18n, WS, status endpoint, request logging, CSRF) are implemented as plugins under `framework/src/play/`. When adding cross-cutting behavior, prefer a new plugin over hooking into core.
- **Server backend abstraction.** `play.server.PlayServer` / `Starter` are implemented separately in each of `netty3`, `netty4`, `javanet`. The chosen backend is on the runtime classpath of the application, never of the framework. UI tests prove all three behave equivalently.
- **No bytecode "enhancers".** A core design decision vs. Play1: RePlay does not use Javassist to rewrite classes at load time. Anything in `play.classloading` is for classpath scanning / hot-reload, not bytecode mutation. Many Play1 idioms (static action methods, `throw`-based responses, auto-generated getters/setters, JPA finder methods) therefore do not work — see the porting guide in `README.md` for the canonical rewrites.
- **Controllers return `Result`, never throw it.** `play.mvc.results.*` (`View`, `RenderJson`, `Redirect`, `NotFound`, `Ok`, …) are returned. `@Before`/`@After` methods also return `Result` and return `null` to continue. This is the most frequent friction point when porting Play1 code.
- **Routing.** `conf/routes` is parsed by `play.mvc.Router`; routes resolve to `Controller.action` strings. `Router.absolute(Http.Request.current())` (RePlay) replaces Play1's no-arg `Router.absolute()`.
- **Multi-module apps.** RePlay does not support Play1-style nested modules. Instead, set `play.classes.scanJars=foo-*.jar,bar-*.jar` in `application.conf` so the classpath scanner descends into those jars. See `replay-tests/multi-module-app`.
- **Sessions/cookies.** `CookieSessionStore` uses different signing/encryption than Play1, so a Play1 app and a RePlay app cannot run side-by-side over the same domain — users would be logged out continuously.

## Code style & conventions

- Java sources are formatted by `google-java-format` (Gradle plugin, version pinned to 1.8 at the root). `.editorconfig` enforces 2-space indent, LF line endings, UTF-8, 100-col line length, no trailing whitespace, no final newline.
- `-parameters` is required at compile time (used by reflective param-name binding in controllers); enabled in `build.gradle` for both Java and IDEA.
- Nullability is annotated with **JSpecify** (`org.jspecify.annotations.Nullable`/`@NonNull`). Do not introduce JSR-305 (`javax.annotation.Nullable`) — it was removed in 2.8.0.
- Logging: SLF4J (`org.slf4j.Logger` + `LoggerFactory`). `play.Logger` does not exist for runtime logging; `PlayLoggingSetup` only wires SLF4J. Use `{}` placeholders, not `String.format`.
- Kotlin is allowed in source/test sets (`include '**/*.kt'`); `helloworld-kotlin` is the canonical example.

## CI

`.github/workflows/test.yml` runs three matrices — Linux × {netty3, netty4, javanet}, plus Windows (javanet) and macOS (netty3). Each runs `clean test`, `precompileTemplates`, then the corresponding `uitest-*`. If you add a feature that touches the HTTP layer, expect failures in the other backends — fix them before merging.

## When porting Play1 code

`README.md` has a long, authoritative "Porting a Play1 application to RePlay" section. Read it before suggesting changes that look like Play1 idioms (`renderJSON(...)`, static action methods, `Model.findById`, `JPA.setRollbackOnly()`, `play.libs.Crypto`, `Http.Request.current()` in non-controller code, etc.). It documents the exact RePlay replacements.