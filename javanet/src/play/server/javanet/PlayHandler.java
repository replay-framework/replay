package play.server.javanet;

import com.google.common.net.HttpHeaders;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.Invocation;
import play.InvocationContext;
import play.Invoker;
import play.Play;
import play.data.binding.CachedBoundActionMethodArgs;
import play.db.jpa.JPA;
import play.exceptions.UnexpectedException;
import play.i18n.Messages;
import play.libs.MimeTypes;
import play.mvc.ActionInvoker;
import play.mvc.Http;
import play.mvc.Router;
import play.mvc.Scope;
import play.mvc.results.NotFound;
import play.mvc.results.RenderStatic;
import play.server.IpParser;
import play.server.ServerAddress;
import play.server.ServerHelper;
import play.templates.JavaExtensions;
import play.utils.ErrorsCookieCrypter;
import play.utils.Utils;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.net.HttpHeaders.CONTENT_TYPE;
import static com.google.common.net.HttpHeaders.COOKIE;
import static com.google.common.net.HttpHeaders.ETAG;
import static com.google.common.net.HttpHeaders.HOST;
import static com.google.common.net.HttpHeaders.IF_MODIFIED_SINCE;
import static com.google.common.net.HttpHeaders.IF_NONE_MATCH;
import static com.google.common.net.HttpHeaders.LAST_MODIFIED;
import static com.google.common.net.HttpHeaders.SET_COOKIE;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNullElse;
import static org.apache.commons.lang3.StringUtils.defaultString;
import static play.mvc.Http.Methods.GET;
import static play.mvc.Http.Methods.HEAD;
import static play.mvc.Http.StatusCode.BAD_REQUEST;
import static play.mvc.Http.StatusCode.INTERNAL_ERROR;
import static play.mvc.Http.StatusCode.NOT_FOUND;
import static play.mvc.Http.StatusCode.NOT_MODIFIED;

@ParametersAreNonnullByDefault
public class PlayHandler implements HttpHandler {
  private static final Logger logger = LoggerFactory.getLogger(PlayHandler.class);
  private static final Logger securityLogger = LoggerFactory.getLogger("security");
  private final IpParser ipParser = new IpParser();
  private final ServerHelper serverHelper = new ServerHelper();
  private final FileService fileService = new FileService();
  private final ErrorsCookieCrypter errorsCookieCrypter = new ErrorsCookieCrypter();
  private final Map<String, RenderStatic> staticPathsCache = new HashMap<>();

  private final Invoker invoker;
  private final ActionInvoker actionInvoker;

  PlayHandler(Invoker invoker, ActionInvoker actionInvoker) {
    this.invoker = invoker;
    this.actionInvoker = actionInvoker;
  }

  @Override
  public void handle(HttpExchange exchange) throws IOException {
    Http.Request request = new Http.Request();
    Http.Response response = new Http.Response();

    try {
      Http.Request.setCurrent(request);
      Http.Response.setCurrent(response);

      request = parseRequest(exchange);
      Http.Request.setCurrent(request);

      response.out = new ByteArrayOutputStream();
      response.direct = null;

      boolean raw = Play.pluginCollection.rawInvocation(request, response, null, Scope.RenderArgs.current(), null);
      if (raw) {
        copyResponse(exchange, request, response);
      }
      else {
        // Delegate to Play framework
        invoker.invoke(new JavaNetInvocation(request, response, exchange));
      }

    }
    catch (IllegalArgumentException ex) {
      logger.warn("Exception on request. serving 400 back", ex);
      serve400(ex, exchange);
    }
    catch (Exception ex) {
      serve500(ex, exchange, request, response);
    }
  }

  private void copyResponse(HttpExchange exchange, Http.Request request, Http.Response response) throws Exception {
    logger.trace("copyResponse: begin");

    sendContentType(exchange, response);
    addToResponse(exchange, response);

    File file = response.direct instanceof File ? (File) response.direct : null;
    InputStream is = response.direct instanceof InputStream ? (InputStream) response.direct : null;

    boolean keepAlive = isKeepAlive(exchange);
    if (file != null && file.isFile()) {
      addEtag(exchange, file);
      if (response.status == NOT_MODIFIED) {
        if (!keepAlive) {
          // TODO Close the connection when the whole content is written out.
        }
      }
      else {
        fileService.serve(file, exchange, request, response, keepAlive);
      }
    }
    else if (is != null) {
      if (!exchange.getRequestMethod().equals(HEAD) && exchange.getResponseCode() != NOT_MODIFIED) {
        // writeFuture = ctx.getChannel().write(new ChunkedStream(is));
      }
      else {
        is.close();
      }
      if (!keepAlive) {
        // writeFuture.addListener(ChannelFutureListener.CLOSE);
      }
    }
    else {
      writeResponse(exchange, response);
    }
    logger.trace("copyResponse: end");
  }

