package play.server.javanet;

import com.sun.net.httpserver.HttpExchange;
import io.netty.handler.codec.http.HttpHeaderValues;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.libs.MimeTypes;
import play.mvc.Http.Request;
import play.mvc.Http.Response;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.concurrent.TimeUnit;

import static com.google.common.net.HttpHeaders.ACCEPT_RANGES;
import static com.google.common.net.HttpHeaders.CONTENT_TYPE;
import static java.lang.System.nanoTime;
import static java.util.Objects.requireNonNullElse;
import static play.mvc.Http.Methods.HEAD;
import static play.mvc.Http.StatusCode.NOT_MODIFIED;
import static play.mvc.Http.StatusCode.OK;

@ParametersAreNonnullByDefault
public class FileService {
  private static final Logger logger = LoggerFactory.getLogger(FileService.class);

  public void serve(File localFile, HttpExchange exchange, Request request, Response response, boolean keepAlive) throws IOException {
    long startedAt = nanoTime();
    String file = localFile.getAbsolutePath();

    try (RandomAccessFile raf = new RandomAccessFile(localFile, "r")) {
      try {
        long fileLength = raf.length();
        String fileContentType = MimeTypes.getContentType(localFile.getName(), "text/plain");
        String contentType = requireNonNullElse(response.contentType, fileContentType);

        if (logger.isTraceEnabled()) {
          logger.trace("serving {}, keepAlive:{}, contentType:{}, fileLength:{}, request.path:{}", file,
            keepAlive, contentType, fileLength, request.path);
        }

        setHeaders(exchange, fileLength, contentType);
        writeFileContent(file, exchange, raf, keepAlive, fileContentType, startedAt);

      }
      catch (Throwable e) {
        logger.error("Failed to serve {} in {} ms", file, formatNanos(nanoTime() - startedAt), e);
      }
    }
  }

  private void setHeaders(HttpExchange exchange, long fileLength, String contentType) throws IOException {
    exchange.getResponseHeaders().set(CONTENT_TYPE, contentType);
    exchange.getResponseHeaders().set(ACCEPT_RANGES, HttpHeaderValues.BYTES.toString()); // TODO is it needed?
    if (exchange.getResponseCode() != NOT_MODIFIED) {
      exchange.sendResponseHeaders(OK, fileLength);
    }
  }

  private void writeFileContent(String file, HttpExchange exchange,
                                RandomAccessFile raf, boolean isKeepAlive, String fileContentType,
                                long startedAt) throws IOException {
    if (exchange.getRequestMethod().equals(HEAD)) {
      logger.trace("served {} {} in {} ms", exchange.getRequestMethod(), file, formatNanos(nanoTime() - startedAt));
    }
    else {
      try (OutputStream out = exchange.getResponseBody()) {
        byte[] buffer = new byte[1024];
        int count = raf.read(buffer);
        while (count != -1) {
          out.write(buffer, 0, count);
          count = raf.read(buffer, 0, 1024);
        }
      }
    }
  }

  private static String formatNanos(long executionTimeNanos) {
    return String.format("%d.%d", TimeUnit.NANOSECONDS.toMillis(executionTimeNanos), executionTimeNanos % 1_000_000 / 1_000);
  }

}
