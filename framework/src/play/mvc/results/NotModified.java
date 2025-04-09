package play.mvc.results;

import play.mvc.Http;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.mvc.Scope.Flash;
import play.mvc.Scope.RenderArgs;
import play.mvc.Scope.Session;

/** 304 Not Modified */
public class NotModified extends Result {

  private String eTag;

  public NotModified() {
    super("NotModified");
  }

  public NotModified(String eTag) {
    this.eTag = eTag;
  }

  @Override
  public void apply(
      Request request, Response response, Session session, RenderArgs renderArgs, Flash flash) {
    response.status = Http.StatusCode.NOT_MODIFIED;
    if (eTag != null) {
      response.setHeader("Etag", eTag);
    }
  }

  public String getETag() {
    return eTag;
  }
}
