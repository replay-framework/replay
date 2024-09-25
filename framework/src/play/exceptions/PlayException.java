package play.exceptions;

/** The super class for all Play! exceptions */
public abstract class PlayException extends RuntimeException {

  protected PlayException() {}

  protected PlayException(String message) {
    super(message);
  }

  protected PlayException(Throwable cause) {
    super(cause);
  }

  protected PlayException(String message, Throwable cause) {
    super(message, cause);
  }
}
