package play.server;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

public class ResettableFileInputStreamTest {
  @Test
  public void canResetInputStream() throws IOException {
    File file = File.createTempFile("replay-resettable-file-input-stream", "test");
    FileUtils.write(file, "zebra", UTF_8);

    ResettableFileInputStream in = new ResettableFileInputStream(file);

    assertThat(IOUtils.toString(in, UTF_8)).isEqualTo("zebra");
    assertThat(IOUtils.toString(in, UTF_8)).isEqualTo("");

    in.reset();
    assertThat(IOUtils.toString(in, UTF_8)).isEqualTo("zebra");
  }
}