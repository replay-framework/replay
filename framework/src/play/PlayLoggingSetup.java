package play;

import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.xml.DOMConfigurator;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.net.URL;

public class PlayLoggingSetup {
  public void init() {
    String log4jPath = Play.configuration.getProperty("application.log.path", "/log4j.xml");
    URL log4jConf = PlayLoggingSetup.class.getResource(log4jPath);
    if (log4jConf == null) {
      log4jPath = Play.configuration.getProperty("application.log.path", "/log4j.properties");
      log4jConf = PlayLoggingSetup.class.getResource(log4jPath);
    }
    if (log4jConf == null) {
      throw new RuntimeException("File " + log4jPath + " not found");
    }

    LoggerFactory.getLogger(PlayLoggingSetup.class).info("Logging configuration: {}", log4jConf);

    if (log4jPath.endsWith(".xml")) {
      DOMConfigurator.configure(log4jConf);
    }
    else {
      PropertyConfigurator.configure(log4jConf);
    }

    SLF4JBridgeHandler.removeHandlersForRootLogger();
    SLF4JBridgeHandler.install();
  }
}
