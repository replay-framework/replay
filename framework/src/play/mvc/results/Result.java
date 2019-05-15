package play.mvc.results;

import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.mvc.Scope.Flash;
import play.mvc.Scope.RenderArgs;
import play.mvc.Scope.Session;
import play.utils.FastRuntimeException;

/**
 * Result support
 */
public abstract class Result extends FastRuntimeException {

    protected Result() {
    }

    protected Result(String description) {
        super(description);
    }

    public abstract void apply(Request request, Response response, Session session, RenderArgs renderArgs, Flash flash);

    protected void setContentTypeIfNotSet(Response response, String contentType) {
        response.setContentTypeIfNotSet(contentType);
    }

    public boolean isRenderingTemplate() {
        return false;
    }
}
