package criminals;

import static play.PropertiesConfLoader.read;

import java.util.Properties;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.CheckReturnValue;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import play.modules.guice.GuiceBeanSource;
import play.server.Starter;

@NullMarked
@CheckReturnValue
public class Application {
  @CanIgnoreReturnValue
  public int start(String playId) {
    return start(playId, null);
  }

  @CanIgnoreReturnValue
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
