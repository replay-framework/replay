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
import play.mvc.Http;

public class PlayHandlerTest {

  private final PlayHandler playHandler = new PlayHandler(null, null);

  @Test
  public void parseRequest() throws URISyntaxException, IOException {
    HttpExchange exchange = mock();
    Headers headers = mock();
    when(exchange.getRequestURI()).thenReturn(new URI("/login/to/shop?user=bob&pwd=secret"));
    when(exchange.getRequestMethod()).thenReturn("GET");
    when(exchange.getRequestHeaders()).thenReturn(headers);
    when(headers.getFirst("Host")).thenReturn("site.eu:8080");
    when(headers.getFirst("Content-Type")).thenReturn("text/json");
    when(exchange.getRemoteAddress()).thenReturn(new InetSocketAddress("192.168.0.10", 443));
    when(exchange.getRequestBody())
        .thenReturn(new ByteArrayInputStream("I am request body".getBytes(UTF_8)));

    Http.Request request = playHandler.parseRequest(exchange);

    assertThat(request.host).isEqualTo("site.eu:8080");
    assertThat(request.url).isEqualTo("/login/to/shop?user=bob&pwd=secret");
    assertThat(request.method).isEqualTo("GET");
    assertThat(request.domain).isEqualTo("site.eu");
    assertThat(request.path).isEqualTo("/login/to/shop");
    assertThat(request.querystring).isEqualTo("user=bob&pwd=secret");
    assertThat(request.remoteAddress).isEqualTo("192.168.0.10");
    assertThat(request.contentType).isEqualTo("text/json");
    assertThat(request.port).isEqualTo(8080);
    assertThat(request.isSecure()).isEqualTo(false);
  }
}
