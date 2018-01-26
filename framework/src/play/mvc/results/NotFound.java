package play.mvc.results;

import play.Play;
import play.exceptions.TemplateNotFoundException;
import play.exceptions.UnexpectedException;
import play.libs.MimeTypes;
import play.mvc.Http;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.mvc.Scope;
import play.templates.TemplateLoader;

import java.util.Map;

/**
 * 404 not found
 */
public class NotFound extends Result {

    /**
     * @param why a description of the problem
     */
    public NotFound(String why) {
        super(why);
    }

    /**
     * @param method routed method
     * @param path  routed path 
     */
    public NotFound(String method, String path) {
        super(method + " " + path);
    }

    @Override
    public void apply(Request request, Response response) {
        response.status = Http.StatusCode.NOT_FOUND;
        String format = request.format;
        if(request.isAjax() && "html".equals(format)) {
            format = "txt";
        }
        response.contentType = MimeTypes.getContentType("xx."+format);
        Map<String, Object> binding = Scope.RenderArgs.current().data;
        binding.put("result", this);
        binding.put("session", Scope.Session.current());
        binding.put("request", Http.Request.current());
        binding.put("flash", Scope.Flash.current());
        binding.put("params", Scope.Params.current());
        binding.put("play", new Play());

        String errorHtml;
        try {
            errorHtml = TemplateLoader.load("errors/404." + (format == null ? "html" : format)).render(binding);
        } catch (TemplateNotFoundException e) {
            errorHtml = "Not found";
        }

        try {
            response.out.write(errorHtml.getBytes(response.encoding));
        } catch (Exception e) {
            throw new UnexpectedException(e);
        }
    }
}
