package play.plugins.security;

import java.lang.reflect.Method;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import play.PlayPlugin;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.mvc.NoAuthenticityToken;
import play.mvc.Scope.Flash;
import play.mvc.Scope.RenderArgs;
import play.mvc.Scope.Session;
import play.mvc.results.Forbidden;

public class AuthenticityTokenPlugin extends PlayPlugin {

  @ParametersAreNonnullByDefault
  @Override
  public void beforeActionInvocation(
      Request request,
      Response response,
      Session session,
      RenderArgs renderArgs,
      Flash flash,
      Method actionMethod) {
    if (!"POST".equalsIgnoreCase(request.method)) return;
    if (request.invokedMethod.isAnnotationPresent(NoAuthenticityToken.class)) return;

    String[] authenticityToken = request.params.getAll("authenticityToken");
    verifyTokenIsPresent(authenticityToken);
    verifyAllTokensAreEqual(authenticityToken);
    verifyToken(session, authenticityToken[0]);
  }

  private void verifyTokenIsPresent(@Nullable String[] authenticityToken) {
    if (authenticityToken == null || authenticityToken.length == 0) {
      throw new Forbidden("No authenticity token");
    }
  }

  private void verifyAllTokensAreEqual(String[] authenticityToken) {
    if (authenticityToken.length > 1) {
      for (int i = 1; i < authenticityToken.length; i++) {
        if (!authenticityToken[i].equals(authenticityToken[0])) {
          throw new Forbidden("Multiple authenticity tokens");
        }
      }
    }
  }

  private void verifyToken(Session session, String authenticityToken) {
    if (!authenticityToken.equals(session.getAuthenticityToken())) {
      throw new Forbidden("Bad authenticity token");
    }
  }
}
