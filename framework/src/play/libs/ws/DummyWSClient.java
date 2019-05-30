package play.libs.ws;

import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.emptyMap;

public class DummyWSClient extends WSAsync {
  private final Map<String, Map<HttpMethod, DummyWSRequest>> requests = new HashMap<>();
  private final Map<String, Map<HttpMethod, HttpResponse>> responses = new HashMap<>();

  public void replyWith(String url, HttpMethod method, int status, String body) {
    this.responses
      .computeIfAbsent(url, u -> new HashMap<>())
      .put(method, new DummyHttpResponse(status, body));
  }

  @Override
  public WSRequest newRequest(String url) {
    return new DummyWSRequest(url, requests, responses);
  }

  public DummyWSRequest actualRequest(String url, HttpMethod method) {
    return requests.getOrDefault(url, emptyMap()).get(method);
  }
}
