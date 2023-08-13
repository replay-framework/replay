package play;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.data.validation.Validation;
import play.exceptions.TemplateNotFoundException;
import play.exceptions.UnexpectedException;
import play.libs.MimeTypes;
import play.mvc.Http;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.mvc.Scope.Flash;
import play.mvc.Scope.RenderArgs;
import play.mvc.Scope.Session;
import play.mvc.results.NotFound;
import play.templates.TemplateLoader;


public class TemplateErrorHandler implements ErrorHandler {
  private static final Logger logger = LoggerFactory.getLogger(TemplateErrorHandler.class);

  public void applicationErrorResponse(play.mvc.results.Error error, Request request, Response response,
      Session session,
      RenderArgs renderArgs, Flash flash) {
    response.status = error.getStatus();
    String format = request.format;
    if (request.isAjax() && "html".equals(format)) {
      format = "txt";
    }
    response.contentType = MimeTypes.getContentType("xx." + format);
    Map<String, Object> binding = renderArgs.data;
    binding.put("exception", error);
    binding.put("result", error);
    binding.put("session", session);
    binding.put("request", request);
    binding.put("response", response);
    binding.put("flash", flash);
    binding.put("params", request.params);
    binding.put("play", new Play());

    String templatePath = "errors/" + error.getStatus() + "." + (format == null ? "html" : format);
    String errorHtml;
    try {
      errorHtml = TemplateLoader.load(templatePath).render(binding);
    } catch (TemplateNotFoundException noTemplateInDesiredFormat) {
      errorHtml = error.getMessage();
    } catch (Exception e) {
      logger.error("Failed to render {}", templatePath, e);
      errorHtml = error.getMessage();
    }
    try {
      response.out.write(errorHtml.getBytes(response.encoding));
    } catch (Exception e) {
      throw new UnexpectedException(e);
    }
  }

  @Nonnull
  @CheckReturnValue
  public String frameworkErrorResponse(Http.Request request, String format, Exception e, Charset encoding) {
    if (e instanceof NotFound) {
      return TemplateLoader.load("errors/404." + format).render(getBindingForErrors(request, e, false));
    } else {
      return TemplateLoader.load("errors/500." + format).render(getBindingForErrors(request, e, true));
    }
  }

  @Nonnull
  @CheckReturnValue
  private Map<String, Object> getBindingForErrors(Http.Request request, Exception e, boolean isError) {
    Map<String, Object> binding = new HashMap<>(4);
    if (isError) {
      binding.put("exception", e);
    } else {
      binding.put("result", e);
    }
    binding.put("request", request);
    binding.put("play", new Play());
    try {
      binding.put("errors", Validation.errors());
    } catch (Exception ex) {
      logger.error("Error when getting Validation errors", ex);
    }
    return binding;
  }
}
