package app;

import play.modules.guice.GuiceBeanSource;
import play.server.Starter;

public class LiquiBaseApp {
  public static void main(String[] args) {
    int port = run("prod");
    System.out.println("Try: http://localhost:" + port);
    System.out.println();
  }

  public static int run(String playId) {
    GuiceBeanSource guice = new GuiceBeanSource(new PetModule());
    return Starter.start(playId, guice);
  }
}
