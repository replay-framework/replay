package play.server;

import play.Play;

public class Server {

  @Deprecated
  public static int httpPort;

  public Server(Play play) {
  }

  public Server(Play play, int port) {
  }

  public int start() {
    throw new IllegalStateException(
      "Please add the dependency com.codeborne.replay:javanet:netty3, com.codeborne.replay:netty4 or "
      + "com.codeborne.replay:javanet to your project, and make sure it is declared before "
      + "com.codeborne.replay:framework");
  }
}
