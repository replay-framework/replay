package play.data.parsing;

import org.junit.Test;
import play.mvc.Http;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.io.IOUtils.toByteArray;
import static org.assertj.core.api.Assertions.assertThat;

public class TextParserTest {
  TextParser parser = new TextParser();

  @Test
  public void parsesRequestBodyAsText() throws IOException {
    Http.Request request = new Http.Request();
    request.encoding = UTF_8;
    request.body = new ByteArrayInputStream("Don't reset me please".getBytes(UTF_8));

    assertThat(parser.parse(request).get("body"))
      .as("returns request body as <'body', body> map")
      .isEqualTo(new String[] {"Don't reset me please"});

    assertThat(toByteArray(request.body))
      .as("Important: request body should not be reset - some controllers might need to read it")
      .isEqualTo("Don't reset me please".getBytes(UTF_8));
  }
}