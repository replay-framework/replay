package criminals;

import play.Play;
import play.PropertiesConfLoader;
import play.modules.guice.GuiceBeanSource;
import play.mvc.CookieSessionStore;
import play.server.Server;

import java.util.Properties;

import static java.util.Collections.singletonList;

public class Application {
  public void start(String playId) {
    PropertiesConfLoader configurationLoader = new PropertiesConfLoader();
    Properties configuration = configurationLoader.readConfiguration(playId);
    GuiceBeanSource guice = new GuiceBeanSource(singletonList(new Module(configuration)));
    Play play = new Play(configurationLoader, guice, new CookieSessionStore());
    play.init(playId);
    play.start();
    new Server(play).start();
  }

  public static void main(String[] args) {
    new Application().start(System.getProperty("play.id", ""));
  }
}
