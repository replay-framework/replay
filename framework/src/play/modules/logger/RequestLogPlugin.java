package play.modules.logger;

import static java.lang.String.format;
import static java.lang.System.nanoTime;
import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import play.PlayPlugin;
import play.mvc.Http.Header;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.mvc.Scope.Flash;
import play.mvc.Scope.RenderArgs;
import play.mvc.Scope.Session;
import play.mvc.results.NotFound;
import play.mvc.results.Result;

@ParametersAreNonnullByDefault
public class RequestLogPlugin extends PlayPlugin {
  private static final String REQUEST_ID_PREFIX =
      Integer.toHexString((int) (Math.random() * 0x1000));
  private static final AtomicLong counter = new AtomicLong(1);
  private static final Pattern REGEX_CLEAN_PARAM_VALUE = Pattern.compile("\r?\n");
  private final Logger logger;

  RequestLogPlugin() {
    this(LoggerFactory.getLogger("request"));
  }

  RequestLogPlugin(Logger logger) {
    this.logger = logger;
  }

  @Override
  public void routeRequest(Request request) {
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

  @Override
  public void beforeActionInvocation(
      Request request,
      Response response,
      @Nullable Session session,
      RenderArgs renderArgs,
      Flash flash,
      Method actionMethod) {
    Object requestId = request.args.get("requestId");
    String sessionId = session == null ? "no-session" : session.getId();
    request.args.put("sessionId", sessionId);

    logger.debug("{}\t{} ...", request.method, request.path);

    Thread.currentThread()
        .setName(
            format(
                "%s %s [%s] (%s %s)",
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
  public void onActionInvocationResult(
      @Nonnull Request request,
      @Nonnull Response response,
      @Nonnull Session session,
      @Nonnull Flash flash,
      @Nonnull RenderArgs renderArgs,
      @Nonnull Result result) {
    request.args.put(Result.class.getName(), result);
  }

  @Override
  public void onActionInvocationException(
      @Nonnull Request request, @Nonnull Response response, @Nonnull Throwable e) {
    if (e.getCause() != null) e = e.getCause();
    request.args.put(Result.class.getName(), new play.mvc.results.Error(e.toString()));
  }

  @Override
  public void onActionInvocationFinally(@Nonnull Request request, @Nonnull Response response) {
    if (request.action == null) return;

    try {
      Result result;
      if (request.actionMethod == null) result = new NotFound(request.path);
      else result = (Result) request.args.get(Result.class.getName());

      logRequestInfo(request, result);
    } finally {
      Thread.currentThread().setName(getOriginalThreadName());
      MDC.remove("requestId");
    }
  }

  private void logRequestInfo(Request request, Result result) {
    if (logger.isInfoEnabled()) {
      StringBuilder sb =
          new StringBuilder()
              .append(request.method)
              .append('\t')
              .append(request.path)
              .append('\t')
              .append(request.remoteAddress)
              .append(':')
              .append(request.args.get("sessionId"))
              .append('\t');

      appendIfNotEmpty(sb, extractParams(request));

      sb.append("->\t").append(result(result));

      Long start = (Long) request.args.get("startTime");
      if (start != null)
        sb.append('\t').append(NANOSECONDS.toMillis(nanoTime() - start)).append(" ms");

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

  private static void appendIfNotEmpty(StringBuilder sb, String s) {
    if (isNotEmpty(s)) sb.append(s).append('\t');
  }

  static String result(@Nullable Result result) {
    return result == null ? "RenderError" : result.toString();
  }

  private static final Set<String> SKIPPED_PARAMS =
      new HashSet<>(asList("action", "controller", "body", "action", "controller"));
  private static final Integer EXCERPT_LENGTH = 100;

  private String extractParams(Request request) {
    try {
      return extractParamsUnsafe(request);
    } catch (Exception e) {
      logger.error(
          format(
              "Failed to parse request params, encoding: %s , headers: %s",
              request.encoding, request.headers),
          e);
      return "";
    }
  }

  private static String extractParamsUnsafe(Request request) {
    StringBuilder sb = new StringBuilder();
    for (Map.Entry<String, String[]> param : request.params.all().entrySet()) {
      String name = param.getKey();
      if (SKIPPED_PARAMS.contains(name)) continue;
      String value = cleanup(name, getParamValue(param.getValue()));
      sb.append('\t').append(name).append('=').append(value);
    }
    return sb.toString().trim();
  }

  private static final Set<String> INTERNAL_PARAMS = Set.of("authenticityToken", "___form_id");

  private static String cleanup(String name, String paramValue) {
    String excerpt = excerpt(paramValue, INTERNAL_PARAMS.contains(name) ? 10 : EXCERPT_LENGTH);
    return REGEX_CLEAN_PARAM_VALUE.matcher(excerpt).replaceAll("\\\\n");
  }

  private static String getParamValue(String[] param) {
    if (param.length == 1) return param[0];
    else return Arrays.toString(param);
  }

  private static String excerpt(String value, int excerptLength) {
    return value.length() > excerptLength
        ? String.format(
            "%s...%s", value.substring(0, excerptLength - 5), value.substring(value.length() - 5))
        : value;
  }
}
