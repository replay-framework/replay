# Changelog

## 1.9.5 (released 18.07.2022)
* #50 Make PdfHelper methods public  --  see PR #51

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
