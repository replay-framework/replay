package app;

import play.Play;
import play.modules.guice.GuiceBeanSource;
import play.server.Server;

public class LiquiBaseApp {
  public static void main(String[] args) {
    int port = run("prod");
    System.out.println("Try: http://localhost:" + port);
    System.out.println();
  }
  
  public static int run(String playId) {
    GuiceBeanSource guice = new GuiceBeanSource(new PetModule());
    Play play = new Play(guice);
    play.init(playId);
    play.start();
    return new Server(play).start();
  }
}
