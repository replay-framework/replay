# Criminals

This is an example of a [RePlay](https://github.com/replay-framework/replay) project.

It may be used to understand the differences between a project based on [Play1](https://github.com/playframework/play1) and a project based on RePlay.

It also serves as a means to test RePlay.


## Running

Since all projects in the `replay-test` folder are parameterized over the server backends (e.g.: `netty3`, `netty4` or `javanet`) you need to
uncomment the `implementation` line in `replay-tests/replay-tests.gradle`:

```java
dependencies {
  // To run the app locally:
  // implementation project(':netty3')

  // ...
}
```

You need to add the `-parameters` flag to the `javac` configuration in:
`Settings` > `Build, Execution, Deployment` > `Compiler` > `Java Compiler` > `Additional command line parameters`.
Then rebuild your project with: `Build` > `Rebuild Project`.

After that you can run the project from IntelliJ with the pre-configured `Criminals` Run Configuration.

To log in the password should be identical the email address provided in the login page.
You are then asked to provide an OTP, the OTP is found output of the application
after submitting your username and password.

