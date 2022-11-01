Any contributions to RePlay are both welcomed and appreciated.

## Branches

- The latest state is always in `master` branch
- Every bugfix/feature is developed in a separate branch created from the `master` branch
- Once a bugfix/feature is accepted, it gets merged to `master` branch


## How to build

    ./gradlew check uitest publishToMavenLocal

This puts the `*.jar` files in your local maven repository at: `~/.m2/repository/com/codeborne/replay`

You can also customize parameters of UI tests (browser, headless, timeout etc.):

     ./gradlew uitest -Dselenide.browser=firefox -Dselenide.headless=true

## How to release

This requires write permission to `com.codeborne` group in Maven central repository:

    ./release.sh

This uploads the `*.jar` files to: https://oss.sonatype.org/#stagingRepositories

