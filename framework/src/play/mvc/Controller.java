package play.mvc;

import java.util.Objects;
import play.data.validation.Validation;
import play.mvc.results.BadRequest;
import play.mvc.results.Forbidden;
import play.rebel.View;

public class Controller implements PlayContextController {
  protected ActionContext actionContext;
  protected Http.Request request;
  protected Http.Response response;
  protected Scope.Session session;
  protected Scope.Flash flash;
  protected Scope.Params params;
  protected Scope.RenderArgs renderArgs;
  protected Validation validation;

  public void setContext(ActionContext actionContext) {
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
        || !Objects.equals(params.get("authenticityToken"), session.getAuthenticityToken())) {
      throw new Forbidden("Bad authenticity token");
    }
  }

  /**
   * This "Play1-style" method throws {@link play.mvc.results.Forbidden} exception.
   *
   * @deprecated instead if throwing an exception we recommend to RETURN the result. It makes your
   *     code clear and testable. Use method {@link #forbiddenResult()}.
   */
  @Deprecated
  protected static void forbidden() {
    throw new Forbidden("Access denied");
  }

  /**
   * This "Play1-style" method throws {@link play.mvc.results.Forbidden} exception.
   *
   * @deprecated instead if throwing an exception we recommend to RETURN the result. It makes your
   *     code clear and testable. Use method {@link #forbiddenResult()}.
   */
  @Deprecated
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
