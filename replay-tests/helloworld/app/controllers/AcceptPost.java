package controllers;

import play.mvc.Controller;
import play.mvc.results.RenderJson;

/** Used by LargePostBodySpec#exerciseFileChannelBufferWithLargePostBody() */
public class AcceptPost extends Controller {

  public RenderJson respondWithEmptyObject() {

    // this triggers the log warning from TextParser.resetBodyInputStreamIfPossible
    String body = request.params.get("body");

    return new RenderJson("{}");
  }
}
