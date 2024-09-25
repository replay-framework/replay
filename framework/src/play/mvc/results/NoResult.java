package play.mvc.results;

import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.mvc.Scope.Flash;
import play.mvc.Scope.RenderArgs;
import play.mvc.Scope.Session;

public class NoResult extends Result {

  @Override
  public void apply(
      Request request, Response response, Session session, RenderArgs renderArgs, Flash flash) {}
}
