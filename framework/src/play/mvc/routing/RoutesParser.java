package play.mvc.routing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.ClasspathResource;
import play.mvc.Router.Route;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class RoutesParser {
  private static final Logger logger = LoggerFactory.getLogger(RoutesParser.class);
  private static final Pattern RE_MULTIPLE_SPACES = Pattern.compile("\\s+");
  private static final RoutePattern routePattern = new RoutePattern();

  /**
   * Parse a route file. If an action starts with <i>"plugin:name"</i>, replace that route by the ones declared in the
   * plugin route file denoted by that <i>name</i>, if found.
   */
  public List<Route> parse(ClasspathResource routesFile) {
    String content = routesFile.content();
    assertDoesNotContain(routesFile, content, "${");
    assertDoesNotContain(routesFile, content, "#{");
    assertDoesNotContain(routesFile, content, "%{");
    return parse(content, routesFile);
  }

  private void assertDoesNotContain(ClasspathResource routesFile, String content, String substring) {
    if (content.contains(substring)) {
      throw new IllegalArgumentException("Routes file " + routesFile + " cannot contain " + substring);
    }
  }

  private List<Route> parse(String content, ClasspathResource routesFile) {
    List<Route> routes = new ArrayList<>();

    int lineNumber = 0;
    for (String line : content.split("\n")) {
      lineNumber++;
      line = removeMultipleSpaces(line);
      if (line.isEmpty() || line.startsWith("#")) {
        continue;
      }
      try {
        RoutePattern.RouteLine route = routePattern.matcher(line);
        if (route.action.startsWith("module:")) {
          throw new IllegalArgumentException(String.format("Modules are not supported anymore (found route '%s')", route.action));
        } else {
          routes.add(new Route(route.method, route.path.replace("//", "/"), route.action, routesFile, lineNumber));
        }
      }
      catch (IllegalArgumentException invalidRoute) {
        logger.error("{} at line {}: {}", invalidRoute, lineNumber, line);
      }
    }

    return routes;
  }

  String removeMultipleSpaces(String line) {
    return RE_MULTIPLE_SPACES.matcher(line.trim()).replaceAll(" ");
  }
}
