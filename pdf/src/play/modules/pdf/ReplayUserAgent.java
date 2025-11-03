package play.modules.pdf;

import static com.google.common.cache.CacheBuilder.newBuilder;
import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static org.xhtmlrenderer.pdf.ITextRenderer.DEFAULT_DOTS_PER_PIXEL;

import com.google.common.cache.Cache;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xhtmlrenderer.pdf.ITextOutputDevice;
import org.xhtmlrenderer.pdf.ITextUserAgent;
import org.xhtmlrenderer.resource.ImageResource;
import play.Play;

@ParametersAreNonnullByDefault
public class ReplayUserAgent extends ITextUserAgent {

  private static final Logger logger = LoggerFactory.getLogger(ReplayUserAgent.class);
  private static final Pattern REGEX_URL_QUERY = Pattern.compile("\\?.*");
  private static final Cache<String, ImageResource> imageCache = newBuilder()
      .expireAfterAccess(1, MINUTES)
      .expireAfterWrite(1, HOURS)
      .maximumSize(100)
      .build();

  private final FileSearcher fileSearcher;

  public ReplayUserAgent(ITextOutputDevice outputDevice) {
    this(outputDevice, new FileSearcher());
  }

  ReplayUserAgent(ITextOutputDevice outputDevice, FileSearcher fileSearcher) {
    super(outputDevice, DEFAULT_DOTS_PER_PIXEL);
    this.fileSearcher = fileSearcher;
  }

  @Override
  @Nonnull
  public ImageResource getImageResource(String uri) {
    try {
      return imageCache.get(uri, () -> super.getImageResource(uri));
    } catch (ExecutionException e) {
      throw new RuntimeException("Failed to load image from " + uri, e);
    }
  }

  @Override
  protected InputStream resolveAndOpenStream(@Nullable String uri) {
    trustCertsIfNeeded();
    return super.resolveAndOpenStream(uri);
  }

  private void trustCertsIfNeeded() {
    if (Play.configuration.property("play.pdf.ssl.acceptUnknownCertificate", "false")
        .hasValue("true")) {
      try {
        trustCerts();
      } catch (NoSuchAlgorithmException | KeyManagementException e) {
        logger.error("Failed to trust all SSL certs", e);
      }
    }
  }

  private void trustCerts() throws NoSuchAlgorithmException, KeyManagementException {
    TrustManager[] trustAllCerts = new TrustManager[]{new LoyalTrustManager()};

    // Install the all-trusting trust manager
    SSLContext sc = SSLContext.getInstance("SSL");
    sc.init(null, trustAllCerts, new SecureRandom());
    HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

    // Create all-trusting host name verifier
    HostnameVerifier allHostsValid = (hostname, session) -> true;

    // Install the all-trusting host verifier
    HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
  }

  @Nullable
  @Override
  public String resolveURI(@Nullable String uri) {
    if (uri != null) {
      // try to find it in play
      String filePath = REGEX_URL_QUERY.matcher(uri).replaceFirst("");
      File file = fileSearcher.searchFor(filePath);
      logger.debug("Resolved uri {} to file {}", uri, file == null ? null : file.getAbsolutePath());
      if (file != null && file.exists()) {
        return toAbsoluteUrl(file);
      }
    }

    return super.resolveURI(uri);
  }

  private String toAbsoluteUrl(File file) {
    try {
      return file.getCanonicalFile().toURI().toURL().toExternalForm();
    } catch (IOException e) {
      throw new RuntimeException("Failed to convert to URL: " + file, e);
    }
  }
}
