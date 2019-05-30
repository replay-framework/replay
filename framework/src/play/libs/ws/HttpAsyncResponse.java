package play.libs.ws;

import com.ning.http.client.Response;
import play.mvc.Http;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * An HTTP response wrapper
 */
public class HttpAsyncResponse extends HttpResponse {

    private Response response;

    /**
     * You shouldn't have to create an HttpResponse yourself
     *
     * @param response
     *            The given response
     */
    public HttpAsyncResponse(Response response) {
        this.response = response;
    }

    /**
     * The HTTP status code
     *
     * @return the status code of the http response
     */
    @Override
    public Integer getStatus() {
        return this.response.getStatusCode();
    }

    /**
     * the HTTP status text
     *
     * @return the status text of the http response
     */
    @Override
    public String getStatusText() {
        return this.response.getStatusText();
    }

    @Override
    public String getHeader(String key) {
        return response.getHeader(key);
    }

    @Override
    public List<Http.Header> getHeaders() {
        Map<String, List<String>> hdrs = response.getHeaders();
        List<Http.Header> result = new ArrayList<>();
        for (String key : hdrs.keySet()) {
            result.add(new Http.Header(key, hdrs.get(key)));
        }
        return result;
    }

    @Override
    public String getString() {
        try {
            return response.getResponseBody(getEncoding().name());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getString(Charset encoding) {
        try {
            return response.getResponseBody(encoding.name());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * get the response as a stream
     *
     * @return an inputstream
     */
    @Override
    public InputStream getStream() {
        try {
            return response.getResponseBodyAsStream();
        } catch (IllegalStateException e) {
            return new ByteArrayInputStream(new byte[] {}); // Workaround
                                                            // AHC's bug on
                                                            // empty
                                                            // responses
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
