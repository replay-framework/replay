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
    throw new IllegalStateException("Please add dependency replay-netty3, replay-netty4 or replay-javanet to your project");
  }
}
