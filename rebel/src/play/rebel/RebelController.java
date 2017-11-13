package play.rebel;

import play.data.validation.Validation;
import play.mvc.Http;
import play.mvc.PlayController;
import play.mvc.Scope;
import play.mvc.results.*;

import java.io.InputStream;
import java.lang.annotation.Annotation;

/**
 * A superclass for all controllers that are intended to work without play enhancers.
 */
public class RebelController implements PlayController {

  protected Http.Request request = Http.Request.current();

  @Deprecated
  protected static Http.Request request() {
    return Http.Request.current();
  }
  
  protected Http.Response response = Http.Response.current();

  protected Scope.Session session = Scope.Session.current();

  @Deprecated
  protected static Scope.Session session() {
    return Scope.Session.current();
  }

  protected Scope.Flash flash = Scope.Flash.current();

  @Deprecated
  protected static Scope.Flash flash() {
    return Scope.Flash.current();
  }

  @Deprecated
  protected static void flash(String key, Object value) {
    Scope.Flash.current().put(key, value);
  }

  protected Scope.Params params = Scope.Params.current();

  @Deprecated
  protected static Scope.Params params() {
    return Scope.Params.current();
  }
  
  protected Scope.RenderArgs renderArgs = Scope.RenderArgs.current();

  @Deprecated
  protected static Scope.RenderArgs renderArgs() {
    return Scope.RenderArgs.current();
  }

  protected Scope.RouteArgs routeArgs = Scope.RouteArgs.current();
  
  protected Validation validation = Validation.current();

  protected static void checkAuthenticity() {
    if (Scope.Params.current().get("authenticityToken") == null
        || !Scope.Params.current().get("authenticityToken").equals(Scope.Session.current().getAuthenticityToken())) {
      throw new Forbidden("Bad authenticity token");
    }
  }

  @Deprecated
  protected static void renderJSON(String jsonString) {
    throw new RenderJson(jsonString);
  }

  @Deprecated
  protected static void renderJSON(Object o) {
    throw new RenderJson(o);
  }

  @Deprecated
  protected static void renderText(Object text) {
    throw new RenderText(text == null ? "" : text.toString());
  }

  @Deprecated
  protected static void renderBinary(InputStream is, String name) {
    throw new RenderBinary(is, name, false);
  }

  protected static void forbidden() {
    throw new Forbidden("Access denied");
  }

  protected static void forbidden(String reason) {
    throw new Forbidden(reason);
  }

  @Deprecated
  protected static void notFound() {
    notFound("");
  }

  @Deprecated
  protected static void notFound(String what) {
    throw new NotFound(what);
  }

  @Deprecated
  protected static void error(String reason) {
    throw new play.mvc.results.Error(reason);
  }

  @Deprecated
  protected static void error(int status, String reason) {
    throw new play.mvc.results.Error(status, reason);
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

  protected static <T extends Annotation> T getActionAnnotation(Class<T> annotationClass) {
    return request().invokedMethod.getAnnotation(annotationClass);
  }
}
