package play.utils;

import com.google.errorprone.annotations.CheckReturnValue;
import org.jspecify.annotations.NullMarked;
import play.libs.Crypter;

@NullMarked
@CheckReturnValue
public class ErrorsCookieCrypter {
  private final Crypter crypter = new Crypter("errors-");

  public String encrypt(String errorsCookie) {
    return crypter.encryptAES(Math.random() + ":" + errorsCookie);
  }

  public String decrypt(String errorsCookie) {
    String decryptCookie = crypter.decryptAES(errorsCookie);
    return decryptCookie.substring(decryptCookie.indexOf(':') + 1);
  }
}
