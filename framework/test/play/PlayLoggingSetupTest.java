package play;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

public class PlayLoggingSetupTest {
  private static Properties playConfig;
  private static File applicationPath;
  private static String id;
  private static final PlayLoggingSetup loggingSetup = new PlayLoggingSetup();

  @BeforeClass
  public static void rememberOriginalConfigurationAndLog() {
    playConfig = Play.configuration;
    applicationPath = Play.applicationPath;
    id = Play.id;
  }

  @AfterClass
  public static void restoreOriginalConfigurationAndLog() {
    Play.configuration = playConfig;
    Play.applicationPath = applicationPath;
    Play.id = id;
    if (Play.id != null && Play.configuration != null) {
      loggingSetup.init();
    }
  }

  @Before
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
    assertEquals(Level.ERROR, log4jLogger.getLevel());
  }

  @Test
  public void init_with_xml() {
    Play.configuration.setProperty("application.log.path", "/play/testlog4j.xml");
    loggingSetup.init();
    Logger log4jLogger = Logger.getLogger("logtest.xml");
    assertEquals(Level.ERROR, log4jLogger.getLevel());
  }
}
