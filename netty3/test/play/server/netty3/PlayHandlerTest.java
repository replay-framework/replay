package play.server.netty3;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.http.DefaultHttpRequest;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.junit.jupiter.api.Test;
import play.mvc.Http;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URISyntaxException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.jboss.netty.buffer.ChannelBuffers.EMPTY_BUFFER;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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

  @Test
  public void parseBadUriThrowsIllegalArgumentException() {
    MessageEvent message = mock();
    HttpRequest nettyRequest = mock();
    HttpHeaders headers = mock();
    when(nettyRequest.getUri()).thenReturn("?utm=unparseable+|+pipe+character");
    when(nettyRequest.getMethod()).thenReturn(HttpMethod.GET);
    when(nettyRequest.headers()).thenReturn(headers);

    assertThatThrownBy(() -> playHandler.parseRequest(nettyRequest, message))
        .isInstanceOf(URISyntaxException.class);
  }

  @Test
  public void respondWithBadRequestOnBadUri() {
    var uri = "?utm=unparseable+|+pipe+character";
    var httpRequest = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, uri);
    MessageEvent messageEvent = mock();
    when(messageEvent.getMessage()).thenReturn(httpRequest);
    ChannelFuture channelFuture = mock(ChannelFuture.class);
    Channel channel = mock();
    when(channel.write(any())).thenReturn(channelFuture);
    ChannelHandlerContext ctx = mock();
    when(ctx.getChannel()).thenReturn(channel);

    playHandler.messageReceived(ctx, messageEvent);

    verify(channel, times(1)).write(argThat(obj -> {
      if (obj instanceof HttpResponse) {
        assertThat(((HttpResponse) obj).getStatus()).isEqualTo(HttpResponseStatus.BAD_REQUEST);
        return true;
      } else {
        return false;
      }
    }));
  }
}