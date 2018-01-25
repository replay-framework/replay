package play.data.parsing;

import play.exceptions.UnexpectedException;
import play.mvc.Http;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

public class TextParser extends DataParser {

    @Override
    public Map<String, String[]> parse(Http.Request request) {
        try {
            Map<String, String[]> params = new HashMap<>();
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            int b;
            while ((b = request.body.read()) != -1) {
                os.write(b);
            }
            byte[] data = os.toByteArray();
            params.put("body", new String[] {new String(data, request.encoding)});
            return params;
        } catch (Exception e) {
            throw new UnexpectedException(e);
        }
    }

}
