# Changelog

## 1.9.1 (released 06.01.2022)
* #36 fix annotation-based validation

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
