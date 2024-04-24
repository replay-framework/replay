package hello;

import play.server.Starter;

public class HelloWorldApp {
  public int start(String playId) {
    return Starter.start(playId);
  }
  public static void main(String[] args) {
    new HelloWorldApp().start(System.getProperty("play.id", "prod"));
  }
}
