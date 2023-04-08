package play.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.Play;
import play.data.validation.Validation;
import play.mvc.Http;
import play.mvc.results.NotFound;
import play.templates.TemplateLoader;
import play.vfs.VirtualFile;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.Map;

@ParametersAreNonnullByDefault
public class ServerHelper {
  private static final Logger logger = LoggerFactory.getLogger(ServerHelper.class);

  @Nonnull
  @CheckReturnValue
  public String generateNotFoundResponse(Http.Request request, String format, NotFound e) {
    return TemplateLoader.load("errors/404." + format).render(getBindingForErrors(request, e, false));
  }

  @Nonnull
  @CheckReturnValue
  public String generateErrorResponse(Http.Request request, String format, Exception e) {
    return TemplateLoader.load("errors/500." + format).render(getBindingForErrors(request, e, true));
  }

  @Nonnull
  @CheckReturnValue
  private Map<String, Object> getBindingForErrors(Http.Request request, Exception e, boolean isError) {
    Map<String, Object> binding = new HashMap<>(4);
    if (isError) {
      binding.put("exception", e);
    }
    else {
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

  @Nullable
  @CheckReturnValue
  public VirtualFile findFile(String resource) {
    VirtualFile file = Play.getVirtualFile(resource);
    if (file != null && file.exists() && file.isDirectory()) {
      VirtualFile index = file.child("index.html");
      if (index.exists()) {
        return index;
      }
    }
    return file;
  }
}
