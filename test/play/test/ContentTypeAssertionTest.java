package play.test;

import org.junit.Test;
import play.mvc.Http.Response;

import static play.test.FunctionalTest.assertContentType;

public class ContentTypeAssertionTest {

    @Test(expected = AssertionError.class)
    public void givenContentTypeIsMissing_shouldThrowAssertionError() {
        Response responseWithoutContentType = new Response();

        assertContentType("text/html", responseWithoutContentType);
    }

}
