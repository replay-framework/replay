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
signing.keyId=********
signing.password=***********************
signing.secretKeyRingFile=/home/username/.gnupg/secring.gpg
sonatypeUsername=*******
sonatypePassword=********************
```

If you have no GnuPG singing key yet, there is a guide for setting up GnuPG on [the Sonatype website](https://central.sonatype.org/publish/requirements/gpg).

Once you have a signing key setup you need to fill in `signing.keyId` with the short key found after the encryption algorithm (or simply the last 8 characters of the long key format) shown with:

```sh
gpg --list-keys --keyid-format short
```

The `signing.password` should be set to the passphrase use to access GnuPG. You can test you passphrase with:

```sh
echo "1234" | gpg2 --batch --passphrase-fd 1 --local-user <SHORT KEY ID> -as - > /dev/null && echo "Passphrase correct"
```

The `signing.secretKeyRingFile` should point to *your* keyring file (in your `$HOME` folder).
Since `gnupg` v2.1 it is not create by default, generate it with:

```sh
gpg --export-secret-keys -o /home/username/.gnupg/secring.gpg
```

And the Sonatype creds you should have gotten by registering [here](https://central.sonatype.org).


Steps to release version, for exmaple, version `2.4.0`:

1. Create a release branch, e.g. `release/2.4.0`
2. Merge the branches that you want to be part of this release
3. Fill/edit the `CHANGELOG.md`
4. Replace previous version by "2.4.0" in `build.gradle`
5. Commit & push (CHANGELOG.md + build.gradle + and all changes)
6. Run `./release.sh 2.4.0`  This runs the tests, sets+pushes a `git tag`, and uploads the `*.jar` files to [oss.sonatype.org](https://oss.sonatype.org)
7. Login to https://oss.sonatype.org/#stagingRepositories
   * Click "Close", wait until "release" button gets enabled (~1-2 minutes)
   * Click "Release" (no need to fill description)
   * After ~5 minutes, the new jar will be available in Central Maven repo
8. Merge the just created release branch into the `main` branch
9. Open https://github.com/replay-framework/replay/milestones -> 2.4.0 -> "Edit milestone" -> "Close milestone"
10. Open https://github.com/replay-framework/replay/releases -> "Draft a new release"
   * Fill the release details (copy-paste from `CHANGELOG.md`)
   * Click "Publish release"
10. Replace the version in `build.gradle` with the next-up version with a `-SNAPSHOT` suffix (e.g.: "2.5.0-SNAPSHOT") and commit
11. Create a new release on github: https://github.com/replay-framework/replay/releases
12. Close milestone 2.4.0 and create a new milestone 2.5.0: https://github.com/replay-framework/replay/milestones

This uploads the `*.jar` files to: https://s01.oss.sonatype.org/#stagingRepositories
