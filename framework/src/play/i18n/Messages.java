package play.i18n;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.Play;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * I18n Helper
 * <p>
 * translation are defined as properties in /conf/messages.<em>locale</em> files with locale being the i18n country code
 * fr, en, fr_FR
 * 
 * <pre>
 * # /conf/messages.fr
 * hello=Bonjour, %s !
 * </pre>
 * 
 * {@code
 * Messages.get( "hello", "World"); // => "Bonjour, World !"
 * }
 * 
 */
public class Messages {
    private static final Logger logger = LoggerFactory.getLogger(Messages.class);

    private static final Object[] NO_ARGS = new Object[] { null };

    public static Properties defaults = new Properties();

    public static Map<String, Properties> locales = new HashMap<>();

    private static final Pattern recursive = Pattern.compile("&\\{(.*?)}");

    /**
     * Given a message code, translate it using current locale. If there is no message in the current locale for the
     * given key, the key is returned.
     * 
     * @param key
     *            the message code
     * @param args
     *            optional message format arguments
     * @return translated message
     */
    public static String get(Object key, Object... args) {
        return getMessage(Lang.get(), key, args);
    }

    /**
     * Return several messages for a locale
     * 
     * @param locale
     *            the locale code, e.g. fr, fr_FR
     * @param keys
     *            the keys to get messages from. Wildcards can be used at the end: {'title', 'login.*'}
     * @return messages as a {@link Properties java.util.Properties}
     */
    public static Properties find(String locale, Set<String> keys) {
        Properties result = new Properties();
        Properties all = all(locale);
        // Expand the set for wildcards
        Set<String> wildcards = new HashSet<>();
        for (String key : keys) {
            if (key.endsWith("*"))
                wildcards.add(key);
        }
        for (String key : wildcards) {
            keys.remove(key);
            String start = key.substring(0, key.length() - 1);
            for (Object key2 : all.keySet()) {
                if (((String) key2).startsWith(start)) {
                    keys.add((String) key2);
                }
            }
        }
        // Build the result
        for (Object key : all.keySet()) {
            if (keys.contains(key)) {
                result.put(key, all.get(key));
            }
        }
        return result;
    }

    public static String getMessage(String locale, Object key, Object... args) {
        return getMessage(locale, (k) -> k.toString(), key, args);
    }

    public static String getMessage(@Nonnull String locale, @Nonnull Function<Object, String> defaultMessage, @Nullable Object key, Object... args) {
        // Check if there is a plugin that handles translation
        Optional<String> message = Play.pluginCollection.getMessage(locale, key, args);
        if (message.isPresent()) {
            return message.get();
        }

        if (key == null || "".equals(key)) {
            return "";
        }
        String value = null;
        if (locales != null) {
            if (locales.containsKey(locale)) {
                value = locales.get(locale).getProperty(key.toString());
            }
            if (value == null && locale != null && locale.length() == 5 && locales.containsKey(locale.substring(0, 2))) {
                value = locales.get(locale.substring(0, 2)).getProperty(key.toString());
            }
        }
        if (value == null && defaults != null) {
            value = defaults.getProperty(key.toString());
        }
        if (value == null) {
            value = defaultMessage.apply(key);
        }
        if (value == null) {
            return "";
        }
        Locale l = Lang.getLocaleOrDefault(locale);
        return formatString(l, value, args);
    }

    public static String formatString(String value, Object... args) {
        return formatString(Lang.getLocale(), value, args);
    }

    public static String formatString(Locale locale, String value, Object... args) {
        String message = String.format(locale, value, coolStuff(value, args));

        Matcher matcher = recursive.matcher(message);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(sb, get(matcher.group(1)));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private static final Pattern formatterPattern = Pattern.compile("%((\\d+)\\$)?([-#+ 0,(]+)?(\\d+)?([.]\\d+)?([bBhHsScCdoxXeEfgGaAtT])");

    @SuppressWarnings("unchecked")
    private static Object[] coolStuff(String pattern, Object[] args) {
        // when invoked with a null argument we get a null args instead of an
        // array with a null value.

        if (args == null || args.length == 0)
            return NO_ARGS;

        Class<? extends Number>[] conversions = new Class[args.length];

        Matcher matcher = formatterPattern.matcher(pattern);
        int incrementalPosition = 1;
        while (matcher.find()) {
            String conversion = matcher.group(6);
            Integer position;
            if (matcher.group(2) == null) {
                position = incrementalPosition++;
            } else {
                position = Integer.parseInt(matcher.group(2));
            }
            if (conversion.equals("d") && position <= conversions.length) {
                conversions[position - 1] = Long.class;
            }
            if (conversion.equals("f") && position <= conversions.length) {
                conversions[position - 1] = Double.class;
            }
        }

        Object[] result = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            if (args[i] == null) {
                continue;
            }
            if (conversions[i] == null) {
                result[i] = args[i];
            } else {
                try {
                    String argValue = String.valueOf(args[i]);
                    if (Double.class.isAssignableFrom(conversions[i])) {
                        result[i] = Double.parseDouble(argValue);
                    }
                    else if (Long.class.isAssignableFrom(conversions[i])) {
                        result[i] = Long.parseLong(argValue.contains(".") ? argValue.substring(0, argValue.indexOf('.')) : argValue);
                    }
                    else {
                        throw new IllegalStateException("Cannot parse argument #" + i + "=" + argValue + ": unknown conversion type " + conversions[i]);
                    }
                } catch (IllegalStateException e) {
                    throw e;
                } catch (NumberFormatException ignore) {
                    result[i] = null;
                } catch (RuntimeException e) {
                    logger.error("Invalid value: " + args[i], e);
                    result[i] = null;
                }
            }
        }
        return result;
    }

    /**
     * return all messages for a locale
     * 
     * @param locale
     *            the locale code eg. fr, fr_FR
     * @return messages as a {@link Properties java.util.Properties}
     */
    public static Properties all(String locale) {
        if (locale == null || "".equals(locale)) {
            return defaults;
        }
        Properties mergedMessages = new Properties();
        mergedMessages.putAll(defaults);
        if (locales != null) {
            if (locale.length() == 5 && locales.containsKey(locale.substring(0, 2))) {
                mergedMessages.putAll(locales.get(locale.substring(0, 2)));
            }
            mergedMessages.putAll(locales.get(locale));
        }
        return mergedMessages;
    }

}
