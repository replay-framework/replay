package play.server.netty4;

import io.netty.buffer.EmptyByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import org.junit.jupiter.api.Test;
import play.mvc.Http;

import java.net.InetSocketAddress;
import java.net.URISyntaxException;

import static io.netty.buffer.ByteBufAllocator.DEFAULT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PlayHandlerTest {

  private final PlayHandler playHandler = new PlayHandler(null, null);

  @Test
  public void parseRequest() throws URISyntaxException {
    ChannelHandlerContext ctx = mock();
    FullHttpRequest nettyRequest = mock();
    HttpHeaders headers = mock();
    when(nettyRequest.uri()).thenReturn("/login/to/shop?user=bob&pwd=secret");
    when(nettyRequest.method()).thenReturn(HttpMethod.GET);
    when(nettyRequest.headers()).thenReturn(headers);
    when(headers.get("Host")).thenReturn("site.eu:8080");
    when(headers.get("Content-Type")).thenReturn("text/json");
    Channel ch = mock();
    when(ch.remoteAddress()).thenReturn(new InetSocketAddress("192.168.0.10", 443));
    when(ctx.channel()).thenReturn(ch);
    when(nettyRequest.content()).thenReturn(new EmptyByteBuf(DEFAULT));
    
    Http.Request request = playHandler.parseRequest(ctx, nettyRequest);

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