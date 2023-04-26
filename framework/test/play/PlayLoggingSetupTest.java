package play;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

public class PlayLoggingSetupTest {
  private static Properties playConfig;
  private static File applicationPath;
  private static String id;
  private static final PlayLoggingSetup loggingSetup = new PlayLoggingSetup();

  @BeforeAll
  public static void rememberOriginalConfigurationAndLog() {
    playConfig = Play.configuration;
    applicationPath = Play.applicationPath;
    id = Play.id;
  }

  @AfterAll
  public static void restoreOriginalConfigurationAndLog() {
    Play.configuration = playConfig;
    Play.applicationPath = applicationPath;
    Play.id = id;
    if (Play.id != null && Play.configuration != null) {
      loggingSetup.init();
    }
  }

  @BeforeEach
  public void setUp() {
    Play.configuration = new Properties();
    Play.applicationPath = new File(".");
    Play.id = "test";
  }

  @Test
  public void init_with_properties() {
    Play.configuration.setProperty("application.log.path", "/play/testlog4j.properties");
    loggingSetup.init();
    Logger log4jLogger = Logger.getLogger("logtest.properties");
    assertThat(log4jLogger.getLevel()).isEqualTo(Level.ERROR);
  }

  @Test
  public void init_with_xml() {
    Play.configuration.setProperty("application.log.path", "/play/testlog4j.xml");
    loggingSetup.init();
    Logger log4jLogger = Logger.getLogger("logtest.xml");
    assertThat(log4jLogger.getLevel()).isEqualTo(Level.ERROR);
  }
}
