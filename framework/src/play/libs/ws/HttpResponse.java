package play.libs.ws;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import play.Play;
import play.libs.XML;
import play.mvc.Http;
import play.utils.HTTP;

import javax.annotation.Nullable;
import javax.xml.parsers.DocumentBuilder;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An HTTP response wrapper
 */
public abstract class HttpResponse {
    private static final Logger logger = LoggerFactory.getLogger(HttpResponse.class);

    private Charset encoding;

    /**
     * the HTTP status code
     *
     * @return the status code of the http response
     */
    public abstract Integer getStatus();

    /**
     * The HTTP status text
     *
     * @return the status text of the http response
     */
    public abstract String getStatusText();

    /**
     * @return true if the status code is 20x, false otherwise
     */
    public boolean success() {
        return Http.StatusCode.success(this.getStatus());
    }

    /**
     * The http response content type
     *
     * @return the content type of the http response
     */
    public String getContentType() {
        return getHeader("content-type") != null ? getHeader("content-type") : getHeader("Content-Type");
    }

    public Charset getEncoding() {
        // Have we already parsed it?
        if (encoding != null) {
            return encoding;
        }

        // no! must parse it and remember
        String contentType = getContentType();
        if (contentType == null) {
            encoding = Play.defaultWebEncoding;
        } else {
            HTTP.ContentTypeWithEncoding contentTypeEncoding = HTTP.parseContentType(contentType);
            if (contentTypeEncoding.encoding == null) {
                encoding = Play.defaultWebEncoding;
            } else {
                encoding = contentTypeEncoding.encoding;
            }
        }
        return encoding;

    }

    @Nullable public abstract String getHeader(String key);

    public abstract List<Http.Header> getHeaders();

    /**
     * Parse and get the response body as a {@link Document DOM document}
     *
     * @return a DOM document
     */
    public Document getXml() {
        return getXml(getEncoding());
    }

    /**
     * parse and get the response body as a {@link Document DOM document}
     *
     * @param encoding
     *            xml charset encoding
     * @return a DOM document
     */
    public Document getXml(Charset encoding) {
        try {
            InputSource source = new InputSource(new StringReader(getString()));
            source.setEncoding(encoding.name());
            DocumentBuilder builder = XML.newDocumentBuilder();
            return builder.parse(source);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * get the response body as a string
     *
     * @return the body of the http response
     */
    public abstract String getString();

    /**
     * get the response body as a string
     *
     * @param encoding
     *            string charset encoding
     * @return the body of the http response
     */
    public abstract String getString(Charset encoding);

    /**
     * Parse the response string as a query string.
     *
     * @return The parameters as a Map. Return an empty map if the response is not formed as a query string.
     */
    public Map<String, String> getQueryString() {
        Map<String, String> result = new HashMap<>();
        String body = getString();
        for (String entry : body.split("&")) {
            int pos = entry.indexOf('=');
            if (pos > -1) {
                result.put(entry.substring(0, pos), entry.substring(pos + 1));
            } else {
                result.put(entry, "");
            }
        }
        return result;
    }

    /**
     * get the response as a stream
     * <p>
     * + this method can only be called onced because async implementation does not allow it to be called + multiple
     * times +
     * </p>
     *
     * @return an inputstream
     */
    public abstract InputStream getStream();

    /**
     * get the response body as a {@link com.google.gson.JsonElement}
     *
     * @return the json response
     */
    public JsonElement getJson() {
        String json = getString();
        try {
            return new JsonParser().parse(json);
        } catch (Exception e) {
            logger.error("Bad JSON: \n{}", json);
            throw new RuntimeException("Cannot parse JSON", e);
        }
    }

}
