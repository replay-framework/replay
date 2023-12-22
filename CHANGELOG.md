# Changelog

## 2.3.1 (released 22.12.2023) - see https://github.com/replay-framework/replay/milestone/14?closed=1

* Move project from https://github.com/codeborne/replay to https://github.com/replay-framework/replay
* Move project in Maven Central repo from "com.codeborne.replay" to "io.github.replay-framework" groupId
* #309 Bump Kotlin from 1.9.21 to 1.9.22
* #307 Bump LiquiBase from 4.25.0 to 4.25.1
* #308 Bump Guava from 32.1.3-jre to 33.0.0-jre

## 2.3.0 (released 16.12.2023) - see https://github.com/replay-framework/replay/milestone/13?closed=1
* #281 Replace iText by OpenPDF for generating PDFs
* #286 Replace query param decoder in UrlEncodedParser
* #301 Log exception class name when 500 occurs (#303)
* #264 Remove deprecation from Injector.getBeanOfType
* #275 Reduce exception throwing from FlashStore (#278)
* #282 don't fail app start in development mode
* #283 Make Model.id field protected (#284)
* #280 refactor PDF generation code
* Bump liquibase from 4.23.2 to 4.25.0
* Bump netty4 from 4.1.99.Final to 4.1.104.Final
* Bump com.zaxxer:HikariCP from 5.0.1 to 5.1.0
* Bump FLYING_SOURCER_VERSION from 9.2.2 to 9.3.1
* Bump Jackson from 2.15.2 to 2.16.0

