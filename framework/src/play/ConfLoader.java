package play;

import org.slf4j.LoggerFactory;
import play.libs.IO;
import play.utils.OrderSafeProperties;
import play.vfs.VirtualFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConfLoader {
  private org.slf4j.Logger logger = LoggerFactory.getLogger(getClass());

  public Properties readOneConfigurationFile(String filename) {
      Properties propsFromFile = null;

      VirtualFile appRoot = VirtualFile.open(Play.applicationPath);

      VirtualFile conf = appRoot.child("conf/" + filename);
      if (Play.confs.contains(conf)) {
          throw new RuntimeException("Detected recursive @include usage. Have seen the file " + filename + " before");
      }

      try {
          propsFromFile = IO.readUtf8Properties(conf.inputstream());
      } catch (RuntimeException e) {
          if (e.getCause() instanceof IOException) {
              logger.error("Cannot read {}", filename);
              Play.fatalServerErrorOccurred();
          }
      }
      Play.confs.add(conf);

      // OK, check for instance specifics configuration
      Properties newConfiguration = new OrderSafeProperties();
      Pattern pattern = Pattern.compile("^%([a-zA-Z0-9_\\-]+)\\.(.*)$");
      for (Object key : propsFromFile.keySet()) {
          Matcher matcher = pattern.matcher(key + "");
          if (!matcher.matches()) {
              newConfiguration.put(key, propsFromFile.get(key).toString().trim());
          }
      }
      for (Object key : propsFromFile.keySet()) {
          Matcher matcher = pattern.matcher(key + "");
          if (matcher.matches()) {
              String instance = matcher.group(1);
              if (instance.equals(Play.id)) {
                  newConfiguration.put(matcher.group(2), propsFromFile.get(key).toString().trim());
              }
          }
      }
      propsFromFile = newConfiguration;
      // Resolve ${..}
      pattern = Pattern.compile("\\$\\{([^}]+)}");
      for (Object key : propsFromFile.keySet()) {
          String value = propsFromFile.getProperty(key.toString());
          Matcher matcher = pattern.matcher(value);
          StringBuffer newValue = new StringBuffer(100);
          while (matcher.find()) {
              String jp = matcher.group(1);
              String r = System.getProperty(jp);
              if (r == null) {
                  r = System.getenv(jp);
              }
              if (r == null) {
                  logger.warn("Cannot replace {} in configuration ({}={})", jp, key, value);
                  continue;
              }
              matcher.appendReplacement(newValue, r.replaceAll("\\\\", "\\\\\\\\"));
          }
          matcher.appendTail(newValue);
          propsFromFile.setProperty(key.toString(), newValue.toString());
      }
      // Include
      Map<Object, Object> toInclude = new HashMap<>(16);
      for (Object key : propsFromFile.keySet()) {
          if (key.toString().startsWith("@include.")) {
              try {
                  String filenameToInclude = propsFromFile.getProperty(key.toString());
                  toInclude.putAll(readOneConfigurationFile(filenameToInclude));
              } catch (Exception ex) {
                  logger.warn("Missing include: {}", key, ex);
              }
          }
      }
      propsFromFile.putAll(toInclude);

      return propsFromFile;
  }

  static void extractHttpPort() {
      String javaCommand = System.getProperty("sun.java.command", "");
      jregex.Matcher m = new jregex.Pattern(".* --http.port=({port}\\d+)").matcher(javaCommand);
      if (m.matches()) {
          Play.configuration.setProperty("http.port", m.group("port"));
      }
  }
}
