package play.server.netty3;

import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ResponseRenderingListener implements ChannelFutureListener {
  private static final Logger logger = LoggerFactory.getLogger(ResponseRenderingListener.class);
  private final HttpRequest request;
  private final HttpResponse response;

  ResponseRenderingListener(HttpRequest nettyRequest, HttpResponse response) {
    request = nettyRequest;
    this.response = response;
  }

  @Override
  public void operationComplete(ChannelFuture future) {
    if (future.isSuccess()) {
      logger.trace("served {} {} (status: {})", 
        request.getMethod(), request.getUri(), response.getStatus());
    }
    else {
      logger.error("failed to serve {} {} (status: {})", 
        request.getMethod(), request.getUri(), response.getStatus(), future.getCause());
    }
  }

}
