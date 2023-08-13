package play;

import java.nio.charset.Charset;
import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import play.exceptions.UnexpectedException;
import play.libs.MimeTypes;
import play.mvc.Http;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.mvc.Scope.Flash;
import play.mvc.Scope.RenderArgs;
import play.mvc.Scope.Session;
import play.mvc.results.BadRequest;
import play.mvc.results.Forbidden;
import play.mvc.results.NotFound;
import play.mvc.results.Unauthorized;


public class SimpleErrorHandler implements ErrorHandler {

  public void applicationErrorResponse(
      play.mvc.results.Error error,
      Request request,
      Response response,
      Session session,
      RenderArgs renderArgs,
      Flash flash
  ) {
    response.status = error.getStatus();
    String format = request.format;
    if (request.isAjax() && "html".equals(format)) {
      format = "txt";
    }
    response.contentType = MimeTypes.getContentType("xx." + format);
    try {
      final String title = titleFromExceptionType(error);
      if (response.contentType.startsWith("text/html")) {
        response.out.write(
            simpleErrorPageHtml(title, error.getMessage(), response.encoding)
                .getBytes(response.encoding));
      } else {
        response.out.write((title + "\n\n" + error.getMessage()).getBytes(response.encoding));
      }
    } catch (Exception e) {
      throw new UnexpectedException(e);
    }
  }

  /**
   * Called when errors occur in the framework.
   */
  @Nonnull
  @CheckReturnValue
  public String frameworkErrorResponse(
      Http.Request request,
      String format,
      Exception e,
      Charset encoding
  ) {
    final String contentType = MimeTypes.getContentType("xx." + format);
    final String title = titleFromExceptionType(e);
    if (contentType.startsWith("text/html")) {
      return simpleErrorPageHtml(title, e.getMessage(), encoding);
    } else {
      return title + "\n\n" + e.getMessage();
    }
  }

  private static String titleFromExceptionType(Exception e) {
    if (e instanceof BadRequest) {
      return "Bad request";
    } else if (e instanceof Unauthorized) {
      return "Unauthorized";
    } else if (e instanceof Forbidden) {
      return "Forbidden";
    } else if (e instanceof NotFound) {
      return "Not found";
    }
    return "Server error";
  }

  private String simpleErrorPageHtml(String title, String message, Charset encoding) {
    return ""
        + "<!doctype html>\n"
        + "<html lang=\"en\">\n"
        + "    <head>\n"
        + "        <title>Not found</title>\n"
        + "        <meta charset=\"" + encoding.name() + "\"/>\n"
        + "    </head>\n"
        + "    <body>\n"
        + "        <h1>" + title + "</h1>\n"
        + "        <p>" + message + "</p>\n"
        + "    </body>\n"
        + "</html>";
  }
}
