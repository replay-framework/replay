package play.mvc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.libs.Codec;

import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Provides operations around the encoding and decoding of Cookie data.
 */
public class SessionDataEncoder {
  private static final Logger logger = LoggerFactory.getLogger(SessionDataEncoder.class);

  public Map<String, String> decode(String data) {
    Map<String, String> map = new HashMap<>();
    try {
      data = new String(Codec.decodeBASE64(data), UTF_8);
    }
    catch (Exception cookieWasNotEncoded) {
      logger.warn("!!! Cookie decoding (base64) failed, will try plain");
    }
    String[] keyValues = data.split("&");
    for (String keyValue : keyValues) {
      String[] splitted = keyValue.split("=", 2);
      if (splitted.length == 2) {
        try {
          map.put(URLDecoder.decode(splitted[0], UTF_8), URLDecoder.decode(splitted[1], UTF_8));
        }
        catch (Exception e) {
          logger.error("!!! Cookie parsing failed: " + e + ",\ncookie.key=" + splitted[0] + "\n  Data: " + data);
        }
      }
    }
    return map;
  }

  public String encode(Map<String, String> map) {
    StringBuilder data = new StringBuilder();
    String separator = "";
    for (Map.Entry<String, String> entry : map.entrySet()) {
      if (entry.getValue() != null) {
        data.append(separator)
            .append(escape(entry.getKey()))
            .append("=")
            .append(escape(entry.getValue()));
        separator = "&";
      }
    }
    return Codec.encodeBASE64(data.toString());
  }

  private String escape(String value) {
    return value.replace("%", "%25").replace("=", "%3D").replace("&", "%26").replace("+", "%2B");
  }
}
