package play.server;

import static java.util.regex.Pattern.compile;

import java.net.InetSocketAddress;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class IpParser {

  private static final Pattern REGEX_IPV4 = compile("/(\\d+\\.\\d+\\.\\d+\\.\\d+):\\d+");
  private static final Pattern REGEX_IPV6 = compile("(.*)%.*");
  private static final Pattern REGEX_LOCALHOST = compile("^127\\.0\\.0\\.1:?\\d*$");

  @Nonnull
  @CheckReturnValue
  public String getRemoteIpAddress(InetSocketAddress address) {
    return getRemoteIpAddress(address.getAddress().getHostAddress());
  }

  @Nonnull
  @CheckReturnValue
  String getRemoteIpAddress(String address) {
    Matcher matcher = REGEX_IPV4.matcher(address);
    if (matcher.matches()) {
      return matcher.replaceFirst("$1");
    }
    Matcher matcher2 = REGEX_IPV6.matcher(address);
    if (matcher2.matches()) {
      return matcher2.replaceFirst("$1");
    }
    return address;
  }

  @CheckReturnValue
  public boolean isLoopback(String host, InetSocketAddress address) {
    try {
      return address.getAddress().isLoopbackAddress() && isLocalhost(host);
    } catch (RuntimeException ignore) {
      return false;
    }
  }

  @CheckReturnValue
  boolean isLocalhost(String host) {
    return REGEX_LOCALHOST.matcher(host).matches();
  }

  @Nonnull
  @CheckReturnValue
  public ServerAddress parseHost(@Nullable String host) {
    if (host == null) {
      return new ServerAddress("", 80, "");
    }

    // Check for IPv6 address
    if (host.startsWith("[")) {
      // There is no port
      if (host.endsWith("]")) {
        return new ServerAddress("", 80, host);
      }
      // There is a port so take from the last colon
      int portStart = host.lastIndexOf(':');
      if (portStart > 0 && (portStart + 1) < host.length()) {
        String domain = host.substring(0, portStart);
        int port = Integer.parseInt(host.substring(portStart + 1));
        return new ServerAddress(domain, port, host);
      }
    }

    // Non IPv6 but has port
    if (host.contains(":")) {
      String[] hosts = host.split(":");
      int port = Integer.parseInt(hosts[1]);
      String domain = hosts[0];
      return new ServerAddress(domain, port, host);
    }

    return new ServerAddress(host, 80, host);
  }
}
