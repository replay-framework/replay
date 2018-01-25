package play.libs;

import org.junit.BeforeClass;
import org.junit.Test;
import play.PlayBuilder;
import play.mvc.Http.Response;

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
        String oldEncoding = Response.current().encoding;
        try {
            Response.current().encoding = "my-response-encoding";
            assertEquals("text/xml; charset=my-response-encoding",
                         MimeTypes.getContentType("test.xml"));
        }
        finally {
            Response.current().encoding = oldEncoding;
        }
    }

    @Test
    public void contentTypeShouldReturnDefaultCharsetInAbsenceOfResponse() {
        Response originalResponse = Response.current();
        try {
            Response.setCurrent(null);
            assertEquals("text/xml; charset=" + play.Play.defaultWebEncoding,
                         MimeTypes.getContentType("test.xml"));
        }
        finally {
            Response.setCurrent(originalResponse);
        }
    }
}