## 2.2.0 (released 30.09.2023) - see https://github.com/replay-framework/replay/milestone/12?closed=1
* #228 Migrate `javax.persistence` -> `jakarta.persistence` (#229)  --  thanks to Cies Breijs
* The most important thing: A cool logo (#213)  --  thanks to Szabolcs Hubai
* Load conf/log4j.properties automatically if exists (#203)  --  thanks to Szabolcs Hubai
* Interesting bugfix in Play1 (on JPABase) (#230)  --  thanks to Cies Breijs
* #256 [refactoring] Provide method isSecure() (with caching), remove property secure (#257)  --  thanks to Cies Breijs
* #253 Add more specific url builder overloads (#255)  --  thanks to Cies Breijs
* [refactoring] Make "play.mvc.Controller".setContext implementable (#223)  --  thanks to Cies Breijs
* #182 [refactoring] Use Files instead of File (#185)  --  thanks to Cies Breijs
* Bump netty4Version from 4.1.93.Final to 4.1.99.Final
* Bump groovyVersion from 3.0.17 to 3.0.19 (#204) (#231)
* Bump org.liquibase:liquibase-core from 4.22.0 to 4.23.2
* Bump com.h2database:h2 from 2.1.214 to 2.2.224
* Bump com.fasterxml.jackson.core:jackson-databind from 2.15.1 to 2.15.2 (#179)
* Bump org.jetbrains.kotlin.jvm from 1.8.21 to 1.9.10
* Bump FLYING_SOURCER_VERSION from 9.1.22 to 9.2.1 (#242) (#243)

## 2.1.0 (released 30.05.2023) - see https://github.com/replay-framework/replay/milestone/11?closed=1
* Less logging and 400 response on URI parsing error  --  thanks to Cies Breijs for PR #178
* bump Guice from 5.1.0 to 6.0.0 - see https://github.com/google/guice/wiki/Guice600
* Bump Liquibase from 4.21.1 to 4.22.0
* Bump Guava from 31.1-jre to 32.0.0-jre
* Bump commons-io from 2.11.0 to 2.12.0
* Bump netty4 from 4.1.91.Final to 4.1.93.Final
* Bump ClassGraph from 4.8.157 to 4.8.160
* remove unused class ExceptionsMonitoringPlugin

## 2.0.0 (released 18.04.2023) - see https://github.com/replay-framework/replay/milestone/10?closed=1
* added experimental support for Netty4 (Netty 3 is also still used by default)  --  thanks to Szabolcs Hubai! (#25) (#95)
* added backend `javanet` as an alternative for Netty3/Netty4 (#152)
* Bump org.liquibase:liquibase-core from 4.20.0 to 4.21.1 (#155) (#109)
* Bump groovyVersion from 3.0.16 to 3.0.17 (#142)
* Bump ch.qos.reload4j:reload4j from 1.2.24 to 1.2.25 (#140)
* Bump slf4jVersion from 2.0.6 to 2.0.7 (#139)
* Bump org.eclipse.jdt:org.eclipse.jdt.core from 3.32.0 to 3.33.0 (#137)
* Bump ehcache from 3.10.1 to 3.10.8 (#108)

## 1.12.0 (released 13.01.2023)
* #77 Expose plugin descriptor  --  thanks to Cies Breijs for PR #79 and #66
* #78 Allow null `Play.routes` in `Router.detectChanges()`  --  thanks to Cies Breijs for PR #81
* Encrypt error cookie  --  thanks to Aleksei Anishchenko and Evgeny Tkachenya (#69)
* Make Error class immutable
* #76 expose `Play.beanSource`  --  thanks to Cies Breijs (#80)
* Restore LiquiBase plugin (#109)
* Bump liquibase-core from 4.15.0 to 4.18.0 (#92)
* Bump groovy from 3.0.13 to 3.0.14 (#94) (#105)
* Bump org.eclipse.jdt.core from 3.30.0 to 3.32.0 (#90)
* Bump ehcache from 3.10.1 to 3.10.8 (#108)
* Bump Flying Sourcer from 9.1.9 to 9.1.22 (#87)
* Bump classgraph from 4.8.149 to 4.8.154 (#85)
* Bump slf4j from 2.0.3 to 2.0.6 (#86)
* Bump reload4j from 1.2.22 to 1.2.24 (#100)
* Bump gson from 2.10 to 2.10.1 (#106)

## 1.11.0 (released 28.10.2022)

This release dropped some dependencies, if you need those simply add them as dependencies of your app.

The Hibernate upgrade exposes `jakarta.validation.*` instead of `javax.validation.*`, if you use this you need to update relevant import statements.

* #61 Remove H2 dependency (and DBBrowserPlugin)
* #62 Remove jaxen dependency (and XPath utility class)
* #56 Do not expose Apache's commons-lang v2
* #57, #59 Remove unused dependencies (ezmorph, asm, cglib, javax.activation, validation-api, jboss-logging, classmate, xmlpull, snakeyaml, jsr107cache)
* #60 Remove not-strictly-RePlay dependencies (groovy-dateutil, groovy-datetime)
* upgrade dependencies (gson:2.10, groovy:3.0.13, hibernate:5.6.12, slf4j:2.0.3)
* #48 Add test that exercises FileChannelBuffer - thanks to Cies Breijs for PR #52
* #63 Make RePlay more modular

## 1.10.0 (released 11.09.2022)

* #53 refactor PDF generation code (removed `PDF.Options`, rewritten `PDFDocument`, `PdfHelper`, `PdfGenerator`)
* #53 add an example how to implement custom plugin for generating HTML for PDF

## 1.9.5 (released 01.09.2022)
* #50 Make PdfHelper methods public  --  see PR #51
* upgrade dependencies (Eclipse compiler 3.30.0, hibernate:5.6.11.Final, groovy:3.0.12, liquibase:4.15.0, ehcache:3.10.1, slf4j:2.0.0, reload4j:1.2.22, asm:9.3, gson:2.9.1, guava:31.1-jre)

## 1.9.4 (released 04.02.2022)
* #41 fix "IOException: mark/reset not supported" when parsing http parameters

## 1.9.3 (released 03.02.2022)
* upgrade to guice guice:5.1.0, hibernate-core:5.6.5.Final, commons-beanutils:1.9.4 etc. 

## 1.9.2 (released 18.01.2022)
* replace log4j:1.2.17 -> reload4j:1.2.18.0
* upgrade slf4j:1.7.32 -> 1.7.33

## 1.9.1 (released 06.01.2022)
* #36 fix annotation-based validation  --  see PR #37
* #38 upgrade to EHCache 3.9.9, Eclipse compiler 3.28.0 etc
* #34 Make the `i18n` tag to work

## 1.9.0 (released 06.12.2021)
* upgrade to Gradle 7.x (incl. declaring dependencies with `api` or `compile`)
* add support for MySql 8 driver  --  thanks Cies Breijs for PR #32
* #29 replace environment variables in `${...}` in application.conf  --  thanks Cies Breijs for PR #31
* improve readme file  --  thanks Cies Breijs for PR #30
* Deprecate Play1-style static methods `Controller.forbidden` 

## 1.8.8 (released 30.06.2021)
* upgrade dependencies: Eclipse compiler 3.26.0, Groovy 3.0.8, hibernate 5.5.2, Hikari CP 4.0.3 etc.

## 1.8.7 (released 22.06.2021)
* restore the lost dependency "shaniparser 1.4.22" (disappeared after shutting down JCentral repository)

## 1.8.6 (released 13.01.2021)
* load liquibase*.xsd files from classpath, not from network

## 1.7 (released 30.06.2020)
* ...
