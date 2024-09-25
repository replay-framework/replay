package hello;

import play.ClasspathResource;
import play.Play;
import play.PropertiesConfLoader;
import play.inject.DefaultBeanSource;
import play.mvc.CookieSessionStore;
import play.server.Starter;

public class HelloWorldApp {
  public int start(String playId) {
    Play play = new Play(new PropertiesConfLoader("conf/"), new DefaultBeanSource(), new CookieSessionStore());
    play.minimalInit(playId);
    Play.routes = ClasspathResource.file("conf/routes");
    Play.pluginCollection.loadPlugins();
    play.start();
    return Starter.start(play);
  }
  public static void main(String[] args) {
    new HelloWorldApp().start(System.getProperty("play.id", "prod"));
  }
}
