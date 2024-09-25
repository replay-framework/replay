package play.server;

import static java.nio.charset.StandardCharsets.UTF_16;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.Charset;
import org.junit.jupiter.api.Test;
import play.mvc.Http;

public class ServerHelperTest {
  private final ServerHelper helper = new ServerHelper();

  @Test
  public void isKeepAlive_http_1_0() {
    assertThat(helper.isKeepAlive("HTTP/1.0", "close")).isFalse();
    assertThat(helper.isKeepAlive("HTTP/1.0", "keep-alive")).isTrue();
    assertThat(helper.isKeepAlive("HTTP/1.0", ""))
        .as("By default keep-alive not enabled")
        .isFalse();
  }

  @Test
  public void isKeepAlive_http_1_1() {
    assertThat(helper.isKeepAlive("HTTP/1.1", "close")).isFalse();
    assertThat(helper.isKeepAlive("HTTP/1.1", "keep-alive")).isTrue();
    assertThat(helper.isKeepAlive("HTTP/1.1", "")).as("By default keep-alive is enabled").isTrue();
  }

  @Test
  public void relativeUrl() {
    assertThat(helper.relativeUrl("/", null)).isEqualTo("/");
    assertThat(helper.relativeUrl("/", "bob=secret")).isEqualTo("/?bob=secret");
  }

  @Test
  public void getContentTypeValue() {
    assertThat(helper.getContentTypeValue(response("", UTF_8)))
        .isEqualTo("text/plain; charset=UTF-8");
    assertThat(helper.getContentTypeValue(response("text/plain", UTF_8)))
        .isEqualTo("text/plain; charset=UTF-8");
    assertThat(helper.getContentTypeValue(response("text/plain", UTF_16)))
        .isEqualTo("text/plain; charset=UTF-16");
    assertThat(helper.getContentTypeValue(response("text/plain; charset=UTF-128", UTF_8)))
        .isEqualTo("text/plain; charset=UTF-128");
    assertThat(helper.getContentTypeValue(response("application/excel", UTF_8)))
        .isEqualTo("application/excel");
  }

  private static Http.Response response(String contentType, Charset encoding) {
    Http.Response response = new Http.Response();
    response.contentType = contentType;
    response.encoding = encoding;
    return response;
  }
}
