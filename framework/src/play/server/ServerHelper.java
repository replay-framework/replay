package play.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.Play;
import play.data.validation.Validation;
import play.mvc.Http;
import play.mvc.results.NotFound;
import play.templates.TemplateLoader;
import play.utils.Utils;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.File;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static com.google.common.net.HttpHeaders.KEEP_ALIVE;
import static java.lang.Integer.parseInt;
import static java.util.stream.Collectors.joining;
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static play.mvc.Http.Headers.Values.CLOSE;

@ParametersAreNonnullByDefault
public class ServerHelper {
  private static final Logger logger = LoggerFactory.getLogger(ServerHelper.class);

  @CheckReturnValue
  public static int maxContentLength(int defaultValue) {
    String setting = Play.configuration.getProperty("play.netty.maxContentLength");
    return setting == null ? defaultValue : parseInt(setting);
  }

  @CheckReturnValue
  public boolean isKeepAlive(String protocol, String connectionHeader) {
    switch (protocol) {
      case "HTTP/1.0": return KEEP_ALIVE.equalsIgnoreCase(connectionHeader);
      default: return !CLOSE.equalsIgnoreCase(connectionHeader);
    }
  }

  @CheckReturnValue
  public boolean isModified(String etag, long last, @Nullable String ifNoneMatch, @Nullable String ifModifiedSince) {
    if (ifNoneMatch != null) {
      return !ifNoneMatch.equals(etag);
    }

    if (ifModifiedSince != null) {
      if (!isEmpty(ifModifiedSince)) {
        try {
          Date browserDate = Utils.getHttpDateFormatter().parse(ifModifiedSince);
          if (browserDate.getTime() >= last) {
            return false;
          }
        }
        catch (ParseException ex) {
          logger.warn("Can't parse HTTP date", ex);
        }
        return true;
      }
    }
    return true;
  }

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

  public String getContentTypeValue(Http.Response response) {
    String contentType = defaultIfBlank(response.contentType, "text/plain");
    if (contentType.startsWith("text/") && !contentType.contains("charset")) {
      return contentType + "; charset=" + response.encoding;
    }
    return contentType;
  }

  @Nullable
  @CheckReturnValue
  public File findFile(String resource) {
    File file = Play.file(resource);
    if (file != null && file.exists() && file.isDirectory()) {
      File index = new File(file, "index.html");
      if (index.exists()) {
        return index;
      }
    }
    return file;
  }

  @Nonnull
  @CheckReturnValue
  public String relativeUrl(String path, @Nullable String query) {
    return Stream.of(path, query)
      .filter(s -> s != null && !s.isEmpty())
      .collect(joining("?"));
  }
}
