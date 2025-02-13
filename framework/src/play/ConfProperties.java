package play;

import java.util.Properties;

public class ConfProperties extends Properties {

  public static class PropertyResult {
    private final String propertyValue;

    PropertyResult(final String propertyValue) {
      this.propertyValue = propertyValue;
    }

    /** Case insensitive equality check on configuration properties. */
    public boolean hasValue(final String equalsTo) {
      return propertyValue.equalsIgnoreCase(equalsTo);
    }
  }

  /** Helper method of an often recurring type of check on Play's configuration properties. */
  public PropertyResult property(final String key, final String defaultValue) {
    return new PropertyResult(this.getProperty(key, defaultValue));
  }
}
