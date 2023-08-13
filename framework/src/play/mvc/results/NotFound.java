package play.mvc.results;

import play.Play;
import play.exceptions.TemplateNotFoundException;
import play.exceptions.UnexpectedException;
import play.libs.MimeTypes;
import play.mvc.Http;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.mvc.Scope.Flash;
import play.mvc.Scope.RenderArgs;
import play.mvc.Scope.Session;
import play.templates.TemplateLoader;

import java.util.Map;

/**
 * 404 not found
 */
public class NotFound extends Error {

    /**
     * @param why a description of the problem
     */
    public NotFound(String why) {
        super(Http.StatusCode.NOT_FOUND, why);
    }

    /**
     * @param method routed method
     * @param path  routed path 
     */
    public NotFound(String method, String path) {
        this(method + " " + path);
    }

    @Override
    public boolean isRenderingTemplate() {
        return true;
    }
}
