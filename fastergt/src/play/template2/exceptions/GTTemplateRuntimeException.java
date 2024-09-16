package play.template2.exceptions;

/**
 * Special exception that should only be thrown when the error is a direct consequence of a
 * template-error. Eg. inside a java-method called from generated-list-code.
 *
 * <p>When this exception passes out, we fix the stacktrace so that the first element is in the
 * template itself.
 */
public class GTTemplateRuntimeException extends GTException {
  public GTTemplateRuntimeException(String s, Throwable cause) {
    super(s, cause);
  }

  public GTTemplateRuntimeException(String s) {
    super(s);
  }
}
