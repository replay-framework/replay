package play.utils;


import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.*;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.joining;
import static org.apache.commons.io.IOUtils.readLines;
import static org.apache.commons.text.StringEscapeUtils.escapeJava;

/**
 * Custom impl of java.util.properties that preserves the key-order from the file
 * and that reads the properties-file in utf-8
 */
public class OrderSafeProperties extends java.util.Properties {

  // set used to preserve key order
  private final LinkedHashSet<Object> keys = new LinkedHashSet<>();

  /**
   * escapes "special-chars" (to utf-16 on the format \\uxxxx) in lines and store as iso-8859-1.
   * see info about escaping:
   * - <a href="http://download.oracle.com/javase/1.5.0/docs/api/java/util/Properties.html">...</a>
   * - "public void load(InputStream inStream)"
   */
  @Override
  public void load(InputStream inputStream) throws IOException {
    String propertiesAsIso8859 = readLines(inputStream, UTF_8)
        .stream()
        .map(line -> removeEscapedBackslashes(escapeJava(unescapeQuotes(line))))
        .collect(joining("\n"));
    super.load(new StringReader(propertiesAsIso8859));
  }

  /**
   * There is a rule: "...by the rule above, single and double quote characters preceded
   * by a backslash still yield single and double quote characters, respectively."
   * <p>
   * According to this rule, we must transform \" => " and \' => ' before escaping to prevent escaping the backslash
   */
  static String unescapeQuotes(String line) {
    return line.replaceAll("\\\\\"", "\"").replaceAll("(^|[^\\\\])(\\\\')", "$1'");
  }

  static String removeEscapedBackslashes(String line) {
    return line.replaceAll("\\\\\\\\", "\\\\");
  }

  @Override
  public Enumeration<Object> keys() {
    return Collections.enumeration(keys);
  }

  @Override
  public Set<Object> keySet() {
    return keys;
  }

  @Override
  public Object put(Object key, Object value) {
    keys.add(key);
    return super.put(key, value);
  }

  @Override
  public Object remove(Object o) {
    keys.remove(o);
    return super.remove(o);
  }

  @Override
  public void clear() {
    keys.clear();
    super.clear();
  }

  @Override
  public void putAll(Map<?, ?> map) {
    keys.addAll(map.keySet());
    super.putAll(map);
  }

  @Override
  public Set<Map.Entry<Object, Object>> entrySet() {
    Set<Map.Entry<Object, Object>> entrySet = new LinkedHashSet<>(keys.size());
    for (Object key : keys) {
      entrySet.add(Map.entry(key, get(key)));
    }

    return entrySet;
  }
}
