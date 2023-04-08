Any contributions to RePlay are both welcomed and appreciated.

## Branches

- The latest state is always in `main` branch
- Every bugfix/feature is developed in a separate branch created from the `main` branch
- Once a bugfix/feature is accepted, it gets merged to `main` branch


## How to build

    ./gradlew check uitest publishToMavenLocal

This puts the `*.jar` files in your local maven repository at: `~/.m2/repository/com/codeborne/replay`

You can also customize parameters of UI tests (browser, headless, timeout etc.):

     ./gradlew uitest -Dselenide.browser=firefox -Dselenide.headless=true

## Network implementations

By default, RePlay uses Netty3 for network communication. 
There is alternative implementation on Netty4, but it's under development now. 

To run tests on Netty4:

     ./gradlew test uitest -Pserver=netty4

## How to release

This requires write permission to `com.codeborne` group in Maven central repository:

    ./release.sh

This uploads the `*.jar` files to: https://oss.sonatype.org/#stagingRepositories

