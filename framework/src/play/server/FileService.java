package play.server;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.http.*;
import org.jboss.netty.handler.stream.ChunkedFile;
import org.jboss.netty.handler.stream.ChunkedInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.libs.MimeTypes;
import play.mvc.Http.Request;
import play.mvc.Http.Response;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;

public class FileService  {
    private static final Logger logger = LoggerFactory.getLogger(FileService.class);

    public void serve(File localFile, HttpRequest nettyRequest, HttpResponse nettyResponse, ChannelHandlerContext ctx, Request request, Response response, Channel channel) throws FileNotFoundException {
        RandomAccessFile raf = new RandomAccessFile(localFile, "r");
        try {
            long fileLength = raf.length();
            
            boolean isKeepAlive = HttpHeaders.isKeepAlive(nettyRequest) && nettyRequest.getProtocolVersion().equals(HttpVersion.HTTP_1_1);

            if (logger.isTraceEnabled()) {
                logger.trace("keep alive {}", isKeepAlive);
                logger.trace("content type {}", response.contentType != null ? response.contentType : MimeTypes.getContentType(localFile.getName(), "text/plain"));
            }

            if (!nettyResponse.getStatus().equals(HttpResponseStatus.NOT_MODIFIED)) {
                // Add 'Content-Length' header only for a keep-alive connection.
                logger.trace("file length {}", fileLength);
                nettyResponse.headers().set(HttpHeaders.Names.CONTENT_LENGTH, String.valueOf(fileLength));
            }

            if (response.contentType != null) {
                nettyResponse.headers().set(CONTENT_TYPE, response.contentType);
            } else {
                nettyResponse.headers().set(CONTENT_TYPE, (MimeTypes.getContentType(localFile.getName(), "text/plain")));
            }

            nettyResponse.headers().set(HttpHeaders.Names.ACCEPT_RANGES, HttpHeaders.Values.BYTES);

            // Write the initial line and the header.
            ChannelFuture writeFuture = null;

            // Write the content.
            if (!nettyRequest.getMethod().equals(HttpMethod.HEAD)) {
                ChunkedInput chunkedInput = getChunkedInput(raf, MimeTypes.getContentType(localFile.getName(), "text/plain"), nettyRequest, nettyResponse);
                if (channel.isOpen()) {
                    channel.write(nettyResponse);
                    writeFuture = channel.write(chunkedInput);
                }else{
                    logger.debug("Try to write on a closed channel[keepAlive:{}]: Remote host may have closed the connection", isKeepAlive);
                }
            } else {
                if (channel.isOpen()) {
                    writeFuture = channel.write(nettyResponse);
                }else{
                    logger.debug("Try to write on a closed channel[keepAlive:{}]: Remote host may have closed the connection", isKeepAlive);
                }
                raf.close();
            }

            if (writeFuture != null && !isKeepAlive) {
                writeFuture.addListener(ChannelFutureListener.CLOSE);
            }
        } catch (Throwable exx) {
            exx.printStackTrace();
            closeQuietly(raf);
            try {
                if (ctx.getChannel().isOpen()) {
                    ctx.getChannel().close();
                }
            } catch (Throwable ex) { /* Left empty */ }
        }
    }
    
    private static ChunkedInput getChunkedInput(RandomAccessFile raf, String contentType, HttpRequest nettyRequest, HttpResponse nettyResponse) throws IOException {
        if(ByteRangeInput.accepts(nettyRequest)) {
            ByteRangeInput server = new ByteRangeInput(raf, contentType, nettyRequest);
            server.prepareNettyResponse(nettyResponse);
            return server;
        } else {
            return new ChunkedFile(raf);
        }
    }
}