  private void sendContentType(HttpExchange exchange, Http.Response response) {
    if (response.out != null) {
      String contentType = serverHelper.getContentTypeValue(response);
      exchange.getResponseHeaders().set(CONTENT_TYPE, contentType);
    }
  }

  private void saveExceededSizeError(HttpExchange exchange, Http.Request request) {
    String warning = exchange.getRequestHeaders().getFirst(HttpHeaders.WARNING);
    String length = exchange.getRequestHeaders().getFirst(HttpHeaders.CONTENT_LENGTH);
    if (warning != null) {
      logger.trace("saveExceededSizeError: begin");

      try {
        StringBuilder error = new StringBuilder();
        error.append("\u0000");
        error.append("play.netty.maxContentLength");
        error.append(":");
        String size;
        try {
          size = JavaExtensions.formatSize(Long.parseLong(length));
        } catch (Exception e) {
          size = length + " bytes";
        }
        error.append(Messages.get(warning, size));
        error.append("\u0001");
        error.append(size);
        error.append("\u0000");
        Http.Cookie cookieErrors = request.cookies.get(Scope.COOKIE_PREFIX + "_ERRORS");
        if (cookieErrors != null && cookieErrors.value != null && !cookieErrors.value.isEmpty()) {
          try {
            String decryptErrors = errorsCookieCrypter.decrypt(URLDecoder.decode(cookieErrors.value, UTF_8));
            error.append(decryptErrors);
          } catch (RuntimeException e) {
            securityLogger.error("Failed to decrypt cookie {}: {}", Scope.COOKIE_PREFIX + "_ERRORS", cookieErrors.value, e);
          }
        }
        String errorData = URLEncoder.encode(errorsCookieCrypter.encrypt(error.toString()), UTF_8);
        Http.Cookie cookie = new Http.Cookie(Scope.COOKIE_PREFIX + "_ERRORS", errorData);
        request.cookies.put(Scope.COOKIE_PREFIX + "_ERRORS", cookie);
        logger.trace("saveExceededSizeError: end");
      } catch (RuntimeException e) {
        throw new UnexpectedException("Error serialization problem", e);
      }
    }
  }

  private void addToResponse(HttpExchange exchange, Http.Response response) {
    addHeadersToResponse(exchange, response.headers);
    addDateToResponse(exchange);
    addCookiesToResponse(exchange, response.cookies);
    addCacheControlToResponse(exchange, response);
  }

  private void addHeadersToResponse(HttpExchange exchange, Map<String, Http.Header> headers) {
    for (Map.Entry<String, Http.Header> entry : headers.entrySet()) {
      Http.Header hd = entry.getValue();
      for (String value : hd.values) {
        exchange.getResponseHeaders().add(entry.getKey(), value);
      }
    }
  }

  private void addDateToResponse(HttpExchange exchange) {
    exchange.getResponseHeaders().set(HttpHeaders.DATE, Utils.getHttpDateFormatter().format(new Date()));
  }

  private void flushCookies(HttpExchange exchange, Http.Response response) {
    try {
      addCookiesToResponse(exchange, response.cookies);

    } catch (Exception e) {
      logger.error("Failed to flush cookies", e);
    }
  }

  private void addCookiesToResponse(HttpExchange exchange, Map<String, Http.Cookie> cookies) {
    for (Http.Cookie cookie : cookies.values()) {
      exchange.getResponseHeaders().add(SET_COOKIE, ServerCookieEncoder.STRICT.encode(toNettyCookie(cookie)));
    }
  }

  @Nonnull
  private static Cookie toNettyCookie(Http.Cookie cookie) {
    Cookie c = new DefaultCookie(cookie.name, cookie.value);
    c.setSecure(cookie.secure);
    c.setPath(cookie.path);
    if (cookie.domain != null) {
      c.setDomain(cookie.domain);
    }
    if (cookie.maxAge != null) {
      c.setMaxAge(cookie.maxAge);
    }
    c.setHttpOnly(cookie.httpOnly);
    return c;
  }

