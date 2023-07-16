package play.modules.logger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.PlayPlugin;
import play.exceptions.UnexpectedException;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

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
		"                   RePlay Framework {}, https://github.com/codeborne/replay\n";

	private static final String REPLAY_VERSION_LOCATION = "play/version";

	private String readReplayVersion() {
		try (InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(REPLAY_VERSION_LOCATION)) {
			return new String(Objects.requireNonNull(stream).readAllBytes(), StandardCharsets.UTF_8).trim();
		} catch (Exception e) {
			throw new UnexpectedException("Something is wrong with your build. Cannot find resource " + REPLAY_VERSION_LOCATION);
		}
	}
	@Override
	public void onApplicationStart() {
		logger.info(REPLAY_LOGO, readReplayVersion());
	}
}
