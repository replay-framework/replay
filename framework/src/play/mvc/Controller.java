package play.mvc;

import play.data.validation.Validation;
import play.mvc.results.BadRequest;
import play.mvc.results.Forbidden;
import play.rebel.View;

public class Controller implements PlayController {
  protected ActionContext actionContext;
  protected Http.Request request;
  protected Http.Response response;
  protected Scope.Session session;
  protected Scope.Flash flash;
  protected Scope.Params params;
  protected Scope.RenderArgs renderArgs;
  protected Validation validation;

  protected void setContext(ActionContext actionContext) {
    this.actionContext = actionContext;
    request = actionContext.request;
    response = actionContext.response;
    session = actionContext.session;
    flash = actionContext.flash;
    params = actionContext.request.params;
    renderArgs = actionContext.renderArgs;
    validation = actionContext.validation;
  }

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
