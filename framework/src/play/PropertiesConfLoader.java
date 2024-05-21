package play;

import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.utils.OrderSafeProperties;

import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PropertiesConfLoader implements ConfLoader {
  private static final Logger logger = LoggerFactory.getLogger(PropertiesConfLoader.class);
  private final Pattern overrideKeyPattern = Pattern.compile("^%([a-zA-Z0-9_\\-]+)\\.(.*)$");
  private final Pattern envVarInterpolationPattern = Pattern.compile("\\$\\{([^}]+)}");

  public static Properties read(String playId) {
    return new PropertiesConfLoader().readConfiguration(playId);
  }

  @Override
  public Properties readConfiguration(String playId) {
    return readOneConfigurationFile(playId, "application.conf");
  }

  public Properties readOneConfigurationFile(String playId, String filename) {
    return readOneConfigurationFile(filename, playId, null, new HashSet<>());
  }

  private Properties readOneConfigurationFile(String filename, String playId, String inheritedId, Set<String> confs) {
    ClasspathResource conf = ClasspathResource.file(filename);
    if (confs.contains(conf.url().toExternalForm())) {
      throw new RuntimeException("Detected recursive @include usage. Have seen the file " + filename + " before");
    }

    confs.add(conf.url().toExternalForm());
    Properties propsFromFile = conf.toProperties();

    if (inheritedId == null) {
      inheritedId = propsFromFile.getProperty("%" + playId);
      if (inheritedId != null && inheritedId.startsWith("%")) {
        inheritedId = inheritedId.substring(1);
      }
    }
    propsFromFile = resolvePlayIdOverrides(propsFromFile, playId, inheritedId);

    resolveEnvironmentVariables(propsFromFile, conf);

    resolveIncludes(propsFromFile, playId, inheritedId, confs);

    return propsFromFile;
  }

  /**
   * the ${...} interpolation syntax
   */
  @VisibleForTesting
  void resolveEnvironmentVariables(Properties propsFromFile, ClasspathResource conf) {
    for (Object key : propsFromFile.keySet()) {
      String value = propsFromFile.getProperty(key.toString());
      Matcher matcher = envVarInterpolationPattern.matcher(value);
      StringBuilder newValue = new StringBuilder(100);
      while (matcher.find()) {
        String envVarKey = matcher.group(1);
        String envVarValue = getEnvVar(envVarKey);
        if (envVarValue == null) {
          logger.warn("Cannot replace {} in {} ({}={})", envVarKey, conf == null ? "null" : conf.toString(), key, value);
          continue;
        }
        matcher.appendReplacement(newValue, envVarValue.replaceAll("\\\\", "\\\\\\\\"));
      }
      matcher.appendTail(newValue);
      propsFromFile.setProperty(key.toString(), newValue.toString());
    }
  }

  @VisibleForTesting
  String getEnvVar(String envVarKey) {
    return System.getenv(envVarKey);
  }

  @VisibleForTesting
  Properties resolvePlayIdOverrides(Properties propsFromFile, String playId, String inheritedId) {
    Properties newConfiguration = new OrderSafeProperties();

    for (String name : propsFromFile.stringPropertyNames()) {
      Matcher matcher = overrideKeyPattern.matcher(name);
      if (!matcher.matches()) {
        newConfiguration.setProperty(name, propsFromFile.getProperty(name).trim());
      }
    }

    overrideMatching(playId, inheritedId, propsFromFile, newConfiguration);
    overrideMatching(playId, playId, propsFromFile, newConfiguration);
    return newConfiguration;
  }

  private void overrideMatching(String playId, String inheritedId, Properties propsFromFile, Properties newConfiguration) {
    for (String name : propsFromFile.stringPropertyNames()) {
      Matcher matcher = overrideKeyPattern.matcher(name);
      if (matcher.matches()) {
        String instance = matcher.group(1);
        if (instance.equals(playId) || instance.equals(inheritedId)) {
          newConfiguration.setProperty(matcher.group(2), propsFromFile.getProperty(name).trim());
        }
      }
    }
  }

  private void resolveIncludes(Properties propsFromFile, String playId, String inheritedId, Set<String> confs) {
    for (Map.Entry<Object, Object> e : propsFromFile.entrySet()) {
      if (e.getKey().toString().startsWith("@include.")) {
        try {
          String filenameToInclude = e.getValue().toString();
          propsFromFile.putAll(readOneConfigurationFile(filenameToInclude, playId, inheritedId, confs));
        }
        catch (Exception ex) {
          logger.warn("Missing include: {}", e.getKey(), ex);
        }
      }
    }
  }
}
