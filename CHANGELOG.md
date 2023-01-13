# Changelog

## 2.0.0 (planned to 2x.01.2023)
* added experimental support for Netty4 (Netty 3 is also still used by default)  --  thanks to Szabolcs Hubai! (#25) (#95)

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
