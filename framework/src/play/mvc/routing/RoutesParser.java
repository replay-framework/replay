package play.mvc.routing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.Play;
import play.mvc.Router.Route;
import play.vfs.VirtualFile;

import java.util.ArrayList;
import java.util.List;

public class RoutesParser {
  private static final Logger logger = LoggerFactory.getLogger(RoutesParser.class);
  private static final RoutePattern routePattern = new RoutePattern();

  public List<Route> parse(VirtualFile routeFile) {
    return parse(routeFile, "");
  }
  /**
   * Parse a route file. If an action starts with <i>"plugin:name"</i>, replace that route by the ones declared in the
   * plugin route file denoted by that <i>name</i>, if found.
   *
   * @param prefix    The prefix that the path of all routes in this route file start with. This prefix should not end with
   *                  a '/' character.
   */
  private List<Route> parse(VirtualFile routeFile, String prefix) {
    String fileAbsolutePath = routeFile.getRealFile().getAbsolutePath();
    String content = routeFile.contentAsString();
    assertDoesNotContain(fileAbsolutePath, content, "${");
    assertDoesNotContain(fileAbsolutePath, content, "#{");
    assertDoesNotContain(fileAbsolutePath, content, "%{");
    return parse(content, prefix, fileAbsolutePath);
  }

  private void assertDoesNotContain(String fileAbsolutePath, String content, String substring) {
    if (content.contains(substring)) {
      throw new IllegalArgumentException("Routes file " + fileAbsolutePath + " cannot contain " + substring);
    }
  }

  private List<Route> parse(String content, String prefix, String fileAbsolutePath) {
    List<Route> routes = new ArrayList<>();

    int lineNumber = 0;
    for (String line : content.split("\n")) {
      lineNumber++;
      line = line.trim().replaceAll("\\s+", " ");
      if (line.isEmpty() || line.startsWith("#")) {
        continue;
      }
      try {
        RoutePattern.RouteLine route = routePattern.matcher(line);
        if (route.action.startsWith("module:")) {
          String moduleName = route.action.substring("module:".length());
          String newPrefix = prefix + route.path;
          if (newPrefix.length() > 1 && newPrefix.endsWith("/")) {
            newPrefix = newPrefix.substring(0, newPrefix.length() - 1);
          }
          if (moduleName.equals("*")) {
            for (String p : Play.modulesRoutes.keySet()) {
              routes.addAll(parse(Play.modulesRoutes.get(p), newPrefix + p));
            }
          } else if (Play.modulesRoutes.containsKey(moduleName)) {
            routes.addAll(parse(Play.modulesRoutes.get(moduleName), newPrefix));
          } else {
            logger.error("Cannot include routes for module {} (not found)", moduleName);
          }
        } else {
          routes.add(new Route(route.method, (prefix + route.path).replace("//", "/"), route.action, fileAbsolutePath, lineNumber));
        }
      }
      catch (IllegalArgumentException invalidRoute) {
        logger.error("{} at line {}: {}", invalidRoute, lineNumber, line);
      }
    }

    return routes;
  }
}
