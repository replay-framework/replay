package play.data.parsing;

import play.exceptions.UnexpectedException;
import play.mvc.Http;

import java.util.HashMap;
import java.util.Map;

import static org.apache.commons.io.IOUtils.toByteArray;

public class TextParser extends DataParser {
    @Override
    public Map<String, String[]> parse(Http.Request request) {
        try {
            Map<String, String[]> params = new HashMap<>();
            byte[] data = toByteArray(request.body);
            params.put("body", new String[] {new String(data, request.encoding)});
            request.body.reset();
            return params;
        } catch (Exception e) {
            throw new UnexpectedException(e);
        }
    }
}
