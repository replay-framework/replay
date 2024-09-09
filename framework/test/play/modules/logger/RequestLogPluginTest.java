package play.modules.logger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.MDC;
import play.exceptions.TemplateNotFoundException;
import play.mvc.Http;
import play.mvc.Http.Cookie;
import play.mvc.Scope;
import play.mvc.results.RenderJson;
import play.mvc.results.Result;

import java.io.ByteArrayInputStream;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class RequestLogPluginTest {
  private final Logger logger = mock();
  private final RequestLogPlugin plugin = new RequestLogPlugin(logger);
  private final Http.Response response = mock();
  private final Scope.RenderArgs renderArgs = mock();
  private final Scope.Session session = mock();
  private final Scope.Flash flash = new Scope.Flash();

  @BeforeEach
  void setUp() {
    when(logger.isInfoEnabled()).thenReturn(true);
    when(session.getId()).thenReturn("sid12345");
  }

  @Test
  public void routeRequest() {
    Http.Request request = request();
    plugin.routeRequest(request);

    assertThat(request.args.get("startTime")).isNotNull();
    assertThat(request.args.get("requestId")).isNotNull();
    assertThat(MDC.get("requestId")).isEqualTo(request.args.get("requestId"));
  }

  @Test
  void logsSuccessfulAction() {
    Http.Request request = request();
    RenderJson result = new RenderJson("{\"ok\": true}");
    plugin.beforeActionInvocation(request, response, session, renderArgs, flash, null);
    plugin.onActionInvocationResult(request, response, session, flash, renderArgs, result);
    plugin.onActionInvocationFinally(request, response);

    verify(logger).info("GET	/path	192.168.3.4:sid12345	->	RenderJson {\"ok\": true}");
    assertThat(request.args).containsExactlyInAnyOrderEntriesOf(Map.of(
      "sessionId", "sid12345",
      Result.class.getName(), result
    ));
  }

  @Test
  void logsFailedAction() {
    Http.Request request = request();
    TemplateNotFoundException failure = new TemplateNotFoundException("/missing-template.html");
    plugin.beforeActionInvocation(request, response, session, renderArgs, flash, null);
    plugin.onActionInvocationException(request, response, failure);
    plugin.onActionInvocationFinally(request, response);

    verify(logger).info("GET	/path	192.168.3.4:sid12345	->	Error \"play.exceptions.TemplateNotFoundException: Template not found : /missing-template.html\"");
    assertThat(request.args.get(Result.class.getName())).isInstanceOf(play.mvc.results.Result.class);
    assertThat(request.args.get(Result.class.getName())).hasToString("Error \"play.exceptions.TemplateNotFoundException: Template not found : /missing-template.html\"");
  }

  private Http.Request request() {
    return request("/path", emptyMap());
  }

  private Http.Request request(String path, Map<String, Http.Header> headers) {
    Http.Request request = Http.Request.createRequest("127.0.0.1", "GET", path, "", "text/html", new ByteArrayInputStream("".getBytes(UTF_8)), "url", "host", false, 80, "domain",
        headers, Map.of("PLAY_SESSIONID", new Cookie("PLAY_SESSIONID", "session-007")));
    request.action = "action";
    request.actionMethod = "method";
    request.controller = "controller";
    request.remoteAddress = "192.168.3.4";
    request.port = 8080;
    return request;
  }
}