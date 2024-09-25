package play.modules.logger;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.InputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.PlayPlugin;
import play.exceptions.UnexpectedException;

public class RePlayLogoPlugin extends PlayPlugin {
  private static final Logger logger = LoggerFactory.getLogger(RePlayLogoPlugin.class);

  /** The most important thing: A cool logo. */
  private static final String REPLAY_LOGO =
      "\n"
          + "    _____ _____  ___      ___  _\n"
          + "   /    //    / |   \\ ___|   \\| | ___ _  _\n"
          + "  /    //    /  | ' // -_) '_/| |/ _ | \\| |\n"
          + " /    //    /   |_|_\\\\___|_|  |_|\\___|\\_  /\n"
          + "/____//____/                           /_/\n"
          + "                RePlay Framework {}, https://github.com/replay-framework/replay\n";

  private static final String REPLAY_VERSION_LOCATION = "play/version";

  private String readReplayVersion() {
    try (InputStream stream =
        Thread.currentThread()
            .getContextClassLoader()
            .getResourceAsStream(REPLAY_VERSION_LOCATION)) {
      return stream == null ? "?" : new String(requireNonNull(stream).readAllBytes(), UTF_8).trim();
    } catch (IOException e) {
      throw new UnexpectedException(
          "Something is wrong with your build. Cannot read resource " + REPLAY_VERSION_LOCATION, e);
    }
  }

  @Override
  public void onApplicationStart() {
    logger.info(REPLAY_LOGO, readReplayVersion());
  }
}
