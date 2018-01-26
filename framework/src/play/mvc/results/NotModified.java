package play.mvc.results;

import play.mvc.Http;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.mvc.Scope.Flash;
import play.mvc.Scope.RenderArgs;
import play.mvc.Scope.Session;

/**
 * 304 Not Modified
 */
public class NotModified extends Result {

    private String etag;

    public NotModified() {
        super("NotModified");
    }

    public NotModified(String etag) {
        this.etag = etag;
    }

    @Override
    public void apply(Request request, Response response, Session session, RenderArgs renderArgs, Flash flash) {
        response.status = Http.StatusCode.NOT_MODIFIED;
        if (etag != null) {
            response.setHeader("Etag", etag);
        }
    }

    public String getEtag() {
        return etag;
    }
}
