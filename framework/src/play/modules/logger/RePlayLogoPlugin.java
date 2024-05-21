package play.modules.logger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.PlayPlugin;
import play.exceptions.UnexpectedException;

import java.io.IOException;
import java.io.InputStream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;

public class RePlayLogoPlugin extends PlayPlugin {
	private static final Logger logger = LoggerFactory.getLogger(RePlayLogoPlugin.class);


	/**
	 * The most important thing: A cool logo.
	 */
	private static final String REPLAY_LOGO = "\n" +
		"    ______  ______                 _            _\n" +
		"   /     / /     /  _ __ ___ _ __ | | __ _ _  _| |\n" +
		"  /     / /     /  | '_ / -_) '_ \\| |/ _' | || |_|\n" +
		" /     / /     /   |_/  \\___|  __/|_|\\____|\\__ (_)\n" +
		"/_____/ /_____/             |_|            |__/\n" +
		"                   RePlay Framework {}, https://github.com/replay-framework/replay\n";

	private static final String REPLAY_VERSION_LOCATION = "play/version";

	private String readReplayVersion() {
		try (InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(REPLAY_VERSION_LOCATION)) {
			return stream == null ? "?" : new String(requireNonNull(stream).readAllBytes(), UTF_8).trim();
		} catch (IOException e) {
			throw new UnexpectedException("Something is wrong with your build. Cannot read resource " + REPLAY_VERSION_LOCATION, e);
		}
	}
	@Override
	public void onApplicationStart() {
		logger.info(REPLAY_LOGO, readReplayVersion());
	}
}
