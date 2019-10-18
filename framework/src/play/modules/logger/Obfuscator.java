package play.modules.logger;

import javax.annotation.Nullable;
import java.util.regex.Pattern;

public class Obfuscator {
  private final Pattern CARD_NUMBER_PATTERN = Pattern.compile("((?:\\b|\\D)[0-9]{6})([0-9]{6,9})([0-9]{4}(?:\\b|\\D))");

  public boolean isLikeCardNumber(String text) {
    return text != null && CARD_NUMBER_PATTERN.matcher(text).matches();
  }

  @Nullable
  public String maskCardNumber(String text) {
    return text == null ? null : CARD_NUMBER_PATTERN.matcher(text).replaceAll("$1xxxxxx$3");
  }
}
