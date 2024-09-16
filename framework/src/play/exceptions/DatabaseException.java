
package play.exceptions;

public class DatabaseException extends PlayException {
  public DatabaseException(Throwable cause) {
    super(cause);
  }

  public DatabaseException(String message, Throwable cause) {
    super(message, cause);
  }
}
