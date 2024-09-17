package play.libs;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Time utils
 *
 * <p>Provides a parser for time expression.
 *
 * <p>Time expressions provide the ability to specify complex time combinations such as
 * &quot;2d&quot;, &quot;1w2d3h10s&quot; or &quot;2d4h10s&quot;.
 */
public class Time {
  private static final Pattern TIME_CHUNKS = Pattern.compile("(([0-9]+?)(d|h|mi|min|mn|s))+?");
  private static final Integer MINUTE = 60;
  private static final Integer HOUR = 60 * MINUTE;
  private static final Integer DAY = 24 * HOUR;

  /**
   * Parse a duration
   *
   * @param duration 3h, 2mn, 7s or combination 2d4h10s, 1w2d3h10s
   * @return The number of seconds
   */
  public static int parseDuration(String duration) {
    if (duration == null) {
      throw new IllegalArgumentException("duration cannot be null");
    }

    Matcher matcher = TIME_CHUNKS.matcher(duration);
    int seconds = 0;
    if (!matcher.matches()) {
      throw new IllegalArgumentException(
          String.format("Invalid duration pattern: \"%s\"", duration));
    }

    matcher.reset();
    while (matcher.find()) {
      switch (matcher.group(3)) {
        case "d":
          seconds += Integer.parseInt(matcher.group(2)) * DAY;
          break;
        case "h":
          seconds += Integer.parseInt(matcher.group(2)) * HOUR;
          break;
        case "mi":
        case "min":
        case "mn":
          seconds += Integer.parseInt(matcher.group(2)) * MINUTE;
          break;
        default:
          seconds += Integer.parseInt(matcher.group(2));
          break;
      }
    }

    return seconds;
  }
}
