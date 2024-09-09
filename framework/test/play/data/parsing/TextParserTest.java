package play.data.parsing;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import play.mvc.Http;

import java.io.*;
import java.nio.file.Files;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.io.IOUtils.toByteArray;
import static org.assertj.core.api.Assertions.assertThat;

public class TextParserTest {
  private final TextParser parser = new TextParser();

  @Test
  public void parsesRequestBodyAsText() throws IOException {
    Http.Request request = givenRequest(new ByteArrayInputStream("Don't reset me please".getBytes(UTF_8)));

    assertThat(parser.parse(request).get("body"))
      .as("returns request body as <'body', body> map")
      .isEqualTo(new String[] {"Don't reset me please"});

    assertThat(toByteArray(request.body))
      .as("Important: request body should not be reset - some controllers might need to read it")
      .isEqualTo("Don't reset me please".getBytes(UTF_8));
  }

  @Test
  public void shouldNotFail_ifInputStreamDoesNotSupportReset() throws IOException {
    File tempFile = Files.createTempFile("replay", "test").toFile();
    FileUtils.write(tempFile, "Don't reset me please", UTF_8);

    Http.Request request = givenRequest(new FileInputStream(tempFile));

    assertThat(parser.parse(request).get("body")).isEqualTo(new String[] {"Don't reset me please"});
    assertThat(toByteArray(request.body)).isEqualTo("".getBytes(UTF_8));
  }

  private Http.Request givenRequest(InputStream in) {
    Http.Request request = new Http.Request();
    request.encoding = UTF_8;
    request.body = in;
    return request;
  }
}
