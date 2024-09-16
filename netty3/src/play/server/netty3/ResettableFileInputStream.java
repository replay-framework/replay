package play.server.netty3;

import static java.util.Objects.requireNonNull;

import java.io.*;

public class ResettableFileInputStream extends InputStream {
  private final File file;
  private InputStream in;

  public ResettableFileInputStream(File file) throws FileNotFoundException {
    this.file = requireNonNull(file);
    reset();
  }

  @Override
  public int read() throws IOException {
    return in.read();
  }

  @Override
  public final synchronized void reset() throws FileNotFoundException {
    in = new FileInputStream(file);
  }

  @Override
  public void close() throws IOException {
    in.close();
  }
}
