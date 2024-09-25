package play.data.parsing;

import java.util.Map;
import play.mvc.Http;

/** A data parser parse the HTTP request data to a Map&lt;String,String[]&gt; */
public abstract class DataParser {

  public abstract Map<String, String[]> parse(Http.Request request);
}
