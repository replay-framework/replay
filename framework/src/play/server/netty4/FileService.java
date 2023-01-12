package play.server.netty4;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.stream.ChunkedFile;
import io.netty.handler.stream.ChunkedInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.libs.MimeTypes;
import play.mvc.Http.Request;
import play.mvc.Http.Response;

import java.io.*;
import java.util.concurrent.TimeUnit;

import static java.lang.System.nanoTime;
import static io.netty.handler.codec.http.HttpHeaders.Names.ACCEPT_RANGES;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;

public class FileService  {
    private static final Logger logger = LoggerFactory.getLogger(FileService.class);

    public void serve(File localFile, HttpRequest nettyRequest, HttpResponse nettyResponse, ChannelHandlerContext ctx, Request request, Response response, Channel channel) throws FileNotFoundException {
        logger.trace("FileService.serve: begin");
        long startedAt = nanoTime();
        String filePath = localFile.getAbsolutePath();
        RandomAccessFile raf = new RandomAccessFile(localFile, "r");

        try {
            long fileLength = raf.length();
            boolean isKeepAlive = HttpUtil.isKeepAlive(nettyRequest) && nettyRequest.protocolVersion().equals(HttpVersion.HTTP_1_1);
            String fileContentType = MimeTypes.getContentType(localFile.getName(), "text/plain");
            String contentType = response.contentType != null ? response.contentType : fileContentType;

            if (logger.isTraceEnabled()) {
                logger.trace("serving {}, keepAlive:{}, contentType:{}, fileLength:{}, request.path:{}", filePath,
                  isKeepAlive, contentType, fileLength, request.path);
            }

            setHeaders(nettyResponse, fileLength, contentType);
            writeFileContent(filePath, nettyRequest, nettyResponse, channel, raf, isKeepAlive, fileContentType, startedAt);
            logger.trace("FileService.serve: end");
        } catch (Throwable e) {
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
            sendFileFuture.addListener(new ChannelProgressiveFutureListener() {
                                        @Override
                                        public void operationProgressed(ChannelProgressiveFuture future, long progress, long total) throws Exception {
                                            if (total < 0) { // total unknown
                                                logger.trace(future.channel() + " Transfer progress: " + progress);
                                            } else {
                                                logger.trace(future.channel() + " Transfer progress: " + progress + " / " + total);
                                            }
                                        }

                                        @Override
                                        public void operationComplete(ChannelProgressiveFuture future) throws Exception {
                                            logger.trace("served {} in {} ms", filePath, formatNanos(nanoTime() - startedAt));
                                        }
                                    }
            );
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

    private ChunkedInput<ByteBuf> getChunkedInput(String filePath, RandomAccessFile raf, String contentType, HttpRequest nettyRequest, HttpResponse nettyResponse) throws IOException {
        if (ByteRangeInput.accepts(nettyRequest)) {
            logger.trace("getChunkedInput: serving range");
            ByteRangeInput server = new ByteRangeInput(filePath, raf, contentType, nettyRequest);
            server.prepareNettyResponse(nettyResponse);
            return server;
        } else {
            logger.trace("getChunkedInput: serving chunkedfile");
            return new ChunkedFile(raf);
        }
    }

    private String formatNanos(long executionTimeNanos) {
        return String.format("%d.%d", TimeUnit.NANOSECONDS.toMillis(executionTimeNanos), executionTimeNanos % 1_000_000 / 1_000);
    }
}