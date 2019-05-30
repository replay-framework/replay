package play.libs;


import play.libs.WS.HttpResponse;
import play.mvc.Http.Header;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;

public class TestHttpResponse extends HttpResponse {

    private final String queryContent;

    TestHttpResponse(String queryContent) {
        this.queryContent = queryContent;
    }

    @Override
    public Integer getStatus() {
        return null;
    }

    @Override
    public String getStatusText() {
        return null;
    }

    @Override
    public String getHeader(String key) {
        return null;
    }

    @Override
    public List<Header> getHeaders() {
        return null;
    }

    @Override
    public String getString() {
        return this.queryContent;
    }

    @Override
    public String getString(Charset encoding) {
        return this.queryContent;
    }

    @Override
    public InputStream getStream() {
         return new ByteArrayInputStream(this.queryContent.getBytes(UTF_8));
    }
}
