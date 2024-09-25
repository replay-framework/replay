package play.exceptions;

import java.util.Map;

public class NoRouteFoundException extends PlayException {
  public NoRouteFoundException(String file) {
    super(String.format("No route found to display file %s", file));
  }

  public NoRouteFoundException(String action, Map<String, Object> args) {
    super(String.format("No route found for action %s with arguments %s", action, args));
  }
}
