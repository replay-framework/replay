package play.mvc.results;

import play.exceptions.UnexpectedException;
import play.mvc.Http;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.mvc.Scope.Flash;
import play.mvc.Scope.RenderArgs;
import play.mvc.Scope.Session;

/**
 * 302 Redirect
 */
public class RedirectToStatic extends Result {

    private final String file;
    
    public RedirectToStatic(String file) {
        this.file = file;
    }

    @Override
    public void apply(Request request, Response response, Session session, RenderArgs renderArgs, Flash flash) {
        try {
            response.status = Http.StatusCode.FOUND;
            response.setHeader("Location", file);
        } catch (Exception e) {
            throw new UnexpectedException(e);
        }
    }

    public String getFile() {
        return file;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " " + file;
    }
}
