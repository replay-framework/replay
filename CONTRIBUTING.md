Any contributions to RePlay are both welcomed and appreciated.

## Branches

- The latest state is always in `main` branch
- Every bugfix/feature is developed in a separate branch created from the `main` branch
- Once a bugfix/feature is accepted, it gets merged to `main` branch


## How to build

    ./gradlew check uitest publishToMavenLocal

This puts the `*.jar` files in your local maven repository at: `~/.m2/repository/io/github/replay-framework`

You can also customize parameters of UI tests (browser, headless, timeout etc.):

     ./gradlew uitest -Dselenide.browser=firefox -Dselenide.headless=true

## Network implementations

By default, RePlay uses Netty3 for network communication. 
There is alternative implementation on Netty4, but it's under development now. 

To run tests only on Netty3:

     ./gradlew test uitest-netty3

To run tests only on Netty4:

     ./gradlew test uitest-netty4

To run tests only on Javanet:

     ./gradlew test uitest-javanet

## How to release

To make a release, you need to:
1. Have a write permission to `io.github.replay-framework` group in Maven central repository.
2. Have these lines in `~/.gradle/gradle.properties`:

```
signing.keyId=2#####8
signing.password=***********************
signing.secretKeyRingFile=/path/to/.gnupg/secring.gpg
sonatypeUsername=*******
sonatypePassword=********************
```

A guide for setting up GPG for can be found on [the Sonatype website](https://central.sonatype.org/publish/requirements/gpg).

Steps to release version 2.4.0 (for example):
1. Fill the CHANGELOG.md
2. Replace previous version by "2.4.0" in build.gradle
3. Commit & push (CHANGELOG.md + build.gradle + maybe something else)
4. Run ./release.sh 2.4.0  // This uploads the `*.jar` files to https://oss.sonatype.org
5. Login to https://oss.sonatype.org/#stagingRepositories
   * Click "Close", wait until "release" button gets enabled (~1-2 minutes)
   * Click "Release" (no need to fill description)
   * After ~5 minutes, the new jar will be available in Central Maven repo
6. Open https://github.com/replay-framework/replay/milestones -> 2.4.0 -> "Edit milestone" -> "Close milestone"
7. Open https://github.com/replay-framework/replay/releases -> "Draft a new release"
   * Fill the release details (copy-paste from CHANGELOG.md)
   * Click "Publish release"
8. Replace version in build.gradle by "2.5.0-SNAPSHOT" and commit
9. Create a new release on github: https://github.com/replay-framework/replay/releases
10. Close milestone 2.4.0 and create a new milestone 2.5.0: https://github.com/replay-framework/replay/milestones

This uploads the `*.jar` files to: https://s01.oss.sonatype.org/#stagingRepositories
