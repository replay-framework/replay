package play;

import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.xml.DOMConfigurator;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Main logger of the application.
 * Free to use from the application code.
 */
public class Logger {
    /**
     * The application logger (play).
     */
    public static org.apache.log4j.Logger log4j;

    /**
     * true if logger is configured manually (log4j-config file supplied by application)
     */
    public static boolean configuredManually = false;

    /**
     * Try to init stuff.
     */
    public static void init() {
        String log4jPath = Play.configuration.getProperty("application.log.path", "/log4j.xml");
        URL log4jConf = Logger.class.getResource(log4jPath);
        boolean isXMLConfig = log4jPath.endsWith(".xml");
        if (log4jConf == null) { // try again with the .properties
            isXMLConfig = false;
            log4jPath = Play.configuration.getProperty("application.log.path", "/log4j.properties");
            log4jConf = Logger.class.getResource(log4jPath);
        }
        if (log4jConf == null) {
            Properties shutUp = new Properties();
            shutUp.setProperty("log4j.rootLogger", "OFF");
            PropertyConfigurator.configure(shutUp);
        } else if (Logger.log4j == null) {

            try {
                if (Paths.get(log4jConf.toURI()).startsWith(Play.applicationPath.toPath())) {
                    configuredManually = true;
                }
            } catch (IllegalArgumentException | FileSystemNotFoundException | SecurityException | URISyntaxException e) {
            }
            if (isXMLConfig) {
                DOMConfigurator.configure(log4jConf);
            } else {
                PropertyConfigurator.configure(log4jConf);
            }
            Logger.log4j = org.apache.log4j.Logger.getLogger("play");
        }
    }

    /**
     * Force logger to a new level.
     * @param level TRACE,DEBUG,INFO,WARN,ERROR,FATAL
     */
    public static void setUp(String level) {
        Logger.log4j.setLevel(org.apache.log4j.Level.toLevel(level));
    }
}
