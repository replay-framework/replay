package play.utils;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import play.libs.Crypter;

@ParametersAreNonnullByDefault
public class ErrorsCookieCrypter {
  private final Crypter crypter = new Crypter("errors-");

  @Nonnull
  @CheckReturnValue
  public String encrypt(String errorsCookie) {
    return crypter.encryptAES(Math.random() + ":" + errorsCookie);
  }

  @Nonnull
  @CheckReturnValue
  public String decrypt(String errorsCookie) {
    String decryptCookie = crypter.decryptAES(errorsCookie);
    return decryptCookie.substring(decryptCookie.indexOf(':') + 1);
  }
}
