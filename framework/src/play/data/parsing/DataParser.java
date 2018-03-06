package play.data.parsing;

import play.mvc.Http;

import java.util.Map;

/**
 * A data parser parse the HTTP request data to a Map&lt;String,String[]&gt;
 */
public abstract class DataParser {

    public abstract Map<String, String[]> parse(Http.Request request);

}
