package play.libs;

import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

import static org.junit.Assert.assertEquals;

/**
 * Tests for {@link XML} class.
 */
public class XMLTest {

    private static final String ORIGINAL_DOCUMENT =
            "<?xml version=\"1.0\"?>\n" +
            "<feed xmlns=\"http://www.w3.org/2005/Atom\">" +
              "<title>Awesome Blog</title>" +
              "<link href=\"http://blog.example.com/\"/>" +
            "</feed>";
    private Document document;
    
    @Before
    public void setUp() {
        document = XML.getDocument(ORIGINAL_DOCUMENT); 
    }

    private static String stripPreamble(String text) {
        return text.replaceFirst("<\\?[^?]+\\?>\\s*", "");
    }
    
    @Test
    public void serializeShouldReturnWellFormedXml() {
        String outputDocument = XML.serialize(document);
        assertEquals(
                stripPreamble(ORIGINAL_DOCUMENT),
                stripPreamble(outputDocument));
    }
}
