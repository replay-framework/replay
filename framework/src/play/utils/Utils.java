package play.utils;

import static java.util.stream.Collectors.joining;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import play.Play;

@ParametersAreNonnullByDefault
public class Utils {

  public static <T> String join(T[] values, String separator) {
    if (values.length == 0) {
      return "";
    }

    return Stream.of(values).map(Object::toString).collect(joining(separator));
  }

  public static class Maps {

    public static void mergeValueInMap(
        Map<String, String[]> map, String name, @Nullable String value) {
      String[] newValues;
      String[] oldValues = map.get(name);
      if (oldValues == null) {
        newValues = new String[1];
        newValues[0] = value;
      } else {
        newValues = new String[oldValues.length + 1];
        System.arraycopy(oldValues, 0, newValues, 0, oldValues.length);
        newValues[oldValues.length] = value;
      }
      map.put(name, newValues);
    }

    public static void mergeValueInMap(Map<String, String[]> map, String name, String[] values) {
      for (String value : values) {
        mergeValueInMap(map, name, value);
      }
    }

    public static <K, V> Map<K, V> filterMap(Map<K, V> map, String keypattern) {
      try {
        @SuppressWarnings("unchecked")
        Map<K, V> filtered = map.getClass().getDeclaredConstructor().newInstance();
        for (Map.Entry<K, V> entry : map.entrySet()) {
          K key = entry.getKey();
          if (key.toString().matches(keypattern)) {
            filtered.put(key, entry.getValue());
          }
        }
        return filtered;
      } catch (Exception iex) {
        throw new RuntimeException("Failed to create " + map.getClass().getName(), iex);
      }
    }
  }

  private static final ThreadLocal<SimpleDateFormat> httpFormatter = new ThreadLocal<>();

  public static SimpleDateFormat getHttpDateFormatter() {
    if (httpFormatter.get() == null) {
      httpFormatter.set(new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US));
      httpFormatter.get().setTimeZone(TimeZone.getTimeZone("GMT"));
    }
    return httpFormatter.get();
  }

  public static class AlternativeDateFormat {
    private final List<SimpleDateFormat> formats;

    private AlternativeDateFormat(Locale locale, String... alternativeFormats) {
      formats =
          Stream.of(alternativeFormats)
              .map(format -> new SimpleDateFormat(format, locale))
              .collect(Collectors.toList());
    }

    public Date parse(String source) throws ParseException {
      for (SimpleDateFormat dateFormat : formats) {
        if (source.length() == dateFormat.toPattern().replace("'", "").length()) {
          try {
            return dateFormat.parse(source);
          } catch (ParseException ignore) {
          }
        }
      }
      throw new ParseException("Date format not understood: " + source, 0);
    }

    private static final ThreadLocal<AlternativeDateFormat> dateformat = new ThreadLocal<>();

    public static AlternativeDateFormat getDefaultFormatter() {
      if (dateformat.get() == null) {
        dateformat.set(
            new AlternativeDateFormat(
                Locale.US,
                "yyyy-MM-dd'T'HH:mm:ss'Z'", // ISO8601 + timezone
                "yyyy-MM-dd'T'HH:mm:ss", // ISO8601
                "yyyy-MM-dd HH:mm:ss",
                "yyyyMMdd HHmmss",
                "yyyy-MM-dd",
                "yyyyMMdd'T'HHmmss",
                "yyyyMMddHHmmss",
                "dd'/'MM'/'yyyy",
                "dd-MM-yyyy",
                "dd'/'MM'/'yyyy HH:mm:ss",
                "dd-MM-yyyy HH:mm:ss",
                "ddMMyyyy HHmmss",
                "ddMMyyyy"));
      }
      return dateformat.get();
    }
  }

  public static String urlDecodePath(@Nullable String enc) {
    return enc == null
        ? null
        : URLDecoder.decode(enc.replaceAll("\\+", "%2B"), Play.defaultWebEncoding);
  }

  public static String urlEncodePath(String plain) {
    return URLEncoder.encode(plain, Play.defaultWebEncoding);
  }

  public static String formatMemorySize(long bytes) {
    if (bytes < 1024L) {
      return bytes + " B";
    }
    if (bytes < 1048576L) {
      return bytes / 1024L + "KB";
    }
    if (bytes < 1073741824L) {
      return bytes / 1048576L + "MB";
    }
    return bytes / 1073741824L + "GB";
  }
}
