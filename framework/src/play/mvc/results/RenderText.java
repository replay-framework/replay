package play.mvc.results;

import play.exceptions.UnexpectedException;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.mvc.Scope.Flash;
import play.mvc.Scope.RenderArgs;
import play.mvc.Scope.Session;

import static org.apache.commons.lang3.StringUtils.substring;

/**
 * 200 OK with a text/plain
 */
public class RenderText extends Result {
    
    private final String text;
    
    public RenderText(CharSequence text) {
        this.text = text.toString();
    }

    @Override
    public void apply(Request request, Response response, Session session, RenderArgs renderArgs, Flash flash) {
        try {
            setContentTypeIfNotSet(response, "text/plain; charset=" + response.encoding);
            response.out.write(text.getBytes(response.encoding));
        } catch(Exception e) {
            throw new UnexpectedException(e);
        }
    }

    public String getText() {
        return text;
    }

    @Override
    public String toString() {
        return "RenderText " + substring(text, 0, 64);
    }
}
