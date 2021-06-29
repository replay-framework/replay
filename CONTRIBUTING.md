Any contributions to RePlay are both welcomed and appreciated.

## Branches
- The latest state is always in `master` branch
- Every bugfix/feature is developed in a se
  parate branches created from `master` branch
- Once bugfix/feature is accepted, it gets merged to `master` branch


## How to build

- `./gradlew check uitest install`

As a result, you'll get *.jars files in local maven repository at `~/.m2/repository/com/codeborne/replay`.


## How to release

it can be executed only if you have write permission to `com.codeborne` group in Maven central repository:

- `./release.sh`

As a result, you'll find *.jars uploaded to https://oss.sonatype.org/#stagingRepositories

