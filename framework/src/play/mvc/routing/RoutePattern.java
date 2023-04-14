package play.mvc.routing;

import java.util.Set;

import static play.mvc.Http.Methods.DELETE;
import static play.mvc.Http.Methods.GET;
import static play.mvc.Http.Methods.HEAD;
import static play.mvc.Http.Methods.OPTIONS;
import static play.mvc.Http.Methods.PATCH;
import static play.mvc.Http.Methods.POST;
import static play.mvc.Http.Methods.PUT;

class RoutePattern {
  private static final Set<String> methodPattern = Set.of(GET, POST, PUT, PATCH, DELETE, OPTIONS, HEAD, "WS", "*");

  RouteLine matcher(String line) {
    return new RouteLine(line.split("\\s+"));
  }

  static class RouteLine {
    public final String method;
    public final String path;
    public final String action;

    RouteLine(String[] tokens) {
      if (tokens.length != 3) {
        throw new IllegalArgumentException("Invalid route definition: expected 3 parts <METHOD> <PATH> <ACTION>");
      }
      this.method = tokens[0];
      this.path = tokens[1];
      this.action = tokens[2];
      if (!methodPattern.contains(method)) {
        throw new IllegalArgumentException("Invalid route definition: unknown method " + method);
      }
    }
  }
}
