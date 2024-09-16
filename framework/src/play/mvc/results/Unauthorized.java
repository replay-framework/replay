package play.mvc.results;

import play.mvc.Http;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.mvc.Scope.Flash;
import play.mvc.Scope.RenderArgs;
import play.mvc.Scope.Session;

/** 401 Unauthorized */
public class Unauthorized extends Result {

  private final String realm;

  public Unauthorized(String realm) {
    super(realm);
    this.realm = realm;
  }

  @Override
  public void apply(
      Request request, Response response, Session session, RenderArgs renderArgs, Flash flash) {
    response.status = Http.StatusCode.UNAUTHORIZED;
    response.setHeader("WWW-Authenticate", "Basic realm=\"" + realm + "\"");
  }

  public String getRealm() {
    return realm;
  }
}
