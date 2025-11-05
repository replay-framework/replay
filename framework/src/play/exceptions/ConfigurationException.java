package play.exceptions;

import com.google.errorprone.annotations.CheckReturnValue;
import org.jspecify.annotations.NullMarked;

@NullMarked
@CheckReturnValue
public class ConfigurationException extends PlayException {
  public ConfigurationException(String message) {
    super(message);
  }

  public ConfigurationException(String message, Throwable cause) {
    super(message, cause);
  }
}
