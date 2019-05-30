package play.libs;

import org.junit.BeforeClass;
import org.junit.Test;
import play.Play;
import play.PlayBuilder;
import play.mvc.Http.Response;

import java.nio.charset.Charset;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static org.junit.Assert.assertEquals;


/**
 * Tests for {@link MimeTypes} class.
 */
public class MimeTypesTest {
    @BeforeClass
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
            assertEquals("text/xml; charset=ISO-8859-1", MimeTypes.getContentType("test.xml"));
        }
        finally {
            Response.current().encoding = oldEncoding;
        }
    }

    @Test
    public void contentTypeShouldReturnDefaultCharsetInAbsenceOfResponse() {
        Response originalResponse = Response.current();
        try {
            Response.removeCurrent();
            assertEquals("text/xml; charset=" + Play.defaultWebEncoding, MimeTypes.getContentType("test.xml"));
        }
        finally {
            Response.setCurrent(originalResponse);
        }
    }
}
