package play.test;

import org.junit.Test;
import play.mvc.Http.Response;

import static org.junit.Assert.assertEquals;

public class ContentTypeAssertionTest {

    @Test(expected = AssertionError.class)
    public void givenContentTypeIsMissing_shouldThrowAssertionError() {
        Response responseWithoutContentType = new Response();

        assertEquals("text/html", responseWithoutContentType.contentType);
    }

}
