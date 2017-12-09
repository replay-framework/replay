package play;
import org.apache.log4j.Level;
import org.junit.*;

import java.io.File;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

public class LoggerTest {
    private static Properties playConfig;
    private static File applicationPath;
    private static String id;

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
        Play.id = id ;
        if (Play.id != null && Play.configuration != null) {
                Logger.init();
        }
    }
    
    @Before
    public void setUp() {
        Play.configuration = new Properties();
        Play.applicationPath = new File(".");
        Play.id="test";   
    }

    @Test
    public void init_with_properties() {
        Play.configuration.setProperty("application.log.path", "/play/testlog4j.properties");
        Logger.init();
        org.apache.log4j.Logger log4jLogger = org.apache.log4j.Logger.getLogger("logtest.properties");
        assertEquals(Level.ERROR,  log4jLogger.getLevel());
    }

    @Test
    public void init_with_xml() {
        Play.configuration.setProperty("application.log.path", "/play/testlog4j.xml");
        Logger.init();
        org.apache.log4j.Logger log4jLogger = org.apache.log4j.Logger.getLogger("logtest.xml");
        assertEquals(Level.ERROR,  log4jLogger.getLevel());
    }
}
