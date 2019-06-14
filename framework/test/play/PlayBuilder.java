package play;

import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.mvc.Scope.RenderArgs;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Properties;

/**
 * Builder-pattern-builder for Play-class..
 *
 * It's kind of odd since Play only uses statics, But it basically inits the
 * needed properties for Play-object to work in unittests
 */
public class PlayBuilder {

    public void build() {
        Play.configuration = new Properties();
        Play.applicationPath = new File(".");
    }

    public void initMvcObject() {
        if (Request.current() == null) {
            Request request = Request.createRequest(null, "GET", "/", "", null, null, null, null, false, 80, "localhost", false, null,
                    null);
            request.body = new ByteArrayInputStream(new byte[0]);
            Request.setCurrent(request);
        }

        if (Response.current() == null) {
            Response response = new Response();
            response.out = new ByteArrayOutputStream();
            response.direct = null;
            Response.setCurrent(response);
        }

        if (RenderArgs.current() == null) {
            RenderArgs renderArgs = new RenderArgs();
            RenderArgs.current.set(renderArgs);
        }
    }
}