  private void addCacheControlToResponse(HttpExchange exchange, Http.Response response) {
    if (!response.headers.containsKey(HttpHeaders.CACHE_CONTROL) && !response.headers.containsKey(HttpHeaders.EXPIRES) && !(response.direct instanceof File)) {
      exchange.getResponseHeaders().set(HttpHeaders.CACHE_CONTROL, "no-cache");
    }
  }

  private void writeResponse(HttpExchange exchange, Http.Response response) throws IOException {
    logger.trace("writeResponse: begin");

    boolean keepAlive = isKeepAlive(exchange);
    byte[] content = exchange.getRequestMethod().equals(HEAD) ? new byte[0] : response.out.toByteArray();

    logger.trace("writeResponse: content length [{}]", content.length);
    if (response.contentType != null) {
      exchange.getResponseHeaders().set("Content-Type", response.contentType);
    }
    exchange.sendResponseHeaders(response.status, content.length);
    try (OutputStream responseBody = exchange.getResponseBody()) {
      responseBody.write(content);
    }

    logger.trace("writeResponse: end");
  }

  private void addEtag(HttpExchange exchange, File file) throws IOException {
    if (Play.mode == Play.Mode.DEV) {
      exchange.getResponseHeaders().set(HttpHeaders.CACHE_CONTROL, "no-cache");
    }
    else {
      // Check if Cache-Control header is not set
      if (exchange.getResponseHeaders().get(HttpHeaders.CACHE_CONTROL) == null) {
        String maxAge = Play.configuration.getProperty("http.cacheControl", "3600");
        if ("0".equals(maxAge)) {
          exchange.getResponseHeaders().set(HttpHeaders.CACHE_CONTROL, "no-cache");
        }
        else {
          exchange.getResponseHeaders().set(HttpHeaders.CACHE_CONTROL, "max-age=" + maxAge);
        }
      }
    }
    boolean useEtag = "true".equals(Play.configuration.getProperty("http.useETag", "true"));
    long last = file.lastModified();
    String etag = "\"" + last + "-" + file.hashCode() + "\"";
    if (!isModified(etag, last, exchange)) {
      if (useEtag) {
        exchange.getResponseHeaders().set(ETAG, etag);
      }
      if (exchange.getRequestMethod().equals(GET)) {
        exchange.sendResponseHeaders(NOT_MODIFIED, -1);
      }
    }
    else {
      exchange.getResponseHeaders().set(LAST_MODIFIED, Utils.getHttpDateFormatter().format(new Date(last)));
      if (useEtag) {
        exchange.getResponseHeaders().set(ETAG, etag);
      }
    }
  }

  private boolean isModified(String etag, long last, HttpExchange exchange) {
    String ifNoneMatch = exchange.getRequestHeaders().getFirst(IF_NONE_MATCH);
    String ifModifiedSince = exchange.getRequestHeaders().getFirst(IF_MODIFIED_SINCE);
    return serverHelper.isModified(etag, last, ifNoneMatch, ifModifiedSince);
  }

  private boolean isKeepAlive(HttpExchange message) {
    return serverHelper.isKeepAlive(
      message.getProtocol(),
      message.getRequestHeaders().getFirst(HttpHeaders.CONNECTION)
    );
  }

  private class JavaNetInvocation extends Invocation {
    private final Http.Request request;
    private final Http.Response response;
    private final HttpExchange exchange;

    public JavaNetInvocation(Http.Request request, Http.Response response, HttpExchange exchange) {
      this.request = request;
      this.response = response;
      this.exchange = exchange;
    }

