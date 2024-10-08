package play.exceptions;

/** A exception during tag invocation */
public class TagInternalException extends RuntimeException {

  public TagInternalException(String message) {
    super(message);
  }

  public TagInternalException(String message, Throwable cause) {
    super(message, cause);
  }
}
