package play;

import org.slf4j.LoggerFactory;
import play.libs.IO;
import play.utils.OrderSafeProperties;
import play.vfs.VirtualFile;

import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConfLoader {
    private org.slf4j.Logger logger = LoggerFactory.getLogger(getClass());
    private final Pattern overrideKeyPattern = Pattern.compile("^%([a-zA-Z0-9_\\-]+)\\.(.*)$");
    private final Pattern variablePattern = Pattern.compile("\\$\\{([^}]+)}");

    public Properties readOneConfigurationFile(String filename) {
        return readOneConfigurationFile(filename, null);
    }

    protected Properties readOneConfigurationFile(String filename, String inheritedId) {
        VirtualFile conf = VirtualFile.open(Play.applicationPath + "/conf/" + filename);
        if (Play.confs.contains(conf)) {
            throw new RuntimeException("Detected recursive @include usage. Have seen the file " + filename + " before");
        }

        Properties propsFromFile = IO.readUtf8Properties(conf.inputstream());
        Play.confs.add(conf);

        resolveVariables(propsFromFile);

        if (inheritedId == null) {
            inheritedId = propsFromFile.getProperty("%" + Play.id);
            if (inheritedId != null && inheritedId.startsWith("%")) inheritedId = inheritedId.substring(1);
        }
        propsFromFile = resolvePlayIdOverrides(propsFromFile, inheritedId);

        resolveIncludes(propsFromFile, inheritedId);

        return propsFromFile;
    }

    protected Properties resolvePlayIdOverrides(Properties propsFromFile, String inheritedId) {
        Properties newConfiguration = new OrderSafeProperties();

        for (Map.Entry<Object, Object> e : propsFromFile.entrySet()) {
            Matcher matcher = overrideKeyPattern.matcher((e.getKey()).toString());
            if (!matcher.matches()) {
                newConfiguration.put(e.getKey(), e.getValue().toString().trim());
            }
        }

        overrideMatching(inheritedId, propsFromFile, newConfiguration);
        overrideMatching(Play.id, propsFromFile, newConfiguration);
        return newConfiguration;
    }

    private void overrideMatching(String inheritedId, Properties propsFromFile, Properties newConfiguration) {
        for (Map.Entry<Object, Object> e : propsFromFile.entrySet()) {
            Matcher matcher = overrideKeyPattern.matcher(e.getKey().toString());
            if (matcher.matches()) {
                String instance = matcher.group(1);
                if (instance.equals(Play.id) || instance.equals(inheritedId)) {
                    newConfiguration.put(matcher.group(2), e.getValue().toString().trim());
                }
            }
        }
    }

    protected void resolveVariables(Properties propsFromFile) {
        for (Map.Entry<Object, Object> e : propsFromFile.entrySet()) {
            String value = e.getValue().toString();
            Matcher matcher = variablePattern.matcher(value);
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

    protected void resolveIncludes(Properties propsFromFile, String inheritedId) {
        for (Map.Entry<Object, Object> e : propsFromFile.entrySet()) {
            if (e.getKey().toString().startsWith("@include.")) {
                try {
                    String filenameToInclude = e.getValue().toString();
                    propsFromFile.putAll(readOneConfigurationFile(filenameToInclude, inheritedId));
                } catch (Exception ex) {
                    logger.warn("Missing include: {}", e.getKey(), ex);
                }
            }
        }
    }

    void extractHttpPort() {
        String javaCommand = System.getProperty("sun.java.command", "");
        extractHttpPort(javaCommand).ifPresent((port) -> {
            Play.configuration.setProperty("http.port", port);
        });
    }

    Optional<String> extractHttpPort(String javaCommand) {
        Matcher m = Pattern.compile(".* --http.port=(\\d+)").matcher(javaCommand);
        return m.matches() ? Optional.of(m.group(1)) : Optional.empty();
    }
}
