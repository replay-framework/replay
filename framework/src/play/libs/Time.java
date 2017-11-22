package play.libs;

import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Time utils
 *
 * Provides a parser for time expression.
 * <p>
 * Time expressions provide the ability to specify complex time combinations
 * such as &quot;2d&quot;, &quot;1w2d3h10s&quot; or &quot;2d4h10s&quot;.
 * </p>
 *
 */
public class Time {
    private static final Pattern p = Pattern.compile("(([0-9]+?)((d|h|mi|min|mn|s)))+?");
    private static final Integer MINUTE = 60;
    private static final Integer HOUR = 60 * MINUTE;
    private static final Integer DAY = 24 * HOUR;

    /**
     * Parse a duration
     *
     * @param duration
     *            3h, 2mn, 7s or combination 2d4h10s, 1w2d3h10s
     * @return The number of seconds
     */
    public static int parseDuration(String duration) {
        if (duration == null) {
            return 30 * DAY;
        }

        Matcher matcher = p.matcher(duration);
        int seconds = 0;
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid duration pattern : " + duration);
        }

        matcher.reset();
        while (matcher.find()) {
            if (matcher.group(3).equals("d")) {
                seconds += Integer.parseInt(matcher.group(2)) * DAY;
            } else if (matcher.group(3).equals("h")) {
                seconds += Integer.parseInt(matcher.group(2)) * HOUR;
            } else if (matcher.group(3).equals("mi") || matcher.group(3).equals("min") || matcher.group(3).equals("mn")) {
                seconds += Integer.parseInt(matcher.group(2)) * MINUTE;
            } else {
                seconds += Integer.parseInt(matcher.group(2));
            }
        }

        return seconds;
    }
}
