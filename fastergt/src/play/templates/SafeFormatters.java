package play.templates;

import play.templates.types.SafeCSVFormatter;
import play.templates.types.SafeHTMLFormatter;
import play.templates.types.SafeXMLFormatter;

import java.util.HashMap;
import java.util.Map;

public class SafeFormatters {
  private final Map<String, SafeFormatter> safeFormatters = new HashMap<>();

  SafeFormatters() {
    safeFormatters.put("csv", new SafeCSVFormatter());
    safeFormatters.put("html", new SafeHTMLFormatter());
    safeFormatters.put("xml", new SafeXMLFormatter());
  }

  public SafeFormatter get(String extension) {
    return safeFormatters.get(extension);
  }
}
