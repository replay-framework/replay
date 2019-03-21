package play.mvc.results;

import play.exceptions.UnexpectedException;
import play.mvc.Http;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.mvc.Scope.Flash;
import play.mvc.Scope.RenderArgs;
import play.mvc.Scope.Session;
import play.mvc.Url;

import java.util.Map;

import static java.util.Collections.emptyMap;

/**
 * 302 Redirect
 */
public class Redirect extends Result {

    public String url;
    private final Map<String, String> flashMessages;
    public int code = Http.StatusCode.FOUND;

    public Redirect(String url) {
        this.url = url;
        this.flashMessages = emptyMap();
    }

    /**
     * Redirects to a given URL with the parameters specified in a {@link Map}
     *
     * @param url
     *            The URL to redirect to as a {@link String}
     * @param parameters
     *            Parameters to be included at the end of the URL as a HTTP GET. This is a map whose entries are written out as key1=value1&amp;key2=value2 etc..
     */
    public Redirect(String url, Map<String, Object> parameters) {
        this(url, parameters, emptyMap());
    }

    public Redirect(String url, Map<String, Object> parameters, Map<String, String> flashMessages) {
        this.url = new Url(url, parameters).toString();
        this.flashMessages = flashMessages;
    }

    public Redirect(String url,boolean permanent) {
        this.url = url;
        this.flashMessages = emptyMap();
        if (permanent)
            this.code = Http.StatusCode.MOVED;
    }

    public Redirect(String url,int code) {
        this.url = url;
        this.code=code;
        this.flashMessages = emptyMap();
    }

    @Override
    public void apply(Request request, Response response, Session session, RenderArgs renderArgs, Flash flash) {
        try {
            this.flashMessages.forEach((key, value) -> flash.put(key, value));

            // do not touch any valid uri: http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.30
            if (url.matches("^\\w+://.*")) {
            } else if (url.startsWith("/")) {
                url = String.format("http%s://%s%s%s", request.secure ? "s" : "", request.domain, (request.port == 80 || request.port == 443) ? "" : ":" + request.port, url);
            } else {
                url = String.format("http%s://%s%s%s%s", request.secure ? "s" : "", request.domain, (request.port == 80 || request.port == 443) ? "" : ":" + request.port, request.path, request.path.endsWith("/") ? url : "/" + url);
            }
            response.status = code;
            response.setHeader("Location", url);
        } catch (Exception e) {
            throw new UnexpectedException(e);
        }
    }

    public String getUrl() {
        return url;
    }

    public int getCode() {
        return code;
    }

    @Override public String toString() {
        return String.format("redirect:%s:%s", code, url);
    }
}
