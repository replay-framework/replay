package play;

import org.slf4j.LoggerFactory;
import play.libs.IO;
import play.utils.OrderSafeProperties;
import play.vfs.VirtualFile;

import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConfLoader {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(ConfLoader.class);
    private final Pattern overrideKeyPattern = Pattern.compile("^%([a-zA-Z0-9_\\-]+)\\.(.*)$");

    public Properties readOneConfigurationFile(String filename) {
        return readOneConfigurationFile(filename, null, new HashSet<>());
    }

    private Properties readOneConfigurationFile(String filename, String inheritedId, Set<VirtualFile> confs) {
        VirtualFile conf = VirtualFile.open(Play.applicationPath + "/conf/" + filename);
        if (confs.contains(conf)) {
            throw new RuntimeException("Detected recursive @include usage. Have seen the file " + filename + " before");
        }

        Properties propsFromFile = IO.readUtf8Properties(conf.inputstream());
        confs.add(conf);

        if (inheritedId == null) {
            inheritedId = propsFromFile.getProperty("%" + Play.id);
            if (inheritedId != null && inheritedId.startsWith("%")) inheritedId = inheritedId.substring(1);
        }
        propsFromFile = resolvePlayIdOverrides(propsFromFile, inheritedId);

        resolveIncludes(propsFromFile, inheritedId, confs);

        return propsFromFile;
    }

    Properties resolvePlayIdOverrides(Properties propsFromFile, String inheritedId) {
        Properties newConfiguration = new OrderSafeProperties();

        for (String name : propsFromFile.stringPropertyNames()) {
            Matcher matcher = overrideKeyPattern.matcher(name);
            if (!matcher.matches()) {
                newConfiguration.setProperty(name, propsFromFile.getProperty(name).trim());
            }
        }

        overrideMatching(inheritedId, propsFromFile, newConfiguration);
        overrideMatching(Play.id, propsFromFile, newConfiguration);
        return newConfiguration;
    }

    private void overrideMatching(String inheritedId, Properties propsFromFile, Properties newConfiguration) {
        for (String name : propsFromFile.stringPropertyNames()) {
            Matcher matcher = overrideKeyPattern.matcher(name);
            if (matcher.matches()) {
                String instance = matcher.group(1);
                if (instance.equals(Play.id) || instance.equals(inheritedId)) {
                    newConfiguration.setProperty(matcher.group(2), propsFromFile.getProperty(name).trim());
                }
            }
        }
    }

    private void resolveIncludes(Properties propsFromFile, String inheritedId, Set<VirtualFile> confs) {
        for (Map.Entry<Object, Object> e : propsFromFile.entrySet()) {
            if (e.getKey().toString().startsWith("@include.")) {
                try {
                    String filenameToInclude = e.getValue().toString();
                    propsFromFile.putAll(readOneConfigurationFile(filenameToInclude, inheritedId, confs));
                } catch (Exception ex) {
                    logger.warn("Missing include: {}", e.getKey(), ex);
                }
            }
        }
    }
}
