package criminals;

import static play.PropertiesConfLoader.read;

import java.util.Properties;
import javax.annotation.Nullable;
import play.modules.guice.GuiceBeanSource;
import play.server.Starter;

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
    return Starter.start(playId, guice);
  }

  public static void main(String[] args) {
    new Application().start(System.getProperty("play.id", ""));
  }
}
