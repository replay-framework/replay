package play.libs;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.errorprone.annotations.CheckReturnValue;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import play.Play;
import play.exceptions.UnexpectedException;

@NullMarked
@CheckReturnValue
public class Signer {
  private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();

  private final String salt;

  public Signer(String salt) {
    this.salt = salt;
  }

  public String sign(String message) {
    return sign(message, Play.secretKey.getBytes(UTF_8));
  }

  private String sign(String message, byte[] key) {
    if (key.length == 0) {
      throw new IllegalStateException("application.secret is not configured");
    }

    try {
      Mac mac = Mac.getInstance("HmacSHA1");
      SecretKeySpec signingKey = new SecretKeySpec(key, "HmacSHA1");
      mac.init(signingKey);
      byte[] messageBytes = (salt + message).getBytes(UTF_8);
      byte[] result = mac.doFinal(messageBytes);
      int len = result.length;
      char[] hexChars = new char[len * 2];

      for (int charIndex = 0, startIndex = 0; charIndex < hexChars.length; ) {
        int bite = result[startIndex++] & 0xff;
        hexChars[charIndex++] = HEX_CHARS[bite >> 4];
        hexChars[charIndex++] = HEX_CHARS[bite & 0xf];
      }
      return new String(hexChars);
    } catch (Exception ex) {
      throw new UnexpectedException(ex);
    }
  }

  public boolean isValid(@Nullable String signature, String message) {
    return signature != null && signature.equals(sign(message));
  }
}
