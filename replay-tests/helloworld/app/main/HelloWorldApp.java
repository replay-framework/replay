package main;

import play.PropertiesConfLoader;
import play.TemplateErrorHandler;
import play.SimpleErrorHandler;
import play.inject.DefaultBeanSource;
import play.mvc.CookieSessionStore;
import play.server.Starter;

public class HelloWorldApp {

  public static void main(String[] args) {
    int port = startWithTemplateErrorHandler("prod");
    System.out.println("Try: http://localhost:" + port);
    System.out.println("Try: http://localhost:" + port + "/public/hello_world.txt");
    System.out.println();
  }

  public static int startWithTemplateErrorHandler(String playId) {
    return Starter.start(playId, new PropertiesConfLoader(), new DefaultBeanSource(),
        new CookieSessionStore(), new TemplateErrorHandler());
  }

  public static int startWithTextErrorHandler(String playId) {
    return Starter.start(playId, new PropertiesConfLoader(), new DefaultBeanSource(),
        new CookieSessionStore(), new SimpleErrorHandler());
  }
}
