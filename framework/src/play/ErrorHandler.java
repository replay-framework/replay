package play;

import java.nio.charset.Charset;
import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import play.mvc.Http;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.mvc.Scope.Flash;
import play.mvc.Scope.RenderArgs;
import play.mvc.Scope.Session;

public interface ErrorHandler {

  /** Called for errors thrown/returned by the application. */
  void applicationErrorResponse(play.mvc.results.Error error, Request request, Response response,
      Session session, RenderArgs renderArgs, Flash flash);

  /**
   * Called when errors originate in the framework (only 404 and 500).
   *
   * <p>When 'e' is of type NotFound we're dealing with a 404, otherwise it's a 500.
   */
  @Nonnull
  @CheckReturnValue
  String frameworkErrorResponse(Http.Request request, String format, Exception e, Charset encoding);
}
