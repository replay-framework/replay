package play.libs.ws;

import play.mvc.Http;

import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;

public class DummyHttpResponse extends HttpResponse {
  private final int status;
  private final String body;
  private final List<Http.Header> headers;

  public DummyHttpResponse(int status, String body, Http.Header... headers) {
    this.status = status;
    this.body = body;
    this.headers = asList(headers);
  }

  @Override public Integer getStatus() {
    return status;
  }

  @Override public String getStatusText() {
    return "";
  }

  @Override @Nullable
  public String getHeader(String name) {
    return headers.stream()
      .filter(h -> h.name.equals(name))
      .map(h -> h.value())
      .findAny()
      .orElse(null);
  }

  @Override public List<Http.Header> getHeaders() {
    return headers;
  }

  @Override public String getString() {
    return body;
  }

  @Override public String getString(Charset encoding) {
    return body;
  }

  @Override public InputStream getStream() {
    return new ByteArrayInputStream(body.getBytes(UTF_8));
  }

  @Override public String toString() {
    return String.format("%s %s", status, body);
  }
}
