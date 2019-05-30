package play.libs.ws;

import org.apache.commons.lang.NotImplementedException;
import play.libs.Codec;
import play.libs.Promise;
import play.libs.Time;
import play.utils.HTTP;

import java.io.File;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class WSRequest {
    final String url;

    /**
     * The virtual host this request will use
     */
    public String virtualHost;
    protected final Charset encoding;
    public String username;
    public String password;
    public Scheme scheme;
    public Object body;
    public FileParam[] fileParams;
    public final Map<String, String> headers = new LinkedHashMap<>();
    public final Map<String, Object> parameters = new LinkedHashMap<>();
    public String mimeType;

    /**
     * Sets whether redirects (301, 302) should be followed automatically
     */
    public boolean followRedirects = true;

    /**
     * Timeout: value in seconds
     */
    public int timeout = 60;

    protected WSRequest(String url, Charset encoding) {
        this.url = normalizeURL(url);
        this.encoding = encoding;
    }

    private static String normalizeURL(String url) {
        try {
            return new URI(url).toASCIIString();
        } catch (Exception e) {
            return url;
        }
    }

    /**
     * Sets the virtual host to use in this request
     *
     * @param virtualHost
     *            The given virtual host
     * @return the WSRequest
     */
    public WSRequest withVirtualHost(String virtualHost) {
        this.virtualHost = virtualHost;
        return this;
    }

    /**
     * Add a MimeType to the web service request.
     *
     * @param mimeType
     *            the given mimeType
     * @return the WSRequest for chaining.
     */
    public WSRequest mimeType(String mimeType) {
        this.mimeType = mimeType;
        return this;
    }

    /**
     * Define client authentication for a server host provided credentials will be used during the request
     *
     * @param username
     *            Login
     * @param password
     *            Password
     * @param scheme
     *            The given Scheme
     * @return the WSRequest for chaining.
     */
    public WSRequest authenticate(String username, String password, Scheme scheme) {
        this.username = username;
        this.password = password;
        this.scheme = scheme;
        return this;
    }

    /**
     * define client authentication for a server host provided credentials will be used during the request the basic
     * scheme will be used
     *
     * @param username
     *            Login
     * @param password
     *            Password
     * @return the WSRequest for chaining.
     */
    public WSRequest authenticate(String username, String password) {
        return authenticate(username, password, Scheme.BASIC);
    }

    /**
     * Indicate if the WS should continue when hitting a 301 or 302
     *
     * @param value
     *            Indicate if follow or not follow redirects
     * @return the WSRequest for chaining.
     */
    public WSRequest followRedirects(boolean value) {
        this.followRedirects = value;
        return this;
    }

    /**
     * Set the value of the request timeout, i.e. the number of seconds before cutting the connection - default to
     * 60 seconds
     *
     * @param timeout
     *            the timeout value, e.g. "30s", "1min"
     * @return the WSRequest for chaining
     */
    public WSRequest timeout(String timeout) {
        this.timeout = Time.parseDuration(timeout);
        return this;
    }

    /**
     * Add files to request. This will only work with POST or PUT.
     *
     * @param files
     *            list of files
     * @return the WSRequest for chaining.
     */
    public WSRequest files(File... files) {
        this.fileParams = FileParam.getFileParams(files);
        return this;
    }

    /**
     * Add fileParams aka File and Name parameter to the request. This will only work with POST or PUT.
     *
     * @param fileParams
     *            The fileParams list
     * @return the WSRequest for chaining.
     */
    public WSRequest files(FileParam... fileParams) {
        this.fileParams = fileParams;
        return this;
    }

    /**
     * Add the given body to the request.
     *
     * @param body
     *            The request body
     * @return the WSRequest for chaining.
     */
    public WSRequest body(Object body) {
        this.body = body;
        return this;
    }

    /**
     * Add a header to the request
     *
     * @param name
     *            header name
     * @param value
     *            header value
     * @return the WSRequest for chaining.
     */
    public WSRequest setHeader(String name, String value) {
        this.headers.put(HTTP.fixCaseForHttpHeader(name), value);
        return this;
    }

    /**
     * Add a parameter to the request
     *
     * @param name
     *            parameter name
     * @param value
     *            parameter value
     * @return the WSRequest for chaining.
     */
    public WSRequest setParameter(String name, String value) {
        this.parameters.put(name, value);
        return this;
    }

    public WSRequest setParameter(String name, Object value) {
        this.parameters.put(name, value);
        return this;
    }

    /**
     * Use the provided headers when executing request.
     *
     * @param headers
     *            The request headers
     * @return the WSRequest for chaining.
     */
    public WSRequest headers(Map<String, String> headers) {
        this.headers.putAll(headers);
        return this;
    }

    /**
     * Add parameters to request. If POST or PUT, parameters are passed in body using x-www-form-urlencoded if
     * alone, or form-data if there is files too. For any other method, those params are appended to the
     * queryString.
     *
     * @param parameters
     *            The request parameters
     *
     * @return the WSRequest for chaining.
     */
    public WSRequest params(Map<String, String> parameters) {
        this.parameters.putAll(parameters);
        return this;
    }

    /**
     * Execute a GET request synchronously.
     *
     * @return The HTTP response
     */
    public abstract HttpResponse get();

    /**
     * Execute a GET request asynchronously.
     *
     * @return The HTTP response
     */
    public Promise<HttpResponse> getAsync() {
        throw new NotImplementedException();
    }

    /**
     * Execute a PATCH request.
     *
     * @return The HTTP response
     */
    public abstract HttpResponse patch();

    /**
     * Execute a PATCH request asynchronously.
     *
     * @return The HTTP response
     */
    public Promise<HttpResponse> patchAsync() {
        throw new NotImplementedException();
    }

    /**
     * Execute a POST request.
     *
     * @return The HTTP response
     */
    public abstract HttpResponse post();

    /**
     * Execute a POST request asynchronously.
     *
     * @return The HTTP response
     */
    public Promise<HttpResponse> postAsync() {
        throw new NotImplementedException();
    }

    /**
     * Execute a PUT request.
     *
     * @return The HTTP response
     */
    public abstract HttpResponse put();

    /**
     * Execute a PUT request asynchronously.
     *
     * @return The HTTP response
     */
    public Promise<HttpResponse> putAsync() {
        throw new NotImplementedException();
    }

    /**
     * Execute a DELETE request.
     *
     * @return The HTTP response
     */
    public abstract HttpResponse delete();

    /**
     * Execute a DELETE request asynchronously.
     *
     * @return The HTTP response
     */
    public Promise<HttpResponse> deleteAsync() {
        throw new NotImplementedException();
    }

    /**
     * Execute a OPTIONS request.
     *
     * @return The HTTP response
     */
    public abstract HttpResponse options();

    /**
     * Execute a OPTIONS request asynchronously.
     *
     * @return The HTTP response
     */
    public Promise<HttpResponse> optionsAsync() {
        throw new NotImplementedException();
    }

    /**
     * Execute a HEAD request.
     *
     * @return The HTTP response
     */
    public abstract HttpResponse head();

    /**
     * Execute a HEAD request asynchronously.
     *
     * @return The HTTP response
     */
    public Promise<HttpResponse> headAsync() {
        throw new NotImplementedException();
    }

    /**
     * Execute a TRACE request.
     *
     * @return The HTTP response
     */
    public abstract HttpResponse trace();

    /**
     * Execute a TRACE request asynchronously.
     *
     * @return The HTTP response
     */
    public Promise<HttpResponse> traceAsync() {
        throw new NotImplementedException();
    }

    protected String basicAuthHeader() {
        return "Basic " + Codec.encodeBASE64(this.username + ":" + this.password);
    }

    protected String encode(String part) {
        try {
            return URLEncoder.encode(part, encoding);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
