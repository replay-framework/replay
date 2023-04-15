package play.modules.logger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import play.PlayPlugin;
import play.mvc.Http;
import play.mvc.Http.Header;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.mvc.Scope.Flash;
import play.mvc.Scope.RenderArgs;
import play.mvc.Scope.Session;
import play.mvc.results.NotFound;
import play.mvc.results.Result;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static java.lang.String.format;
import static java.lang.System.nanoTime;
import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.split;
import static play.mvc.Scope.COOKIE_PREFIX;

@ParametersAreNonnullByDefault
public class RequestLogPlugin extends PlayPlugin {
  private static final String REQUEST_ID_PREFIX = Integer.toHexString((int) (Math.random() * 0x1000));
  private static final AtomicLong counter = new AtomicLong(1);
  private static final Logger logger = LoggerFactory.getLogger("request");

  @Override public void routeRequest(Request request) {
    String requestId = REQUEST_ID_PREFIX + "-" + nextId();
    request.args.put("startTime", nanoTime());
    request.args.put("requestId", requestId);
    MDC.put("requestId", requestId);
  }

  private static long nextId() {
    long requestNumber = counter.incrementAndGet();
    if (requestNumber <= 0) {
      counter.set(0);
    }
    return requestNumber;
  }

  @Override public void beforeActionInvocation(Request request, Response response, Session session,
                                               RenderArgs renderArgs, Flash flash, Method actionMethod) {
    Object requestId = request.args.get("requestId");
    String sessionId = session == null ? "no-session" : session.getId();

    logger.debug("{}\t{} ...", request.method, request.path);

    Thread.currentThread().setName(format("%s %s [%s] (%s %s)",
      getOriginalThreadName(),
      request.action,
      requestId,
      request.remoteAddress,
      sessionId));
  }

  private String getOriginalThreadName() {
    String name = Thread.currentThread().getName();
    int i = name.indexOf(' ');
    return i == -1 ? name : name.substring(0, i);
  }

  @Override
  public void onActionInvocationResult(@Nonnull Request request, @Nonnull Response response,
                                       @Nonnull Session session, @Nonnull Flash flash,
                                       @Nonnull RenderArgs renderArgs, @Nonnull Result result) {
    request.args.put(Result.class.getName(), result);
  }

  @Override
  public void onActionInvocationException(@Nonnull Request request, @Nonnull Response response, @Nonnull Throwable e) {
    if (e.getCause() != null) e = e.getCause();
    request.args.put(Result.class.getName(), new Error(e.toString()));
  }

  @Override public void onActionInvocationFinally(@Nonnull Request request) {
    if (request.action == null) return;

    try {
      Result result;
      if (request.actionMethod == null)
        result = new NotFound(request.path);
      else
        result = (Result) request.args.get(Result.class.getName());

      logRequestInfo(request, result);
    }
    finally {
      Thread.currentThread().setName(getOriginalThreadName());
      MDC.remove("requestId");
    }
  }

  public static void logRequestInfo(Request request, Result result) {
    if (logger.isInfoEnabled()) {
      StringBuilder sb = new StringBuilder()
        .append(request.method).append('\t')
        .append(request.path).append('\t')
        .append(request.remoteAddress).append(':')
        .append(getSessionId(request)).append('\t');

      appendIfNotEmpty(sb, extractParams(request));

      sb.append("->\t").append(result(result));

      Long start = (Long) request.args.get("startTime");
      if (start != null) sb.append('\t').append(NANOSECONDS.toMillis(nanoTime() - start)).append(" ms");

      appendHeader(request, sb, "referer");
      appendHeader(request, sb, "origin");

      logger.info(sb.toString());
    }
  }

  private static void appendHeader(Request request, StringBuilder sb, String headerName) {
    Header referer = request.headers.get(headerName);
    if (referer != null) {
      sb.append('\t').append(headerName).append('=').append(referer.value());
    }
  }

  private static String getSessionId(Request request) {
    // TODO Remove duplication with CookieSessionStore
    String cookieName = COOKIE_PREFIX + "_SESSION";
    Http.Cookie cookie = request.cookies.get(cookieName);

    if (cookie != null && isNotEmpty(cookie.value)) {
      String[] parts = split(cookie.value, ':');
      return parts[0];
    }
    return null;
  }

  private static void appendIfNotEmpty(StringBuilder sb, String s) {
    if (isNotEmpty(s)) sb.append(s).append('\t');
  }

  static String result(Result result) {
    return result == null ? "RenderError" : result.toString();
  }

  private static final Set<String> SKIPPED_PARAMS = new HashSet<>(asList("action", "controller", "body", "action", "controller"));

  private static Integer EXCERPT_LENGTH = 100;

  public static String extractParams(Request request) {
    try {
      return extractParamsUnsafe(request);
    }
    catch (Exception e) {
      logger.error(format("Failed to parse request params, encoding: %s , headers: %s", request.encoding, request.headers), e);
      return "";
    }
  }

  private static String extractParamsUnsafe(Request request) {
    StringBuilder sb = new StringBuilder();
    for (Map.Entry<String, String[]> param : request.params.all().entrySet()) {
      String name = param.getKey();
      if (SKIPPED_PARAMS.contains(name)) continue;
      sb.append('\t').append(name).append('=');
      @Nullable String value = getParamValue(param.getValue());
      value = excerpt(value);

      sb.append(value != null ? value.replaceAll("\r?\n", "\\\\n") : null);
    }
    return sb.toString().trim();
  }

  private static String getParamValue(String[] param) {
    if (param.length == 1)
      return param[0];
    else
      return Arrays.toString(param);
  }

  private static String excerpt(String value) {
    return value.length() > EXCERPT_LENGTH ? value.substring(0, EXCERPT_LENGTH) + "..." : value;
  }
}