    @Override
    public boolean init() throws IOException {
      logger.trace("init: begin");

      Http.Request.setCurrent(request);
      Http.Response.setCurrent(response);
      Scope.RenderArgs.current.set(null);
      CachedBoundActionMethodArgs.init();

      try {
        if (Play.mode == Play.Mode.DEV) {
          Router.detectChanges();
        }
        if (Play.mode == Play.Mode.PROD
            && staticPathsCache.containsKey(request.domain + " " + request.method + " " + request.path)) {
          RenderStatic rs;
          synchronized (staticPathsCache) {
            rs = staticPathsCache.get(request.domain + " " + request.method + " " + request.path);
          }
          serveStatic(rs, exchange, request, response);
          logger.trace("init: end false");
          return false;
        }
        Router.instance.routeOnlyStatic(request);
        super.init();
      }
      catch (NotFound nf) {
        serve404(nf, exchange, request);
        logger.trace("init: end false");
        return false;
      }
      catch (RenderStatic rs) {
        if (Play.mode == Play.Mode.PROD) {
          synchronized (staticPathsCache) {
            staticPathsCache.put(request.domain + " " + request.method + " " + request.path, rs);
          }
        }
        serveStatic(rs, exchange, request, response);
        logger.trace("init: end false");
        return false;
      }

      logger.trace("init: end true");
      return true;
    }

    @Override
    public InvocationContext getInvocationContext() {
      ActionInvoker.resolve(request);
      return new InvocationContext(Http.invocationType, request.invokedMethod.getAnnotations(),
        request.invokedMethod.getDeclaringClass().getAnnotations());
    }

    @Override
    public void run() {
      try {
        logger.trace("run: begin");
        onStarted();
        try {
          preInit();
          if (init()) {
            before();
            JPA.withinFilter(() -> {
              execute();
              return null;
            });
            after();
            onSuccess();
          }
        }
        catch (Throwable e) {
          onActionInvocationException(request, response, e);
        }
        finally {
          Play.pluginCollection.onActionInvocationFinally(request, response);
          InvocationContext.current.remove();
        }
      }
      catch (Exception e) {
        serve500(e, exchange, request, response);
      }
      finally {
        exchange.close();
      }
      logger.trace("run: end");
    }

    @Override
    public void execute() {
      // Check the exceeded size before rendering, so we can render the
      // error if the size is exceeded
      saveExceededSizeError(exchange, request);
      actionInvoker.invoke(request, response);
    }

    @Override
    public void onSuccess() throws Exception {
      super.onSuccess();
      logger.trace("onSuccess: begin");
      copyResponse(exchange, request, response);
      logger.trace("onSuccess: end");
    }
  }

  Http.Request parseRequest(HttpExchange exchange) throws IOException {
    logger.trace("parseRequest: begin, URI = {}", exchange.getRequestURI());

    URI uri = exchange.getRequestURI();
    String relativeUrl = serverHelper.relativeUrl(uri.getPath(), uri.getQuery());
    String contentType = exchange.getRequestHeaders().getFirst(CONTENT_TYPE);
    String querystring = uri.getQuery();
    String path = uri.getPath();
    String remoteAddress = getRemoteIPAddress(exchange);
    String method = exchange.getRequestMethod();

    // TODO Check max upload size:
    // int max = Integer.parseInt(Play.configuration.getProperty("play.netty.maxContentLength", "-1"));

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    IOUtils.copy(exchange.getRequestBody(), out); // TODO What if upload is too big? Avoid loading the entire array to memory (?)
    byte[] n = out.toByteArray();
    InputStream body = new ByteArrayInputStream(n);

    String host = exchange.getRequestHeaders().getFirst(HOST);
    boolean isLoopback = ipParser.isLoopback(host, exchange.getRemoteAddress());
    ServerAddress serverAddress = ipParser.parseHost(host);

    Http.Request request = Http.Request.createRequest(remoteAddress, method, path, querystring, contentType, body, relativeUrl,
      serverAddress.host, isLoopback, serverAddress.port, serverAddress.domain,
        getHeaders(exchange), getCookies(exchange));

    logger.trace("parseRequest: end");
    return request;
  }

  private String getRemoteIPAddress(HttpExchange exchange) {
    return ipParser.getRemoteIpAddress(exchange.getRemoteAddress());
  }

  private Map<String, Http.Header> getHeaders(HttpExchange exchange) {
    Set<Map.Entry<String, List<String>>> entries = exchange.getRequestHeaders().entrySet();

    Map<String, Http.Header> headers = new HashMap<>(entries.size());
    for (Map.Entry<String, List<String>> header : entries) {
      List<String> headerValues = unmodifiableList(header.getValue());
      Http.Header hd = new Http.Header(header.getKey().toLowerCase(), headerValues);
      headers.put(hd.name, hd);
    }

    return headers;
  }

