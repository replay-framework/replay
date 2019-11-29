package play.mvc;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static play.mvc.Http.Request.cleanupRemoteAddresses;
import static play.mvc.Http.Request.validateXForwarded;

public class HttpTest {
  @Test
  public void validatesXForwardedFor() {
    validateXForwarded(header(""));
    validateXForwarded(header("0.0.0.0 "));
    validateXForwarded(header("192.168.1.1 "));
    validateXForwarded(header("255.255.255.255 "));
    validateXForwarded(header("0000:0000:0000:0000:0000:0000:0000:0000 "));
    validateXForwarded(header("fe00::1 "));
    validateXForwarded(header("fe80::217:f2ff:fe07:ed62 "));
    validateXForwarded(header("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff "));
    validateXForwarded(header("2001:0db8:0000:85a3:0000:0000:ac1f:800 "));
    validateXForwarded(header("0000:0000:0000:0000:0000:0000:0000:0000/128"));
    validateXForwarded(header(" ::1/128, 2001:db8::/48, 192.168.1.1/12 "));
    validateXForwarded(header("192.168.224.0 1"));
    validateXForwarded(header("192.168. 224.0"));
    validateXForwarded(header("2001:0000:1234:0000:0000:C1C0:ABCD:0876 0"));

    assertInvalidHeader("1.2.3.4'\"><b>xss</b>");
    assertInvalidHeader("\"");
    assertInvalidHeader("\\");
    assertInvalidHeader(">");
    assertInvalidHeader("<");
    assertInvalidHeader("'");
  }

  @Test
  public void removesPotentiallyUnsafeAddresses_from_XForwardedForHeader() {
    assertThat(cleanupRemoteAddresses("")).isEqualTo("");
    assertThat(cleanupRemoteAddresses("192.168.224.0")).isEqualTo("192.168.224.0");
    assertThat(cleanupRemoteAddresses("provided-by-hacker, provided-by-apache")).isEqualTo("provided-by-apache");
    assertThat(cleanupRemoteAddresses("provided-by-hacker1, provided-by-hacker2, provided-by-apache")).isEqualTo("provided-by-apache");
  }

  private void assertInvalidHeader(String ip) {
    Assertions.assertThatThrownBy(() -> validateXForwarded(header(ip)))
      .isInstanceOf(RuntimeException.class)
      .hasMessage("Unacceptable X-Forwarded-For format: " + ip);
  }

  private Http.Header header(String ips) {
    return new Http.Header("X-Forwarded-For", ips);
  }
}