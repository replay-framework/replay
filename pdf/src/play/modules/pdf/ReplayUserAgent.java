package play.modules.pdf;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xhtmlrenderer.pdf.ITextOutputDevice;
import org.xhtmlrenderer.pdf.ITextUserAgent;
import play.Play;
import play.vfs.VirtualFile;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.regex.Pattern;

import static org.xhtmlrenderer.pdf.ITextRenderer.DEFAULT_DOTS_PER_PIXEL;

@ParametersAreNonnullByDefault
public class ReplayUserAgent extends ITextUserAgent {
  private static final Logger logger = LoggerFactory.getLogger(ReplayUserAgent.class);
  private static final Pattern REGEX_URL_QUERY = Pattern.compile("\\?.*");
  private final FileSearcher fileSearcher;

  public ReplayUserAgent(ITextOutputDevice outputDevice) {
    this(outputDevice, new FileSearcher());
  }

  ReplayUserAgent(ITextOutputDevice outputDevice, FileSearcher fileSearcher) {
    super(outputDevice, DEFAULT_DOTS_PER_PIXEL);
    this.fileSearcher = fileSearcher;
  }

  @Override
  protected InputStream resolveAndOpenStream(String uri) {
    trustCertsIfNeeded();
    return super.resolveAndOpenStream(uri);
  }

  private void trustCertsIfNeeded() {
    if ("true".equals(Play.configuration.getProperty("play.pdf.ssl.acceptUnknownCertificate", "false"))) {
      try {
        trustCerts();
      }
      catch (NoSuchAlgorithmException | KeyManagementException e) {
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
      VirtualFile file = fileSearcher.searchFor(filePath);
      logger.debug("Resolved uri {} to file {}", uri, file == null ? null : file.getRealFile().getAbsolutePath());
      if (file != null && file.exists()) {
        return toAbsoluteUrl(file);
      }
    }

    return super.resolveURI(uri);
  }

  private String toAbsoluteUrl(VirtualFile file) {
    try {
      return file.getRealFile().getCanonicalFile().toURI().toURL().toExternalForm();
    }
    catch (IOException e) {
      throw new RuntimeException("Failed to convert to URL: " + file.getRealFile(), e);
    }
  }
}
