package play.server.netty3;

import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.junit.jupiter.api.Test;
import play.mvc.Http;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URISyntaxException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jboss.netty.buffer.ChannelBuffers.EMPTY_BUFFER;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PlayHandlerTest {

  private final PlayHandler playHandler = new PlayHandler(null, null);

  @Test
  public void parseRequest() throws IOException, URISyntaxException {
    MessageEvent message = mock();
    HttpRequest nettyRequest = mock();
    HttpHeaders headers = mock();
    when(nettyRequest.getUri()).thenReturn("/login/to/shop?user=bob&pwd=secret");
    when(nettyRequest.getMethod()).thenReturn(HttpMethod.GET);
    when(nettyRequest.headers()).thenReturn(headers);
    when(headers.get("Host")).thenReturn("site.eu:8080");
    when(headers.get("Content-Type")).thenReturn("text/json");
    when(nettyRequest.getContent()).thenReturn(EMPTY_BUFFER);
    when(message.getRemoteAddress()).thenReturn(new InetSocketAddress("192.168.0.10", 443));
    
    Http.Request request = playHandler.parseRequest(nettyRequest, message);

    assertThat(request.host).isEqualTo("site.eu:8080");
    assertThat(request.url).isEqualTo("/login/to/shop?user=bob&pwd=secret");
    assertThat(request.method).isEqualTo("GET");
    assertThat(request.domain).isEqualTo("site.eu");
    assertThat(request.path).isEqualTo("/login/to/shop");
    assertThat(request.querystring).isEqualTo("user=bob&pwd=secret");
    assertThat(request.remoteAddress).isEqualTo("192.168.0.10");
    assertThat(request.contentType).isEqualTo("text/json");
    assertThat(request.port).isEqualTo(8080);
    assertThat(request.secure).isEqualTo(false);
  }
}