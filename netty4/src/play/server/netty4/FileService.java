package play.server.netty4;

import static io.netty.handler.codec.http.HttpHeaderNames.ACCEPT_RANGES;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import static io.netty.handler.codec.http.LastHttpContent.EMPTY_LAST_CONTENT;
import static java.lang.System.nanoTime;

import io.netty.buffer.ByteBuf;
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
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.libs.MimeTypes;
import play.mvc.Http.Request;
import play.mvc.Http.Response;

public class FileService {
  private static final Logger logger = LoggerFactory.getLogger(FileService.class);

  public void serve(
      File localFile,
      HttpRequest nettyRequest,
      HttpResponse nettyResponse,
      ChannelHandlerContext ctx,
      Request request,
      Response response,
      Channel channel)
      throws FileNotFoundException {
    logger.trace("FileService.serve: begin :{}:{}", request.method, request.path);
    long startedAt = nanoTime();
    String filePath = localFile.getAbsolutePath();
    RandomAccessFile raf = new RandomAccessFile(localFile, "r");

    try {
      long fileLength = raf.length();
      boolean isKeepAlive =
          HttpUtil.isKeepAlive(nettyRequest) && nettyRequest.protocolVersion().equals(HTTP_1_1);
      String fileContentType = MimeTypes.getContentType(localFile.getName(), "text/plain");
      String contentType = response.contentType != null ? response.contentType : fileContentType;

      logger.trace(
          "serving {}, keepAlive:{}, contentType:{}, fileLength:{} :{}:{}",
          filePath,
          isKeepAlive,
          contentType,
          fileLength,
          request.method,
          request.path
      );

      setHeaders(nettyResponse, fileLength, contentType);
      writeFileContent(
          filePath,
          nettyRequest,
          nettyResponse,
          channel,
          raf,
          isKeepAlive,
          fileContentType,
          startedAt);
      logger.trace("FileService.serve: end :{}:{}", request.method, request.path);
    } catch (Throwable e) {
      logger.error("Failed to serve {} in {} ms :{}:{}", filePath,
          formatNanos(nanoTime() - startedAt), request.method, request.path, e);
      closeSafely(localFile, raf, request);
      closeSafely(ctx, request);
    }
  }

  private void closeSafely(File localFile, Closeable closeable, Request request) {
    try {
      if (closeable != null) {
        closeable.close();
      }
    } catch (IOException e) {
      logger.warn("Failed to close {} :{}:{}", localFile.getAbsolutePath(), request.method, request.path, e);
    }
  }

  private void closeSafely(ChannelHandlerContext ctx, Request request) {
    try {
      if (ctx.channel().isOpen()) {
        ctx.channel().close();
      }
    } catch (Throwable ex) {
      logger.warn("Failed to close channel :{}:{}", request.method, request.path, ex);
    }
  }

  private void writeFileContent(
      String filePath,
      HttpRequest nettyRequest,
      HttpResponse nettyResponse,
      Channel channel,
      RandomAccessFile raf,
      boolean isKeepAlive,
      String fileContentType,
      long startedAt)
      throws IOException {
    ChannelFuture sendFileFuture = null;
    ChannelFuture lastContentFuture = null;

    if (!nettyRequest.method().equals(HttpMethod.HEAD)) {
      ChunkedInput<ByteBuf> chunkedInput =
          getChunkedInput(filePath, raf, fileContentType, nettyRequest, nettyResponse);
      if (channel.isOpen()) {
        channel.write(nettyResponse);
        sendFileFuture = channel.write(chunkedInput, channel.newProgressivePromise());
        lastContentFuture = channel.writeAndFlush(EMPTY_LAST_CONTENT);
      } else {
        logger.debug(
            "Try to write {} on a closed channel[keepAlive:{}]: Remote host may have closed the connection :{}:{}",
            filePath,
            isKeepAlive,
            nettyRequest.method(), nettyRequest.uri()
        );
      }
    } else {
      if (channel.isOpen()) {
        lastContentFuture = channel.writeAndFlush(nettyResponse);
      } else {
        logger.debug(
            "Try to write {} on a closed channel[keepAlive:{}]: Remote host may have closed the connection :{}:{}",
            filePath,
            isKeepAlive,
            nettyRequest.method(), nettyRequest.uri()
        );
      }
      raf.close();
      logger.trace("HEAD served {} in {} ms :{}:{}", filePath,
          formatNanos(nanoTime() - startedAt), nettyRequest.method(), nettyRequest.uri());
    }

    if (sendFileFuture != null) {
      sendFileFuture.addListener(new FileServingListener(filePath, startedAt, nettyRequest));
    }
    if (!isKeepAlive) {
      lastContentFuture.addListener(ChannelFutureListener.CLOSE);
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

  private ChunkedInput<ByteBuf> getChunkedInput(
      String filePath,
      RandomAccessFile raf,
      String contentType,
      HttpRequest nettyRequest,
      HttpResponse nettyResponse)
      throws IOException {
    if (ByteRangeInput.accepts(nettyRequest)) {
      logger.trace("getChunkedInput: serving range :{}:{}", nettyRequest.method(), nettyRequest.uri());
      ByteRangeInput server = new ByteRangeInput(filePath, raf, contentType, nettyRequest);
      server.prepareNettyResponse(nettyResponse);
      return server;
    } else {
      logger.trace("getChunkedInput: serving chunked file :{}:{}", nettyRequest.method(), nettyRequest.uri());
      return new ChunkedFile(raf);
    }
  }

  private static String formatNanos(long executionTimeNanos) {
    return String.format(
        "%d.%d",
        TimeUnit.NANOSECONDS.toMillis(executionTimeNanos), executionTimeNanos % 1_000_000 / 1_000);
  }

  private static class FileServingListener implements ChannelProgressiveFutureListener {
    private final String filePath;
    private final long startedAt;
    private final HttpRequest request;

    private FileServingListener(String filePath, long startedAt, HttpRequest request) {
      this.filePath = filePath;
      this.startedAt = startedAt;
      this.request = request;
    }

    @Override
    public void operationProgressed(ChannelProgressiveFuture future, long progress, long total) {
      logger.trace(
          "{} Transfer progress: {}/{} (success: {}) :{}:{}",
          future.channel(),
          progress,
          total < 0 ? "?" : total,
          future.isSuccess(),
          request.method(), request.uri()
      );
    }

    @Override
    public void operationComplete(ChannelProgressiveFuture future) {
      if (future.isSuccess()) {
        logger.trace("served {} in {} ms :{}:{}", filePath,
            formatNanos(nanoTime() - startedAt), request.method(), request.uri());
      } else {
        logger.error(
            "failed to serve {} in {} ms :{}:{}",
            filePath,
            formatNanos(nanoTime() - startedAt),
            request.method(), request.uri(),
            future.cause()
        );
      }
    }
  }
}
