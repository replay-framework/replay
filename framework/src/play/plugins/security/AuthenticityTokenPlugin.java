package play.plugins.security;

import play.PlayPlugin;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.mvc.NoAuthenticityToken;
import play.mvc.Scope.Flash;
import play.mvc.Scope.RenderArgs;
import play.mvc.Scope.Session;
import play.mvc.results.Forbidden;

import java.lang.reflect.Method;

public class AuthenticityTokenPlugin extends PlayPlugin {
  @Override
  public void beforeActionInvocation(Request request, Response response, Session session, RenderArgs renderArgs,
                                     Flash flash, Method actionMethod) {
    if (!"POST".equalsIgnoreCase(request.method)) return;
    if (request.invokedMethod.isAnnotationPresent(NoAuthenticityToken.class)) return;

    String authenticityToken = request.params.get("authenticityToken");

    if (authenticityToken == null) {
      throw new Forbidden("No authenticity token");
    }
    if (!authenticityToken.equals(session.getAuthenticityToken())) {
      throw new Forbidden("Bad authenticity token");
    }
  }
}
