package play;

import org.slf4j.LoggerFactory;
import play.libs.IO;
import play.utils.OrderSafeProperties;
import play.vfs.VirtualFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.commons.lang.StringUtils.isNotEmpty;

public class ConfLoader {
    private org.slf4j.Logger logger = LoggerFactory.getLogger(getClass());

    public Properties readOneConfigurationFile(String filename) {
        VirtualFile conf = VirtualFile.open(Play.applicationPath + "/conf/" + filename);
        if (Play.confs.contains(conf)) {
            throw new RuntimeException("Detected recursive @include usage. Have seen the file " + filename + " before");
        }

        Properties propsFromFile = null;
        try {
            propsFromFile = IO.readUtf8Properties(conf.inputstream());
        } catch (RuntimeException e) {
            if (e.getCause() instanceof IOException) {
                logger.error("Cannot read {}", filename);
                Play.fatalServerErrorOccurred();
            }
        }
        Play.confs.add(conf);

        addInheritedConfKeys(propsFromFile);

        propsFromFile = resolveEnvironmentOverrides(propsFromFile);
        resolveVariables(propsFromFile);
        resolveIncludes(propsFromFile);
        return propsFromFile;
    }

    protected void addInheritedConfKeys(Properties propsFromFile) {
      String currentConf = "%" + Play.id;
      String inheritedConf = propsFromFile.getProperty(currentConf);
      if (isNotEmpty(inheritedConf)) {
        int numInherited = 0;
        for (String key : new ArrayList<>(propsFromFile.stringPropertyNames())) {
          if (key.startsWith(inheritedConf)) {
            String newKey = key.replace(inheritedConf, currentConf);
            if (!propsFromFile.containsKey(newKey)) {
              propsFromFile.setProperty(newKey, propsFromFile.getProperty(key));
              numInherited++;
            }
          }
        }
        logger.info("Inherited " + numInherited + " configuration parameters from " + inheritedConf);
      }
    }

    protected Properties resolveEnvironmentOverrides(Properties propsFromFile) {
        Properties newConfiguration = new OrderSafeProperties();
        Pattern pattern = Pattern.compile("^%([a-zA-Z0-9_\\-]+)\\.(.*)$");
        for (Map.Entry<Object, Object> e : propsFromFile.entrySet()) {
            Matcher matcher = pattern.matcher((e.getKey()).toString());
            if (!matcher.matches()) {
                newConfiguration.put(e.getKey(), e.getValue().toString().trim());
            }
        }
        for (Map.Entry<Object, Object> e : propsFromFile.entrySet()) {
            Matcher matcher = pattern.matcher(e.getKey().toString());
            if (matcher.matches()) {
                String instance = matcher.group(1);
                if (instance.equals(Play.id)) {
                    newConfiguration.put(matcher.group(2), e.getValue().toString().trim());
                }
            }
        }
        return newConfiguration;
    }

    protected void resolveVariables(Properties propsFromFile) {
        Pattern pattern = Pattern.compile("\\$\\{([^}]+)}");
        for (Map.Entry<Object, Object> e : propsFromFile.entrySet()) {
            String value = e.getValue().toString();
            Matcher matcher = pattern.matcher(value);
            StringBuffer newValue = new StringBuffer(100);
            while (matcher.find()) {
                String jp = matcher.group(1);
                String r = System.getProperty(jp);
                if (r == null) {
                    r = System.getenv(jp);
                }
                if (r == null) {
                    logger.warn("Cannot replace {} in configuration ({}={})", jp, e.getKey(), value);
                    continue;
                }
                matcher.appendReplacement(newValue, r.replaceAll("\\\\", "\\\\\\\\"));
            }
            matcher.appendTail(newValue);
            propsFromFile.setProperty(e.getKey().toString(), newValue.toString());
        }
    }

    protected void resolveIncludes(Properties propsFromFile) {
        Map<Object, Object> toInclude = new HashMap<>(16);
        for (Map.Entry<Object, Object> e : propsFromFile.entrySet()) {
            if (e.getKey().toString().startsWith("@include.")) {
                try {
                    String filenameToInclude = e.getValue().toString();
                    toInclude.putAll(readOneConfigurationFile(filenameToInclude));
                } catch (Exception ex) {
                    logger.warn("Missing include: {}", e.getKey(), ex);
                }
            }
        }
        propsFromFile.putAll(toInclude);
    }

    void extractHttpPort() {
        String javaCommand = System.getProperty("sun.java.command", "");
        jregex.Matcher m = new jregex.Pattern(".* --http.port=({port}\\d+)").matcher(javaCommand);
        if (m.matches()) {
            Play.configuration.setProperty("http.port", m.group("port"));
        }
    }
}
