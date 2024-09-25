package play.server.netty3;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.io.IOUtils.toByteArray;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import play.data.parsing.TextParser;
import play.mvc.Http;

public class FileChannelBufferTest {
  private final TextParser parser = new TextParser();

  @Test
  public void fileChannelBuffer_supports_reset() throws IOException {
    File tempFile = Files.createTempFile("replay", "test").toFile();
    FileUtils.write(tempFile, "Don't reset me please", UTF_8);

    Http.Request request = givenRequest(new FileChannelBuffer(tempFile).getInputStream());

    assertThat(parser.parse(request).get("body")).isEqualTo(new String[] {"Don't reset me please"});
    assertThat(toByteArray(request.body)).isEqualTo("Don't reset me please".getBytes(UTF_8));
  }

  private Http.Request givenRequest(InputStream in) {
    Http.Request request = new Http.Request();
    request.encoding = UTF_8;
    request.body = in;
    return request;
  }
}
