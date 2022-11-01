package play.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import static java.util.Objects.requireNonNull;

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
