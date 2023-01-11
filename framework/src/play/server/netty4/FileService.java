package play.server.netty4;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelProgressiveFuture;
import io.netty.channel.ChannelProgressiveFutureListener;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.stream.ChunkedFile;
import io.netty.handler.stream.ChunkedInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.libs.MimeTypes;
import play.mvc.Http.Request;
import play.mvc.Http.Response;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.concurrent.TimeUnit;

import static io.netty.handler.codec.http.HttpHeaderNames.ACCEPT_RANGES;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import static java.lang.System.nanoTime;

public class FileService {
  private static final Logger logger = LoggerFactory.getLogger(FileService.class);

  public void serve(File localFile, HttpRequest nettyRequest, HttpResponse nettyResponse, ChannelHandlerContext ctx, 
                    Request request, Response response, Channel channel) throws FileNotFoundException {
    logger.trace("FileService.serve: begin");
    long startedAt = nanoTime();
    String filePath = localFile.getAbsolutePath();
    RandomAccessFile raf = new RandomAccessFile(localFile, "r");

    try {
      long fileLength = raf.length();
      boolean isKeepAlive = HttpUtil.isKeepAlive(nettyRequest) && nettyRequest.protocolVersion().equals(HTTP_1_1);
      String fileContentType = MimeTypes.getContentType(localFile.getName(), "text/plain");
      String contentType = response.contentType != null ? response.contentType : fileContentType;

      if (logger.isTraceEnabled()) {
        logger.trace("serving {}, keepAlive:{}, contentType:{}, fileLength:{}, request.path:{}", filePath,
          isKeepAlive, contentType, fileLength, request.path);
      }

      setHeaders(nettyResponse, fileLength, contentType);
      writeFileContent(filePath, nettyRequest, nettyResponse, channel, raf, isKeepAlive, fileContentType, startedAt);
      logger.trace("FileService.serve: end");
    }
    catch (Throwable e) {
      logger.error("Failed to serve {} in {} ms", filePath, formatNanos(nanoTime() - startedAt), e);
      closeSafely(localFile, raf, request.path);
      closeSafely(ctx, request.path);
    }
  }

  private void closeSafely(File localFile, Closeable closeable, String path) {
    try {
      if (closeable != null) {
        closeable.close();
      }
    }
    catch (IOException e) {
      logger.warn("Failed to close {}, request.path:{}", localFile.getAbsolutePath(), path, e);
    }
  }

  private void closeSafely(ChannelHandlerContext ctx, String path) {
    try {
      if (ctx.channel().isOpen()) {
        ctx.channel().close();
      }
    }
    catch (Throwable ex) {
      logger.warn("Failed to closed channel, request.path:{}", path, ex);
    }
  }

  private void writeFileContent(String filePath, HttpRequest nettyRequest, HttpResponse nettyResponse, Channel channel,
                                RandomAccessFile raf, boolean isKeepAlive, String fileContentType,
                                long startedAt) throws IOException {
    ChannelFuture sendFileFuture = null;
    ChannelFuture lastContentFuture = null;

    if (!nettyRequest.method().equals(HttpMethod.HEAD)) {
      ChunkedInput<ByteBuf> chunkedInput = getChunkedInput(filePath, raf, fileContentType, nettyRequest, nettyResponse);
      if (channel.isOpen()) {
        channel.write(nettyResponse);
        sendFileFuture = channel.write(chunkedInput, channel.newProgressivePromise());
        lastContentFuture = channel.writeAndFlush(Unpooled.EMPTY_BUFFER);
      }
      else {
        logger.debug("Try to write {} on a closed channel[keepAlive:{}]: Remote host may have closed the connection", filePath, isKeepAlive);
      }
    }
    else {
      if (channel.isOpen()) {
        lastContentFuture = channel.writeAndFlush(nettyResponse);
      }
      else {
        logger.debug("Try to write {} on a closed channel[keepAlive:{}]: Remote host may have closed the connection", filePath, isKeepAlive);
      }
      raf.close();
      logger.trace("HEAD served {} in {} ms", filePath, formatNanos(nanoTime() - startedAt));
    }

    if (sendFileFuture != null) {
      sendFileFuture.addListener(new FileServingListener(filePath, startedAt));
      if (!isKeepAlive) {
        lastContentFuture.addListener(ChannelFutureListener.CLOSE);
      }
    }
  }

  private void setHeaders(HttpResponse nettyResponse, long fileLength, String contentType) {
    if (!nettyResponse.status().equals(HttpResponseStatus.NOT_MODIFIED)) {
      // Add 'Content-Length' header only for a keep-alive connection.
      nettyResponse.headers().set(HttpHeaderNames.CONTENT_LENGTH, String.valueOf(fileLength));
    }

    nettyResponse.headers().set(CONTENT_TYPE, contentType);
    nettyResponse.headers().set(ACCEPT_RANGES, HttpHeaderValues.BYTES);
  }

  private ChunkedInput<ByteBuf> getChunkedInput(String filePath, RandomAccessFile raf, String contentType, 
                                                HttpRequest nettyRequest, HttpResponse nettyResponse) throws IOException {
    if (ByteRangeInput.accepts(nettyRequest)) {
      logger.trace("getChunkedInput: serving range");
      ByteRangeInput server = new ByteRangeInput(filePath, raf, contentType, nettyRequest);
      server.prepareNettyResponse(nettyResponse);
      return server;
    }
    else {
      logger.trace("getChunkedInput: serving chunkedfile");
      return new ChunkedFile(raf);
    }
  }

  private static String formatNanos(long executionTimeNanos) {
    return String.format("%d.%d", TimeUnit.NANOSECONDS.toMillis(executionTimeNanos), executionTimeNanos % 1_000_000 / 1_000);
  }

  private static class FileServingListener implements ChannelProgressiveFutureListener {
    private final String filePath;
    private final long startedAt;

    public FileServingListener(String filePath, long startedAt) {
      this.filePath = filePath;
      this.startedAt = startedAt;
    }

    @Override
    public void operationProgressed(ChannelProgressiveFuture future, long progress, long total) {
      logger.trace("{} Transfer progress: {}/{} (success: {})", future.channel(), progress, total < 0 ? "?" : total, future.isSuccess());
    }

    @Override
    public void operationComplete(ChannelProgressiveFuture future) {
      if (future.isSuccess()) {
        logger.trace("served {} in {} ms", filePath, formatNanos(nanoTime() - startedAt));
      }
      else {
        logger.error("failed to serve {} in {} ms", filePath, formatNanos(nanoTime() - startedAt), future.cause());
      }
    }
  }
}
