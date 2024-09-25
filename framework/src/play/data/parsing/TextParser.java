package play.data.parsing;

import static org.apache.commons.io.IOUtils.toByteArray;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.exceptions.UnexpectedException;
import play.mvc.Http;

public class TextParser extends DataParser {
  private static final Logger log = LoggerFactory.getLogger(TextParser.class);

  @Override
  public Map<String, String[]> parse(Http.Request request) {
    Map<String, String[]> params = new HashMap<>();
    try {
      byte[] data = toByteArray(request.body);
      params.put("body", new String[] {new String(data, request.encoding)});
    } catch (IOException e) {
      throw new UnexpectedException(e);
    }
    resetBodyInputStreamIfPossible(request);
    return params;
  }

  private void resetBodyInputStreamIfPossible(Http.Request request) {
    try {
      request.body.reset();
    } catch (IOException resetNotSupported) {
      log.warn(
          "Failed to reset request.body of type {}: {}",
          resetNotSupported.getClass().getName(),
          resetNotSupported.toString());
    }
  }
}
