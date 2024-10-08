package play.server.netty4;

import static io.netty.buffer.Unpooled.wrappedBuffer;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.stream.ChunkedInput;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ByteRangeInput implements ChunkedInput<ByteBuf> {
  private static final Logger logger = LoggerFactory.getLogger(ByteRangeInput.class);

  private final String filePath;
  private final RandomAccessFile file;
  private final HttpRequest request;
  private ByteRange[] byteRanges;
  private int currentByteRange;
  private final String contentType;
  private boolean unsatisfiable;
  private final long fileLength;

  public ByteRangeInput(
      String filePath, RandomAccessFile file, String contentType, HttpRequest request)
      throws IOException {
    this.filePath = filePath;
    this.file = file;
    this.request = request;
    fileLength = file.length();
    this.contentType = contentType;
    initRanges();
    if (logger.isDebugEnabled()) {
      logger.debug(
          "Invoked ByteRangeServer, found byteRanges: {} (with header Range: {}) on file {}",
          Arrays.toString(byteRanges),
          request.headers().get("range"),
          filePath);
    }
  }

  public void prepareNettyResponse(HttpResponse nettyResponse) {
    nettyResponse.headers().add("Accept-Ranges", "bytes");
    if (unsatisfiable) {
      nettyResponse.setStatus(HttpResponseStatus.REQUESTED_RANGE_NOT_SATISFIABLE);
      nettyResponse
          .headers()
          .set("Content-Range", "bytes " + 0 + "-" + (fileLength - 1) + "/" + fileLength);
      nettyResponse.headers().set("Content-length", 0);
    } else {
      nettyResponse.setStatus(HttpResponseStatus.PARTIAL_CONTENT);
      if (byteRanges.length == 1) {
        ByteRange range = byteRanges[0];
        nettyResponse
            .headers()
            .set("Content-Range", "bytes " + range.start + "-" + range.end + "/" + fileLength);
      } else {
        nettyResponse
            .headers()
            .set("Content-type", "multipart/byteranges; boundary=" + DEFAULT_SEPARATOR);
      }
      long length = 0;
      for (ByteRange range : byteRanges) {
        length += range.computeTotalLength();
      }
      nettyResponse.headers().set("Content-length", length);
    }
  }

  @Override
  public ByteBuf readChunk(ChannelHandlerContext ctx) throws Exception {
    return readChunk(ctx.alloc());
  }

  @Override
  public ByteBuf readChunk(ByteBufAllocator allocator) throws Exception {
    logger.trace("nextChunk @{}, currentByteRange={}", filePath, currentByteRange);
    try {
      int count = 0;
      int chunkSize = 8096;
      byte[] buffer = new byte[chunkSize];
      while (count < chunkSize
          && currentByteRange < byteRanges.length
          && byteRanges[currentByteRange] != null) {
        if (byteRanges[currentByteRange].remaining() > 0) {
          count += byteRanges[currentByteRange].fill(buffer, count);
        } else {
          currentByteRange++;
        }
      }
      if (count == 0) {
        return null;
      }

      return wrappedBuffer(buffer);
    } catch (Exception e) {
      logger.error("error sending file {}, currentByteRange={}", filePath, currentByteRange, e);
      throw e;
    }
  }

  @Override
  public long length() {
    return 0;
  }

  @Override
  public long progress() {
    return 0;
  }

  private boolean hasNextChunk() {
    boolean hasNextChunk =
        currentByteRange < byteRanges.length && byteRanges[currentByteRange].remaining() > 0;
    if (logger.isTraceEnabled()) {
      logger.trace("file {} hasNextChunk() : {}", filePath, hasNextChunk);
    }
    return hasNextChunk;
  }

  @Override
  public boolean isEndOfInput() {
    return !hasNextChunk();
  }

  @Override
  public void close() throws IOException {
    file.close();
  }

  public static boolean accepts(HttpRequest request) {
    return request.headers().contains("range");
  }

  private void initRanges() {
    try {
      String headerValue = request.headers().get("range").trim().substring("bytes=".length());
      String[] rangesValues = headerValue.split(",");
      ArrayList<long[]> ranges = new ArrayList<>(rangesValues.length);
      for (String rangeValue : rangesValues) {
        long start, end;
        if (rangeValue.startsWith("-")) {
          end = fileLength - 1;
          start = fileLength - 1 - Long.parseLong(rangeValue.substring("-".length()));
        } else {
          String[] range = rangeValue.split("-");
          start = Long.parseLong(range[0]);
          end = range.length > 1 ? Long.parseLong(range[1]) : fileLength - 1;
        }
        if (end > fileLength - 1) {
          end = fileLength - 1;
        }
        if (start <= end) {
          ranges.add(new long[] {start, end});
        }
      }
      long[][] reducedRanges = reduceRanges(ranges.toArray(new long[0][]));
      ByteRange[] localByteRanges = new ByteRange[reducedRanges.length];
      for (int i = 0; i < reducedRanges.length; i++) {
        long[] range = reducedRanges[i];
        localByteRanges[i] =
            new ByteRange(
                filePath,
                file,
                range[0],
                range[1],
                fileLength,
                contentType,
                reducedRanges.length > 1);
      }
      this.byteRanges = localByteRanges;
      if (this.byteRanges.length == 0) {
        unsatisfiable = true;
      }
    } catch (Exception e) {
      logger.debug("byterange error @{}", filePath, e);
      unsatisfiable = true;
    }
  }

  private static boolean rangesIntersect(long[] r1, long[] r2) {
    return r1[0] >= r2[0] && r1[0] <= r2[1] || r1[1] >= r2[0] && r1[0] <= r2[1];
  }

  private static long[] mergeRanges(long[] r1, long[] r2) {
    return new long[] {Math.min(r1[0], r2[0]), Math.max(r1[1], r2[1])};
  }

  private static long[][] reduceRanges(long[]... chunks) {
    if (chunks.length == 0) return new long[0][];
    long[][] sortedChunks = Arrays.copyOf(chunks, chunks.length);
    Arrays.sort(sortedChunks, Comparator.comparingLong(t -> t[0]));
    ArrayList<long[]> result = new ArrayList<>();
    result.add(sortedChunks[0]);
    for (int i = 1; i < sortedChunks.length; i++) {
      long[] c1 = sortedChunks[i];
      long[] r1 = result.get(result.size() - 1);
      if (rangesIntersect(c1, r1)) {
        result.set(result.size() - 1, mergeRanges(c1, r1));
      } else {
        result.add(c1);
      }
    }
    return result.toArray(new long[0][]);
  }

  static String makeRangeBodyHeader(
      String separator, String contentType, long start, long end, long fileLength) {
    return "--"
        + separator
        + "\r\n"
        + "Content-Type: "
        + contentType
        + "\r\n"
        + "ContentRange: bytes "
        + start
        + "-"
        + end
        + "/"
        + fileLength
        + "\r\n"
        + "\r\n";
  }

  static final String DEFAULT_SEPARATOR = "$$$THIS_STRING_SEPARATES$$$";
}
