package play.server.netty4;

import io.netty.buffer.EmptyByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import play.mvc.Http;

import java.net.InetSocketAddress;
import java.net.URISyntaxException;

import static io.netty.buffer.ByteBufAllocator.DEFAULT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PlayHandlerTest {
  private final ChannelHandlerContext ctx = mock();
  private final FullHttpRequest nettyRequest = mock();
  private final HttpHeaders headers = mock();
  private final PlayHandler playHandler = new PlayHandler(null, null);

  @ParameterizedTest
  @CsvSource({
    "/login/to/shop, World, World",
    "/login/to/Hyper%20Mall, World, World",
    "/login/to/shop, Good+morning, Good morning",
    "/login/to/shop, Good%2Bmorning, Good+morning",
  })
  public void parseRequest(String path,String encodedParameterValue, String parameterValue) throws URISyntaxException {
    when(nettyRequest.uri()).thenReturn(path + "?user=bob&pwd=secret&greeting=" + encodedParameterValue);
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
    assertThat(request.url).isEqualTo(path + "?user=bob&pwd=secret&greeting=" + encodedParameterValue);
    assertThat(request.method).isEqualTo("GET");
    assertThat(request.domain).isEqualTo("site.eu");
    assertThat(request.path).isEqualTo(path);
    assertThat(request.querystring).isEqualTo("user=bob&pwd=secret&greeting=" + encodedParameterValue);
    assertThat(request.params.all()).hasSize(4);
    assertThat(request.params.get("user")).isEqualTo("bob");
    assertThat(request.params.get("pwd")).isEqualTo("secret");
    assertThat(request.params.get("greeting")).isEqualTo(parameterValue);
    assertThat(request.params.get("body")).isEqualTo("");
    assertThat(request.remoteAddress).isEqualTo("192.168.0.10");
    assertThat(request.contentType).isEqualTo("text/json");
    assertThat(request.port).isEqualTo(8080);
    assertThat(request.isSecure()).isEqualTo(false);
  }
}
