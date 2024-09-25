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

The `signing.password` should be set to the passphrase use to access GnuPG. You can test your passphrase with:

```sh
echo "1234" | gpg2 --batch --passphrase-fd 1 --local-user <SHORT KEY ID> -as - > /dev/null && echo "Passphrase correct"
```

The `signing.secretKeyRingFile` should point to *your* keyring file (in your `$HOME` folder).
Since `gnupg` v2.1 it is not create by default, generate it with:

```sh
gpg --export-secret-keys -o /home/username/.gnupg/secring.gpg
```

And the Sonatype credentials `sonatypeUsername` and `sonatypePassword` need to contain the "user token".
You need to generate this token by logging in to `s01.oss.sonatype.org` > click on your username in the top-right corner > click `Profile` > select `User Token` in the drop down.
Important is that the account you are using has permissions for `io/github/replay-framework` (this needs to be granted by raising a support ticket with Sonatype).


Steps to release version, for example, version `X.Y.Z`:

1. Create a release branch, e.g. `release/X.Y.Z`
2. Merge the branches that you want to be part of this release
3. Fill/edit the `CHANGELOG.md`
4. Replace previous version by "X.Y.Z" in `build.gradle`
5. Commit & push (CHANGELOG.md + build.gradle + and all changes)
6. Run `./release.sh X.Y.Z`  This runs the tests, sets+pushes a `git tag`, and uploads the `*.jar` files to [s01.oss.sonatype.org](https://s01.oss.sonatype.org)
7. Login to https://s01.oss.sonatype.org/#stagingRepositories and locate the staging repository...
   * Wait until the "Close" button  gets enabled (~1 minute, "Refresh" may help)
   * Click "Close" (no need to fill description)
   * Wait until "Release" button gets enabled (~3 minutes, "Refresh" may help)
   * Click "Release" (no need to fill description)
   * After ~5-10 minutes, the new jar will be available in Central Maven repo (it may take up to 2 hours until it shows up in [search.maven.org](https://search.maven.org))
   * In more detail this is explained here: https://central.sonatype.org/publish/release
8. Merge the just created release branch into the `main` branch
9. Open https://github.com/replay-framework/replay/milestones -> X.Y.Z -> "Edit milestone" -> "Close milestone"
10. Open https://github.com/replay-framework/replay/releases -> "Draft a new release"
   * Fill the release details (copy-paste from `CHANGELOG.md`)
   * Click "Publish release"
10. Replace the version in `build.gradle` with the next-up version with a `-SNAPSHOT` suffix (e.g.: "A.B.C-SNAPSHOT", where A/B/C are the next in line of X/Y/Z) and commit
11. Create a new release on github -- https://github.com/replay-framework/replay/releases -- and save as "Draft"
12. Close milestone X.Y.Z and create a new milestone A.B.C: https://github.com/replay-framework/replay/milestones


