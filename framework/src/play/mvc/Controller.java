package play.mvc;

import play.data.validation.Validation;
import play.mvc.results.BadRequest;
import play.mvc.results.Forbidden;
import play.rebel.View;

public class Controller implements PlayController {
  protected Http.Request request = Http.Request.current();
  protected Http.Response response = Http.Response.current();
  protected Scope.Session session = Scope.Session.current();
  protected Scope.Flash flash = Scope.Flash.current();
  protected Scope.Params params = Scope.Params.current();
  protected Scope.RenderArgs renderArgs = Scope.RenderArgs.current();
  protected Validation validation = Validation.current();

  protected void checkAuthenticity() {
    if (params.get("authenticityToken") == null
      || !params.get("authenticityToken").equals(session.getAuthenticityToken())) {
      throw new Forbidden("Bad authenticity token");
    }
  }

  protected static void forbidden() {
    throw new Forbidden("Access denied");
  }

  protected static void forbidden(String reason) {
    throw new Forbidden(reason);
  }

  protected BadRequest badRequest() {
    return new BadRequest("Bad request");
  }

  protected View viewResult() {
    return new View();
  }

  protected View viewResult(String templateName) {
    return new View(templateName);
  }

  protected Forbidden forbiddenResult() {
    return new Forbidden();
  }
}