  private Map<String, Http.Cookie> getCookies(HttpExchange exchange) {
    Map<String, Http.Cookie> cookies = new HashMap<>(16);
    String value = exchange.getRequestHeaders().getFirst(COOKIE);
    if (value != null) {
      Set<Cookie> cookieSet = ServerCookieDecoder.STRICT.decode(value);
      for (Cookie cookie : cookieSet) {
        Http.Cookie playCookie = new Http.Cookie(cookie.name(), cookie.value());
        playCookie.path = cookie.path();
        playCookie.domain = cookie.domain();
        playCookie.secure = cookie.isSecure();
        playCookie.httpOnly = cookie.isHttpOnly();
        cookies.put(playCookie.name, playCookie);
      }
    }
    return cookies;
  }

  private void serve400(Exception e, HttpExchange exchange) throws IOException {
    logger.trace("serve400: begin");
    printResponse(exchange, BAD_REQUEST, "text/plain", e.getMessage() + '\n');
    logger.trace("serve400: end");
  }

  private void serve404(NotFound e, HttpExchange exchange, Http.Request request) throws IOException {
    logger.trace("serve404: begin");
    String format = defaultString(request.format, "txt");
    String contentType = MimeTypes.getContentType("404." + format, "text/plain");
    String errorHtml = serverHelper.generateNotFoundResponse(request, format, e);
    printResponse(exchange, NOT_FOUND, contentType, errorHtml);
    logger.trace("serve404: end");
  }

  private void printResponse(HttpExchange exchange, int httpStatus, String contentType, String errorHtml) throws IOException {
    byte[] bytes = errorHtml.getBytes(Play.defaultWebEncoding);
    exchange.getResponseHeaders().set(CONTENT_TYPE, contentType);
    exchange.sendResponseHeaders(httpStatus, bytes.length);
    try (OutputStream out = exchange.getResponseBody()) {
      out.write(bytes);
    }
  }

  private void serve500(Exception e, HttpExchange exchange, Http.Request request, Http.Response response) {
    logger.trace("serve500: begin");

    try {
      flushCookies(exchange, response);

      String format = requireNonNullElse(request.format, "txt");
      String contentType = MimeTypes.getContentType("500." + format, "text/plain");

      try {
        String errorHtml = serverHelper.generateErrorResponse(request, format, e);
        printResponse(exchange, INTERNAL_ERROR, contentType, errorHtml);
        logger.error("Internal Server Error (500) for {} {} ({})", request.method, request.url, e.getClass().getSimpleName(), e);
      }
      catch (Throwable ex) {
        logger.error("Internal Server Error (500) for {} {} ({})", request.method, request.url, e.getClass().getSimpleName(), e);
        logger.error("Error during the 500 response generation", ex);
        sendServerError(exchange, request);
      }
    }
    catch (RuntimeException exxx) {
      logger.error("Error during the 500 response generation", exxx);
      try {
        sendServerError(exchange, request);
      }
      catch (Exception fex) {
        logger.error("(encoding ?)", fex);
      }
      throw exxx;
    }
    finally {
      exchange.close();
    }
    logger.trace("serve500: end");
  }

  private void serveStatic(RenderStatic renderStatic, HttpExchange exchange, Http.Request request, Http.Response response) {
    logger.trace("serveStatic: begin");

    try {
      File file = serverHelper.findFile(renderStatic.file);
      if ((file == null || !file.exists())) {
        serve404(new NotFound("The file " + renderStatic.file + " does not exist"), exchange, request);
      } else {
        serveLocalFile(file, request, response, exchange);
      }
    } catch (Throwable ez) {
      logger.error("serveStatic for request {} {}", request.method, request.url, ez);
      sendServerError(exchange, request);
    }
    logger.trace("serveStatic: end");
  }

  private void sendServerError(HttpExchange exchange, Http.Request request) {
    try {
      printResponse(exchange, INTERNAL_ERROR, "text/plain", "Internal Error");
    } catch (IOException ex) {
      logger.error("serveStatic for request {} {}", request.method, request.url, ex);
    }
  }

  private void serveLocalFile(File localFile, Http.Request request, Http.Response response,
                              HttpExchange exchange) throws IOException {

    boolean keepAlive = isKeepAlive(exchange);
    addEtag(exchange, localFile);

    if (exchange.getResponseCode() != NOT_MODIFIED) {
      fileService.serve(localFile, exchange, request, response, keepAlive);
    }
  }
}

