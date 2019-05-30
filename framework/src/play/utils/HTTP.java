package play.utils;

import org.apache.commons.io.IOUtils;
import play.Play;
import play.exceptions.UnexpectedException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.unmodifiableMap;
import static org.apache.commons.lang.StringUtils.isNotBlank;

public class HTTP {

    public static class ContentTypeWithEncoding {
        public final String contentType;
        public final Charset encoding;

        public ContentTypeWithEncoding(String contentType, Charset encoding) {
            this.contentType = contentType;
            this.encoding = encoding;
        }
    }

    public static ContentTypeWithEncoding parseContentType(String contentType) {
        if (contentType == null) {
            return new ContentTypeWithEncoding("text/html", Play.defaultWebEncoding);
        }

        String[] contentTypeParts = contentType.split(";");
        String _contentType = contentTypeParts[0].trim().toLowerCase();
        Charset _encoding = parseEncoding(contentTypeParts).orElse(Play.defaultWebEncoding);
        return new ContentTypeWithEncoding(_contentType, _encoding);
    }

    private static Optional<Charset> parseEncoding(String[] contentTypeParts) {
        // check for encoding-info
        if (contentTypeParts.length >= 2) {
            String[] encodingInfoParts = contentTypeParts[1].split(("="));
            if (encodingInfoParts.length == 2 && "charset".equalsIgnoreCase(encodingInfoParts[0].trim())) {
                // encoding-info was found in request
                String _encoding = encodingInfoParts[1].trim();

                if (isNotBlank(_encoding) && ((_encoding.startsWith("\"") && _encoding.endsWith("\""))
                        || (_encoding.startsWith("'") && _encoding.endsWith("'")))) {
                    _encoding = _encoding.substring(1, _encoding.length() - 1).trim();
                }

                return Optional.of(Charset.forName(_encoding));
            }
        }
        return Optional.empty();
    }

    private static final Map<String, String> lower2UppercaseHttpHeaders = initLower2UppercaseHttpHeaders();

    private static Map<String, String> initLower2UppercaseHttpHeaders() {
        Map<String, String> map = new HashMap<>();

        String path = "/play/utils/http_headers.properties";

        try (InputStream in = HTTP.class.getResourceAsStream(path)) {
          if (in == null) {
            throw new RuntimeException("Error reading " + path);
          }
          List<String> lines = IOUtils.readLines(in, UTF_8);
          for (String line : lines) {
            line = line.trim();
            if (!line.startsWith("#")) {
              map.put(line.toLowerCase(), line);
            }
          }
        }
        catch (IOException e) {
            throw new UnexpectedException(e);
        }

        return unmodifiableMap(map);
    }

    /**
     * Use this method to make sure you have the correct casing of a http header name. eg: fixes 'content-type' to
     * 'Content-Type'
     * 
     * @param headerName
     *            The given header name to check
     * @return The correct header name
     */
    public static String fixCaseForHttpHeader(String headerName) {
        if (headerName == null) {
            return null;
        }
        String correctCase = lower2UppercaseHttpHeaders.get(headerName.toLowerCase());
        if (correctCase != null) {
            return correctCase;
        }
        // Didn't find it - return it as it is
        return headerName;
    }
}
