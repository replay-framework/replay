package play.libs;

import play.PlayPlugin;
import play.libs.ws.WSAsync;
import play.libs.ws.WSClient;
import play.libs.ws.WSRequest;

/**
 * Simple HTTP client to make webservices requests.
 * 
 * <p>
 * Get latest BBC World news as a RSS content
 * 
 * <pre>
 * HttpResponse response = WS.url("http://newsrss.bbc.co.uk/rss/newsonline_world_edition/front_page/rss.xml").get();
 * Document xmldoc = response.getXml();
 * // the real pain begins here...
 * </pre>
 * <p>
 * 
 * Search what Yahoo! thinks of google (starting from the 30th result).
 * 
 * <pre>
 * HttpResponse response = WS.url("http://search.yahoo.com/search?p=<em>%s</em>&amp;pstart=1&amp;b=<em>%s</em>", "Google killed me", "30").get();
 * if (response.getStatus() == 200) {
 *     html = response.getString();
 * }
 * </pre>
 */
public class WS extends PlayPlugin {
    /**
     * Singletons configured with default encoding - this one is used when calling static method on WS.
     */
    private static WSClient wsClient;

    @Override
    public void onApplicationStop() {
        if (wsClient != null) {
            wsClient.stop();
            wsClient = null;
        }
    }

    @Override
    public void onApplicationStart() {
        wsClient = new WSAsync();
    }

    /**
     * Build a WebService Request with the given URL. This object support chaining style programming for adding params,
     * file, headers to requests.
     * 
     * @param url
     *            of the request
     * @return a WSRequest on which you can add params, file headers using a chaining style programming.
     *
     * @deprecated Using this static method make it hard to test. A better option is to @Inject WSClient instance to your class.
     */
    @Deprecated
    public static WSRequest url(String url) {
        return wsClient.newRequest(url);
    }
}
