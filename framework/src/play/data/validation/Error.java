package play.data.validation;

import static java.util.Collections.emptyList;

import java.util.Collection;
import com.google.errorprone.annotations.CheckReturnValue;
import net.sf.oval.ConstraintViolation;
import org.jspecify.annotations.NullMarked;
import play.i18n.Messages;

/** A validation error */
@NullMarked
@CheckReturnValue
public class Error {

  private final String message;
  private final String key;
  private final Collection<?> variables;

  public Error(String key, String message, Collection<?> variables) {
    this.message = message;
    this.key = key;
    this.variables = variables;
  }

  /** @return The translated message */
  public String message() {
    return message(key);
  }

  /** @return The field name */
  public String getKey() {
    return key;
  }

  /** @return The variables */
  public Collection<?> getVariables() {
    return variables;
  }

  /**
   * @param key Alternate field name (default to java variable name)
   * @return The translated message
   */
  public String message(String key) {
    key = Messages.get(key);
    Object[] args = new Object[variables.size() + 1];
    args[0] = key;
    int i = 1;
    for (Object variable : variables) {
      args[i++] = variable;
    }
    return Messages.get(message, args);
  }

  @Override
  public String toString() {
    return message();
  }

  String getMessageKey() {
    return message;
  }

  static Error toValidationError(String key, ConstraintViolation violation) {
    Collection<?> variables =
        violation.getMessageVariables() == null
            ? emptyList()
            : violation.getMessageVariables().values();
    return new Error(key, violation.getMessage(), variables);
  }
}
