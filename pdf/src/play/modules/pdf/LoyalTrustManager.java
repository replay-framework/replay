package play.modules.pdf;

import java.security.cert.X509Certificate;
import javax.net.ssl.X509TrustManager;

class LoyalTrustManager implements X509TrustManager {
  @Override
  public X509Certificate[] getAcceptedIssuers() {
    return null;
  }

  @Override
  public void checkClientTrusted(X509Certificate[] certs, String authType) {}

  @Override
  public void checkServerTrusted(X509Certificate[] certs, String authType) {}
}
