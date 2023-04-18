package play.server;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class IpParserTest {
  private final IpParser parser = new IpParser();

  @Test
  public void getRemoteIpAddress() {
    assertThat(parser.getRemoteIpAddress("127.0.0.1")).isEqualTo("127.0.0.1");
    assertThat(parser.getRemoteIpAddress("/192.168.0.10:66")).isEqualTo("192.168.0.10");
    assertThat(parser.getRemoteIpAddress("fe80::3dd0:7f8e:57b7:34d5%19")).isEqualTo("fe80::3dd0:7f8e:57b7:34d5");
    assertThat(parser.getRemoteIpAddress("fe80::71a3:2b00:ddd3:753f%eth0")).isEqualTo("fe80::71a3:2b00:ddd3:753f");
  }

  @Test
  public void isLocalhost() {
    assertThat(parser.isLocalhost("127.0.0.1")).isTrue();
    assertThat(parser.isLocalhost("127.0.0.1:66")).isTrue();
    assertThat(parser.isLocalhost("/192.168.0.10:66")).isFalse();
    assertThat(parser.isLocalhost("fe80::3dd0:7f8e:57b7:34d5%19")).isFalse();
  }

  @Test
  public void parseHost() {
    assertThat(parser.parseHost("developer.mozilla.org")).usingRecursiveComparison()
      .isEqualTo(new ServerAddress("developer.mozilla.org", 80, "developer.mozilla.org"));

    assertThat(parser.parseHost("developer.mozilla.org:8088")).usingRecursiveComparison()
      .isEqualTo(new ServerAddress("developer.mozilla.org", 8088, "developer.mozilla.org:8088"));
  }

  @Test
  public void parseHost_null() {
    assertThat(parser.parseHost(null)).usingRecursiveComparison()
      .isEqualTo(new ServerAddress("", 80, ""));
  }

  @Test
  public void parseHost_incorrectFormat() {
    assertThat(parser.parseHost("[::1]:5001")).usingRecursiveComparison()
      .isEqualTo(new ServerAddress("[::1]", 5001, "[::1]:5001"));
  }
}