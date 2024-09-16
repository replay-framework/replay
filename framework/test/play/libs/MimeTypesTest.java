package play.libs;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.Charset;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import play.Play;
import play.PlayBuilder;
import play.mvc.Http.Response;

/** Tests for {@link MimeTypes} class. */
public class MimeTypesTest {
  @BeforeAll
  public static void setUp() {
    PlayBuilder playBuilder = new PlayBuilder();
    playBuilder.build();
    playBuilder.initMvcObject();
  }

  @Test
  public void contentTypeShouldReturnResponseCharsetWhenAvailable() {
    Charset oldEncoding = Response.current().encoding;
    try {
      Response.current().encoding = ISO_8859_1;
      assertThat(MimeTypes.getContentType("test.xml")).isEqualTo("text/xml; charset=ISO-8859-1");
    } finally {
      Response.current().encoding = oldEncoding;
    }
  }

  @Test
  public void contentTypeShouldReturnDefaultCharsetInAbsenceOfResponse() {
    Response originalResponse = Response.current();
    try {
      Response.removeCurrent();
      assertThat(MimeTypes.getContentType("test.xml"))
          .isEqualTo("text/xml; charset=" + Play.defaultWebEncoding);
    } finally {
      Response.setCurrent(originalResponse);
    }
  }
}
