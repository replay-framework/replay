package play.modules.logger;

import org.junit.Test;
import org.slf4j.MDC;
import play.mvc.Http;
import play.mvc.Http.Cookie;

import java.io.ByteArrayInputStream;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;

public class RequestLogPluginTest {
  private final RequestLogPlugin plugin = new RequestLogPlugin();

  @Test
  public void routeRequest() {
    Http.Request request = request();
    plugin.routeRequest(request);

    assertThat(request.args.get("startTime")).isNotNull();
    assertThat(request.args.get("requestId")).isNotNull();
    assertThat(MDC.get("requestId")).isEqualTo(request.args.get("requestId"));
  }

  private Http.Request request() {
    return request("/path", emptyMap());
  }

  private Http.Request request(String path, Map<String, Http.Header> headers) {
    Http.Request request = Http.Request.createRequest("127.0.0.1", "GET", path, "", "text/html", new ByteArrayInputStream("".getBytes(UTF_8)), "url", "host", false, 80, "domain",
      true, headers, Map.of("PLAY_SESSIONID", new Cookie("PLAY_SESSIONID", "session-007")));
    request.action = "action";
    request.actionMethod = "method";
    request.controller = "controller";
    return request;
  }
}