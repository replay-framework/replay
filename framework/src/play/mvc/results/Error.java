package play.mvc.results;

import play.Play;
import play.mvc.Http;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.mvc.Scope.Flash;
import play.mvc.Scope.RenderArgs;
import play.mvc.Scope.Session;

/**
 * 500 Error
 */
public class Error extends Result {

    protected final int status;

    public Error(String reason) {
        super(reason);
        this.status = Http.StatusCode.INTERNAL_ERROR;
    }

    public Error(int status, String reason) {
        super(reason);
        this.status = status;
    }

    @Override
    public void apply(Request request, Response response, Session session, RenderArgs renderArgs, Flash flash) {
        Play.errorHandler.applicationErrorResponse(this, request, response, session, renderArgs, flash);
    }

    public int getStatus() {
        return status;
    }

    @Override
    public boolean isRenderingTemplate() {
        return true;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " \"" + getMessage() + "\"";
    }
}
