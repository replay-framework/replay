package controllers;

import play.mvc.Controller;
import play.mvc.results.RenderJson;

/**
 * Used by LargePostBodySpec#exerciseFileChannelBufferWithLargePostBody()
 */
public class AcceptPost extends Controller {

  public RenderJson respondWithEmptyObject() {

    // This triggers the log warning from TextParser.resetBodyInputStreamIfPossible
    request.params.get("body");

    return new RenderJson(
        String.format("{\"content-length\": %s}", request.headers.get("content-length").value()));
  }
}
