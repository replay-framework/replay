package play.db;

import play.Play;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Configuration {

    /** definition of regex to filter db related settings. */
    private final String regexDbRelatedSettings = "^(db|javax\\.persistence|jpa|(?:org\\.)?hibernate){1}";

    /** compiled regex as a pattern for reuse to filter all db related settings. */
    private final Pattern compiledRegexDbRelatedSettings = Pattern.compile(regexDbRelatedSettings +".*");

    private String configName;

    public boolean isDefault() {
        return DB.DEFAULT.equals(this.configName);
    }

    public Configuration(String configurationName) {
        this.configName = configurationName;
    }

    public static Set<String> getDbNames() {
        TreeSet<String> dbNames = new TreeSet<>();
        // search for case db= or db.url= as at least one of these property is required
        String DB_CONFIG_PATTERN = "^db\\.([^.]*)$|^db\\.([^.]*)\\.url$";
        Pattern pattern = Pattern.compile(DB_CONFIG_PATTERN);

        // List of properties with 2 words
        List<String> dbProperties = Arrays.asList("db.driver", "db.url", "db.user", "db.pass", "db.isolation", "db.destroyMethod",
                "db.testquery");

        for (String property : Play.configuration.stringPropertyNames()) {
            Matcher m = pattern.matcher(property);
            if (m.matches() && !dbProperties.contains(property)) {
                String dbName = (m.group(1) != null ? m.group(1) : m.group(2));
                dbNames.add(dbName);
            }
            // Special case db=... and
            if ("db".equals(property) || "db.url".equals(property)) {
                dbNames.add("default");
            }
        }
        return new TreeSet<>(dbNames);
    }

    public String getProperty(String key) {
        return this.getProperty(key, null);
    }

    public String getProperty(String key, String defaultString) {
        if (key != null) {
            String newKey = generateKey(key);
            Object value = Play.configuration.get(newKey);
            if (value == null && this.isDefault()) {
                value = Play.configuration.get(key);
            }
            if (value != null) {
                return value.toString();
            }
        }

        return defaultString;
    }

    /**
     * Add a parameter in the configuration
     * 
     * @param key
     *            the key of the parameter
     * @param value
     *            the value of the parameter
     * @return the previous value of the specified key in this hashtable, or null if it did not have one
     */
    public Object put(String key, String value) {
        if (key != null) {
            return Play.configuration.put(generateKey(key), value);
        }
        return null;
    }

    String generateKey(String key) {
        Pattern pattern = Pattern.compile(regexDbRelatedSettings + "(\\.?[\\da-zA-Z\\.-_]*)$");
        Matcher m = pattern.matcher(key);
        if (m.matches()) {
            return m.group(1) + "." + this.configName + m.group(2);
        }
        return key;
    }

    public Map<String, String> getProperties() {
        Map<String, String> properties = new HashMap<>();
        Properties props = Play.configuration;

        for (Object key : Collections.list(props.keys())) {
            String keyName = key.toString();

            Matcher matcher = compiledRegexDbRelatedSettings.matcher(keyName);
            if (matcher.matches()) {
                final String key_prefix_for_db_related_setting = matcher.group(1);
                if (keyName.startsWith(key_prefix_for_db_related_setting + "." + this.configName + ".")) {
                    String type = key_prefix_for_db_related_setting;
                    String newKey = type;
                    if (keyName.length() > (type + "." + this.configName).length()) {
                        newKey += "." + keyName.substring((type + "." + this.configName).length() + 1);
                    }
                    properties.put(newKey, props.get(key).toString());
                } else if (this.isDefault()) {
                    boolean isDefaultProperty = true;
                    Set<String> dBNames = getDbNames();
                    for (String dbName : dBNames) {
                        if (keyName.startsWith("db." + dbName + ".") ||
                            keyName.startsWith("hibernate." + dbName + ".")) {
                            isDefaultProperty = false;
                            break;
                        }
                    }
                    if (isDefaultProperty) {
                        properties.put(keyName, props.get(key).toString());
                    }
                }
            }
        }
        return properties;
    }
}
