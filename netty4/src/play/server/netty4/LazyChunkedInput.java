package play.server.netty4;

import static io.netty.buffer.Unpooled.wrappedBuffer;
import static java.nio.charset.StandardCharsets.UTF_8;

import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.stream.ChunkedInput;
import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.util.concurrent.ConcurrentLinkedQueue;

class LazyChunkedInput implements ChunkedInput {

  private boolean closed;
  private final ConcurrentLinkedQueue<byte[]> nextChunks = new ConcurrentLinkedQueue<>();

  @Override
  public Object readChunk(ChannelHandlerContext ctx) {
    return readChunk((ByteBufAllocator) null);
  }

  @Override
  public Object readChunk(ByteBufAllocator allocator) {
    if (nextChunks.isEmpty()) {
      return null;
    }
    return wrappedBuffer(nextChunks.poll());
  }

  @Override
  public long length() {
    return nextChunks.size();
  }

  @Override
  public long progress() {
    return 0;
  }

  @Override
  public boolean isEndOfInput() {
    return closed && nextChunks.isEmpty();
  }

  @Override
  public void close() {
    if (!closed) {
      nextChunks.offer("0\r\n\r\n".getBytes(UTF_8));
    }
    closed = true;
  }

  void writeChunk(Object chunk, Charset encoding) throws Exception {
    if (closed) {
      throw new Exception("HTTP output stream closed");
    }

    byte[] bytes;
    if (chunk instanceof byte[]) {
      bytes = (byte[]) chunk;
    } else {
      String message = chunk == null ? "" : chunk.toString();
      bytes = message.getBytes(encoding);
    }

    try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream()) {
      byteStream.write(Integer.toHexString(bytes.length).getBytes(UTF_8));
      byte[] crlf = new byte[] {(byte) '\r', (byte) '\n'};
      byteStream.write(crlf);
      byteStream.write(bytes);
      byteStream.write(crlf);
      nextChunks.offer(byteStream.toByteArray());
    }
  }
}
