package criminals;

import play.Play;
import play.modules.guice.GuiceBeanSource;
import play.server.Server;

import javax.annotation.Nullable;
import java.util.Properties;

import static java.util.Collections.singletonList;
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
    Play play = new Play(guice);
    play.init(playId);
    play.start();
    return new Server(play).start();
  }

  public static void main(String[] args) {
    new Application().start(System.getProperty("play.id", ""));
  }
}
