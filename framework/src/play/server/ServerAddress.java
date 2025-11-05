package play.server;

import com.google.errorprone.annotations.CheckReturnValue;
import org.jspecify.annotations.NullMarked;

@NullMarked
@CheckReturnValue
public class ServerAddress {
  public final String domain;
  public final int port;
  public final String host;

  ServerAddress(String domain, int port, String host) {
    this.host = host;
    this.port = port;
    this.domain = domain;
  }

  @Override
  public String toString() {
    return String.format("%s:%s", domain, port);
  }
}
