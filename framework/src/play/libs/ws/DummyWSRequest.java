package play.libs.ws;

import static java.util.Collections.emptyMap;
import static play.libs.ws.HttpMethod.DELETE;
import static play.libs.ws.HttpMethod.GET;
import static play.libs.ws.HttpMethod.HEAD;
import static play.libs.ws.HttpMethod.OPTIONS;
import static play.libs.ws.HttpMethod.PATCH;
import static play.libs.ws.HttpMethod.POST;
import static play.libs.ws.HttpMethod.PUT;
import static play.libs.ws.HttpMethod.TRACE;

import java.util.HashMap;
import java.util.Map;
import play.Play;

public class DummyWSRequest extends WSRequest {
  private final Map<String, Map<HttpMethod, DummyWSRequest>> requests;
  private final Map<String, Map<HttpMethod, HttpResponse>> responses;

  public DummyWSRequest(
      String url,
      Map<String, Map<HttpMethod, DummyWSRequest>> requests,
      Map<String, Map<HttpMethod, HttpResponse>> responses) {
    super(url, Play.defaultWebEncoding);
    this.requests = requests;
    this.responses = responses;
  }

  @Override
  public HttpResponse get() {
    return forMethod(GET);
  }

  @Override
  public HttpResponse patch() {
    return forMethod(PATCH);
  }

  @Override
  public HttpResponse post() {
    return forMethod(POST);
  }

  @Override
  public HttpResponse put() {
    return forMethod(PUT);
  }

  @Override
  public HttpResponse delete() {
    return forMethod(DELETE);
  }

  @Override
  public HttpResponse options() {
    return forMethod(OPTIONS);
  }

  @Override
  public HttpResponse head() {
    return forMethod(HEAD);
  }

  @Override
  public HttpResponse trace() {
    return forMethod(TRACE);
  }

  private void rememberRequest(HttpMethod get) {
    requests.computeIfAbsent(url, (u) -> new HashMap<>()).put(get, this);
  }

  private Map<HttpMethod, HttpResponse> forURL() {
    return responses.getOrDefault(url, emptyMap());
  }

  private HttpResponse forMethod(HttpMethod method) {
    rememberRequest(method);

    HttpResponse mockedResponse = forURL().get(method);
    if (mockedResponse == null) return notFound(method);
    return mockedResponse;
  }

  private DummyHttpResponse notFound(HttpMethod method) {
    return new DummyHttpResponse(
        404, "Not found: " + method + " " + url + ", expected responses: " + responses);
  }
}
