package play.server.netty3;

import org.jboss.netty.handler.stream.ChunkedInput;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.util.concurrent.ConcurrentLinkedQueue;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.jboss.netty.buffer.ChannelBuffers.wrappedBuffer;

class LazyChunkedInput implements ChunkedInput {

  private boolean closed;
  private final ConcurrentLinkedQueue<byte[]> nextChunks = new ConcurrentLinkedQueue<>();

  @Override
  public boolean hasNextChunk() {
    return !nextChunks.isEmpty();
  }

  @Override
  public Object nextChunk() {
    if (nextChunks.isEmpty()) {
      return null;
    }
    return wrappedBuffer(nextChunks.poll());
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
    }
    else {
      String message = chunk == null ? "" : chunk.toString();
      bytes = message.getBytes(encoding);
    }

    try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream()) {
      byteStream.write(Integer.toHexString(bytes.length).getBytes(UTF_8));
      byte[] crlf = new byte[]{(byte) '\r', (byte) '\n'};
      byteStream.write(crlf);
      byteStream.write(bytes);
      byteStream.write(crlf);
      nextChunks.offer(byteStream.toByteArray());
    }
  }
}
