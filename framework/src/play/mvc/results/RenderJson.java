package play.mvc.results;

import com.google.gson.Gson;
import play.exceptions.UnexpectedException;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.mvc.Scope.Flash;
import play.mvc.Scope.RenderArgs;
import play.mvc.Scope.Session;

import static org.apache.commons.lang3.StringUtils.substring;

/**
 * 200 OK with application/json
 */
public class RenderJson extends Result {

    private static final Gson GSON = new Gson();
    
    private final String json;
    private final Object response;

    public RenderJson(Object response) {
        this.response = response;
        json = GSON.toJson(response);
    }

    public RenderJson(String jsonString) {
        json = jsonString;
        this.response = null;
    }

    public RenderJson(Object response, Gson gson) {
        this.response = response;
        if (gson != null) {
            json = gson.toJson(response);
        } else {
            json = GSON.toJson(response);
        }
    }

    @Override
    public void apply(Request request, Response httpResponse, Session session, RenderArgs renderArgs, Flash flash) {
        try {
            setContentTypeIfNotSet(httpResponse, "application/json; charset=" + httpResponse.encoding);
            httpResponse.out.write(json.getBytes(httpResponse.encoding));
        } catch (Exception e) {
            throw new UnexpectedException(e);
        }
    }

    public String getJson() {
        return json;
    }

    public Object getResponse() {
        return response;
    }

    @Override
    public String toString() {
        return "RenderJson " + substring(json, 0, 64);
    }
}
