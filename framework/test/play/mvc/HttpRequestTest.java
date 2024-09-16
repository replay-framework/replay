package play.mvc;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import play.libs.Codec;

public class HttpRequestTest {

  @Test
  public void testBasicAuth() {
    String encoded = Codec.encodeBASE64("username:pass:wordwithcolon");
    Http.Header header = new Http.Header("authorization", "Basic " + encoded);
    Map<String, Http.Header> headers = new HashMap<>();
    headers.put("authorization", header);

    //This used to throw an exception if there was a colon in the password
    // test with currentRequest
    Http.Request request = request(headers);

    assertThat(request.user).isEqualTo("username");
    assertThat(request.password).isEqualTo("pass:wordwithcolon");
  }

  @Test
  public void userAgent() {
    Http.Request request =
        request(Map.of("user-agent", new Http.Header("user-agent", "Firefox 0.50-beta")));

    assertThat(request.getUserAgent()).isEqualTo("Firefox 0.50-beta");
  }

  @Test
  public void userAgent_unknown() {
    assertThat(request(emptyMap()).getUserAgent()).isEqualTo("n/a");
  }

  private Http.Request request(Map<String, Http.Header> headers) {
    return Http.Request.createRequest(
        null, "GET", "/", "", null, null, null, null, false, 80, "localhost", headers, null);
  }
}
