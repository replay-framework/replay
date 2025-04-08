package play;

import java.util.Properties;

/**
 * With this helper class one could do the following:<p/>
 * <pre>
 * if (!Play.configuration.property("XForwardedSupport", "not-all").hasValue("all") { ... }
 * </pre>
 * instead of:
 * <pre>
 * if (!"ALL".equalsIgnoreCase(Play.configuration.getProperty("XForwardedSupport")) { ... }
 * </pre>
 */
public class ConfProperties extends Properties {

  public static class PropertyValue {
    private final String propertyValue;

    PropertyValue(final String propertyValue) {
      this.propertyValue = propertyValue;
    }

    /** Case insensitive equality check on configuration properties. */
    public boolean hasValue(final String equalsTo) {
      return propertyValue.equalsIgnoreCase(equalsTo);
    }
  }

  /** Helper method of an often recurring type of check on Play's configuration properties. */
  public PropertyValue property(final String key, final String defaultValue) {
    return new PropertyValue(this.getProperty(key, defaultValue));
  }
}
