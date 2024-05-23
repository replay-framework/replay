package criminals;

import play.PropertiesConfLoader;
import play.TemplateErrorHandler;
import play.modules.guice.GuiceBeanSource;
import play.mvc.CookieSessionStore;
import play.server.Starter;

import javax.annotation.Nullable;
import java.util.Properties;

import static play.PropertiesConfLoader.read;

public class Application {
  public void start(String playId) {
    start(playId, null);
  }

  public int start(String playId, @Nullable String criminalRecordsServiceUrl) {
    Properties configuration = read(playId);
    if (criminalRecordsServiceUrl != null) {
      configuration.setProperty("criminal-records.service.url", criminalRecordsServiceUrl);
    }
    GuiceBeanSource guice = new GuiceBeanSource(new Module(configuration));
    return Starter.start(playId, new PropertiesConfLoader(), guice, new CookieSessionStore(), new TemplateErrorHandler());
  }

  public static void main(String[] args) {
    new Application().start(System.getProperty("play.id", ""));
  }
}
