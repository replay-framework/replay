package play.mvc;

import jregex.Matcher;
import jregex.Pattern;

class RoutePattern {
  private Pattern routePattern = new Pattern(
    "^({method}GET|POST|PUT|PATCH|DELETE|OPTIONS|HEAD|WS|\\*)[(]?(\\))?\\s+({path}.*/[^\\s]*)\\s+({action}[^\\s(]+)(\\s*)$"
  );

  RouteMatcher matcher(String line) {
    return new RouteMatcher(routePattern.matcher(line));
  }

  static class RouteMatcher {
    private final Matcher matcher;

    RouteMatcher(Matcher matcher) {
      if (!matcher.matches()) {
        throw new IllegalArgumentException("Invalid route definition");
      }
      this.matcher = matcher;
    }

    String action() {return matcher.group("action");}
    String method() {return matcher.group("method");}
    String path() {return matcher.group("path");}
  }
}
