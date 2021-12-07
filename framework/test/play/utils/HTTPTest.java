package play.utils;

import org.junit.Test;
import play.mvc.Http;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

public class HTTPTest {

    @Test
    public void testFixCaseForHttpHeader() {
        assertThat( HTTP.fixCaseForHttpHeader("Content-type")).isEqualTo("Content-Type");
        assertThat( HTTP.fixCaseForHttpHeader("Content-Type")).isEqualTo("Content-Type");
        assertThat( HTTP.fixCaseForHttpHeader("content-type")).isEqualTo("Content-Type");
        assertThat( HTTP.fixCaseForHttpHeader("Referer")).isEqualTo("Referer");
        assertThat( HTTP.fixCaseForHttpHeader("referer")).isEqualTo("Referer");
        // An one that is not in the list of valid http headers.
        String unknown = "Not-In-the-LiST";
        assertThat( HTTP.fixCaseForHttpHeader(unknown)).isEqualTo(unknown);
    }
    
    @Test
    public void testQuotedCharsetInHttpHeader() {

        HTTP.ContentTypeWithEncoding standardContentType = HTTP.parseContentType("text/html; charset=utf-8");
        assertThat(standardContentType.encoding).isEqualTo(UTF_8);
        assertThat(standardContentType.contentType).isEqualTo("text/html");
        
        
        HTTP.ContentTypeWithEncoding doubleQuotedCharsetContentType = HTTP.parseContentType("text/html; charset=\"utf-8\"");
        assertThat(doubleQuotedCharsetContentType.encoding).isEqualTo(UTF_8);
        assertThat(doubleQuotedCharsetContentType.contentType).isEqualTo("text/html");

        HTTP.ContentTypeWithEncoding simpleQuotedCharsetContentType = HTTP.parseContentType("text/html; charset='utf-8'");
        assertThat(simpleQuotedCharsetContentType.encoding).isEqualTo(UTF_8);
        assertThat(simpleQuotedCharsetContentType.contentType).isEqualTo("text/html");

        HTTP.ContentTypeWithEncoding defaultContentType = HTTP.parseContentType(null);
        assertThat(defaultContentType.encoding).isEqualTo(UTF_8);
        assertThat(defaultContentType.contentType).isEqualTo("text/html");
    }

    @Test @SuppressWarnings("deprecation")
    public void setHeader() {
        Http.Request request = new Http.Request();
        request.setHeader("X-Forwarded-For", "127.0.0.1");

        assertThat(request.headers.get("x-forwarded-for").value()).isEqualTo("127.0.0.1");
    }
}
