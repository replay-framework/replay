package play.server;

import play.Play;

public class Server extends play.server.javanet.Server {

  public Server(Play play) {
    super(play);
  }

  public Server(Play play, int port) {
    super(play, port);
  }
}
