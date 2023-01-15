package play.server.netty4;

import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ResponseRenderingListener implements GenericFutureListener<Future<Void>> {
  private static final Logger logger = LoggerFactory.getLogger(ResponseRenderingListener.class);
  private final HttpRequest request;
  private final HttpResponse response;

  ResponseRenderingListener(HttpRequest nettyRequest, HttpResponse response) {
    request = nettyRequest;
    this.response = response;
  }

  @Override
  public void operationComplete(Future<Void> future) {
    if (future.isSuccess()) {
      logger.trace("served {} {} (status: {})", request.method(), request.uri(), response.status());
    }
    else {
      logger.error("failed to serve {} {} (status: {})", request.method(), request.uri(), response.status(), future.cause());
    }
  }

}
