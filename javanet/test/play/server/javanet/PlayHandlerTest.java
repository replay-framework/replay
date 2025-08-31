package play.server.javanet;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import play.mvc.Http;

public class PlayHandlerTest {

  private final HttpExchange exchange = mock();
  private final Headers headers = mock();
  private final PlayHandler playHandler = new PlayHandler(null, null);

  @ParameterizedTest
  @CsvSource({
      "/login/to/shop, World, World",
      "/login/to/Hyper%20Mall, World, World",
      "/login/to/shop, Good+morning, Good morning",
      "/login/to/shop, Good%2Bmorning, Good+morning",
  })
  public void parseRequest(String path, String encodedParameterValue, String parameterValue) throws URISyntaxException, IOException {
    when(exchange.getRequestURI()).thenReturn(new URI("%s?user=bob&pwd=secret&greeting=%s".formatted(path, encodedParameterValue)));
    when(exchange.getRequestMethod()).thenReturn("GET");
    when(exchange.getRequestHeaders()).thenReturn(headers);
    when(headers.getFirst("Host")).thenReturn("site.eu:8080");
    when(headers.getFirst("Content-Type")).thenReturn("text/json");
    when(exchange.getRemoteAddress()).thenReturn(new InetSocketAddress("192.168.0.10", 443));
    when(exchange.getRequestBody())
        .thenReturn(new ByteArrayInputStream("I am request body".getBytes(UTF_8)));

    Http.Request request = playHandler.parseRequest(exchange);

    assertThat(request.host).isEqualTo("site.eu:8080");
    assertThat(request.url).isEqualTo(path + "?user=bob&pwd=secret&greeting=" + encodedParameterValue);
    assertThat(request.method).isEqualTo("GET");
    assertThat(request.domain).isEqualTo("site.eu");
    assertThat(request.path).isEqualTo(path);
    assertThat(request.querystring).isEqualTo("user=bob&pwd=secret&greeting=" + encodedParameterValue);
    assertThat(request.params.all()).hasSize(4);
    assertThat(request.params.get("user")).isEqualTo("bob");
    assertThat(request.params.get("pwd")).isEqualTo("secret");
    assertThat(request.params.get("greeting")).isEqualTo(parameterValue);
    assertThat(request.params.get("body")).isEqualTo("I am request body");
    assertThat(request.remoteAddress).isEqualTo("192.168.0.10");
    assertThat(request.contentType).isEqualTo("text/json");
    assertThat(request.port).isEqualTo(8080);
    assertThat(request.isSecure()).isEqualTo(false);
  }
}
