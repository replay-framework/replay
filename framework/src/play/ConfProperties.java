package play;

import java.util.Properties;

public class ConfProperties extends Properties {

  /** Helper method of an often recurring type of check on Play's configuration properties. */
  public boolean propWithDefaultEqualsTo(
      final String key,
      final String defaultValue,
      final String equalsTo) {
    return equalsTo.equalsIgnoreCase(this.getProperty(key, defaultValue));
  }
}
