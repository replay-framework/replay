package controllers;

import static java.util.Objects.requireNonNullElse;

import java.util.Map;
import play.mvc.Controller;
import play.mvc.results.RenderJson;

/** Used by LargePostBodySpec#exerciseFileChannelBufferWithLargePostBody() */
public class AcceptPost extends Controller {

  public RenderJson respondWithSameObject() {

    // This triggers the log warning from TextParser.resetBodyInputStreamIfPossible
    String requestBody = requireNonNullElse(request.params.get("body"), "");

    return new RenderJson(
        Map.of(
            "content-length",
            request.headers.get("content-length").value(),
            "origin",
            requestBody));
  }
}
