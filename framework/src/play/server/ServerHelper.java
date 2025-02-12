package play.server;

import static com.google.common.net.HttpHeaders.KEEP_ALIVE;
import static java.lang.Integer.parseInt;
import static java.util.stream.Collectors.joining;
import static org.apache.commons.io.FileUtils.copyURLToFile;
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static play.mvc.Http.Headers.Values.CLOSE;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.ClasspathResource;
import play.Play;
import play.data.validation.Validation;
import play.mvc.Http;
import play.mvc.results.NotFound;
import play.templates.TemplateLoader;
import play.utils.Utils;

@ParametersAreNonnullByDefault
public class ServerHelper {
  public static final String SERVER_HELPER_FIND_FILE_TMP_PATH_PREFIX = "serverhelperfindfilesclasspathfiles/";
  private static final Logger logger = LoggerFactory.getLogger(ServerHelper.class);

  @CheckReturnValue
  public static int maxContentLength(int defaultValue) {
    String setting = Play.configuration.getProperty("play.netty.maxContentLength");
    return setting == null ? defaultValue : parseInt(setting);
  }

  @CheckReturnValue
  public boolean isKeepAlive(String protocol, String connectionHeader) {
    return switch (protocol) {
      case "HTTP/1.0" -> KEEP_ALIVE.equalsIgnoreCase(connectionHeader);
      default -> !CLOSE.equalsIgnoreCase(connectionHeader);
    };
  }

  @CheckReturnValue
  public boolean isModified(
      String etag, long last, @Nullable String ifNoneMatch, @Nullable String ifModifiedSince) {
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
        } catch (ParseException ex) {
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
    return TemplateLoader.load("errors/404." + format)
        .render(getBindingForErrors(request, e, false));
  }

  @Nonnull
  @CheckReturnValue
  public String generateErrorResponse(Http.Request request, String format, Exception e) {
    return TemplateLoader.load("errors/500." + format)
        .render(getBindingForErrors(request, e, true));
  }

  @Nonnull
  @CheckReturnValue
  private Map<String, Object> getBindingForErrors(
      Http.Request request, Exception e, boolean isError) {
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

  public String getContentTypeValue(Http.Response response) {
    String contentType = defaultIfBlank(response.contentType, "text/plain");
    if (contentType.startsWith("text/") && !contentType.contains("charset")) {
      return contentType + "; charset=" + response.encoding;
    }
    return contentType;
  }

  @Nullable
  @CheckReturnValue
  public static File findFile(String resource) {
    File file = Play.file(resource);
    if (file != null && file.exists() && file.isDirectory()) {
      File index = new File(file, "index.html");
      if (index.exists()) {
        return index;
      }
    }
    if (file == null) {
      file = new File(Optional.ofNullable(Play.tmpDir).orElse(Play.appRoot), SERVER_HELPER_FIND_FILE_TMP_PATH_PREFIX + resource);
      if (file.exists())
          return file;
      try {
        if (resource.startsWith("/") && resource.length() > 1) {
          resource = resource.substring(1);
        }
        ClasspathResource cf = ClasspathResource.file(resource);

        try {
          synchronized (ServerHelper.class) {
            copyURLToFile(cf.url(), file);
          }
          logger.trace("Found {} in {} {}", resource, cf.url().getProtocol(), cf.getJarFilePath());
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      } catch (Exception ignored) {
        logger.trace("File {} not found on classpath", resource);
      }
    }
    return file;
  }

  @Nonnull
  @CheckReturnValue
  public String relativeUrl(String path, @Nullable String query) {
    return Stream.of(path, query).filter(s -> s != null && !s.isEmpty()).collect(joining("?"));
  }
}
