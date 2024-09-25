package play.exceptions;

public class ActionNotFoundException extends PlayException {
  private final String action;

  public ActionNotFoundException(String action, Throwable cause) {
    super(String.format("Action %s not found", action), cause);
    this.action = action;
  }

  public String getAction() {
    return action;
  }
}
