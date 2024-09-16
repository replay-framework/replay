package play.libs;

import java.net.URLEncoder;
import play.exceptions.UnexpectedException;

public class URLs {
  public static String encodePart(String part) {
    try {
      return URLEncoder.encode(part, "utf-8");
    } catch (Exception e) {
      throw new UnexpectedException(e);
    }
  }
}
