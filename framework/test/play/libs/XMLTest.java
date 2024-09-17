package play.libs;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

/** Tests for {@link XML} class. */
public class XMLTest {
  private static final Pattern TAG_PATTERN = Pattern.compile("<\\?[^?]+\\?>\\s*");

  private static final String ORIGINAL_DOCUMENT =
      "<?xml version=\"1.0\"?>\n"
          + "<feed xmlns=\"http://www.w3.org/2005/Atom\">"
          + "<title>Awesome Blog</title>"
          + "<link href=\"http://blog.example.com/\"/>"
          + "</feed>";
  private Document document;

  @BeforeEach
  public void setUp() {
    document = XML.getDocument(ORIGINAL_DOCUMENT);
  }

  private static String stripPreamble(String text) {
    return TAG_PATTERN.matcher(text).replaceFirst("");
  }

  @Test
  public void serializeShouldReturnWellFormedXml() {
    String outputDocument = XML.serialize(document);
    assertThat(stripPreamble(outputDocument)).isEqualTo(stripPreamble(ORIGINAL_DOCUMENT));
  }
}
