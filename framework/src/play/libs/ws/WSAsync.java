package play.libs.ws;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.AsyncHttpClientConfig.Builder;
import com.ning.http.client.ProxyServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.Play;

import javax.inject.Singleton;
import javax.net.ssl.SSLContext;
import java.util.Optional;

import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.apache.commons.lang.StringUtils.isNotEmpty;

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
@Singleton
public class WSAsync implements WSClient {
    private static final Logger logger = LoggerFactory.getLogger(WSAsync.class);

    private final AsyncHttpClient httpClient;

    public WSAsync() {
        String userAgent = Play.configuration.getProperty("http.userAgent");
        String keyStore = Play.configuration.getProperty("ssl.keyStore", System.getProperty("javax.net.ssl.keyStore"));
        String keyStorePass = Play.configuration.getProperty("ssl.keyStorePassword", System.getProperty("javax.net.ssl.keyStorePassword"));
        Boolean CAValidation = Boolean.parseBoolean(Play.configuration.getProperty("ssl.cavalidation", "true"));

        Builder confBuilder = new AsyncHttpClientConfig.Builder();

        buildProxy().ifPresent(proxy -> confBuilder.setProxyServer(proxy));

        if (isNotEmpty(userAgent)) {
            confBuilder.setUserAgent(userAgent);
        }

        if (isNotEmpty(keyStore)) {
            logger.info("Keystore configured, loading from '{}', CA validation enabled : {}", keyStore, CAValidation);
            SSLContext sslCTX = WSSSLContext.getSslContext(keyStore, keyStorePass, CAValidation);
            logger.trace("Keystore password : {}, SSLCTX : {}", keyStorePass, sslCTX);
            confBuilder.setSSLContext(sslCTX);
        }

        // when using raw urls, AHC does not encode the params in url.
        // this means we can/must encode it(with correct encoding) before
        // passing it to AHC
        confBuilder.setDisableUrlEncodingForBoundedRequests(true);
        httpClient = new AsyncHttpClient(confBuilder.build());
    }

    private Optional<ProxyServer> buildProxy() {
        String proxyHost = Play.configuration.getProperty("http.proxyHost", System.getProperty("http.proxyHost"));
        if (isEmpty(proxyHost)) return Optional.empty();

        String proxyPort = Play.configuration.getProperty("http.proxyPort", System.getProperty("http.proxyPort"));
        String proxyUser = Play.configuration.getProperty("http.proxyUser", System.getProperty("http.proxyUser"));
        String proxyPassword = Play.configuration.getProperty("http.proxyPassword", System.getProperty("http.proxyPassword"));
        String nonProxyHosts = Play.configuration.getProperty("http.nonProxyHosts", System.getProperty("http.nonProxyHosts"));

        ProxyServer proxy = new ProxyServer(proxyHost, parseProxyPort(proxyPort), proxyUser, proxyPassword);
        if (isNotEmpty(nonProxyHosts)) {
            for (String url : nonProxyHosts.split("\\|")) {
                proxy.addNonProxyHost(url);
            }
        }
        return Optional.of(proxy);
    }

    private int parseProxyPort(String proxyPort) {
        try {
            return Integer.parseInt(proxyPort);
        } catch (NumberFormatException e) {
            logger.error(
              "Cannot parse the proxy port property '{}'. Check property http.proxyPort either in System configuration or in Play config file.",
              proxyPort, e);
            throw new IllegalStateException("WS proxy is misconfigured -- check the logs for details");
        }
    }

    @Override
    public void stop() {
        logger.trace("Releasing http client connections...");
        httpClient.close();
    }

    @Override
    public WSRequest newRequest(String url) {
        return new WSAsyncRequest(httpClient, url, Play.defaultWebEncoding);
    }

}
