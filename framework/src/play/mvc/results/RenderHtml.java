package play.mvc.results;

import play.exceptions.UnexpectedException;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.mvc.Scope.Flash;
import play.mvc.Scope.RenderArgs;
import play.mvc.Scope.Session;

/**
 * 200 OK with a text/plain
 */
public class RenderHtml extends Result {
    
    private final String html;
    
    public RenderHtml(CharSequence html) {
        this.html = html.toString();
    }

    @Override
    public void apply(Request request, Response response, Session session, RenderArgs renderArgs, Flash flash) {
        try {
            setContentTypeIfNotSet(response, "text/html");
            response.out.write(html.getBytes(response.encoding));
        } catch(Exception e) {
            throw new UnexpectedException(e);
        }
    }

    public String getHtml() {
        return html;
    }
}
