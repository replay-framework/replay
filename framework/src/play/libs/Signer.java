package play.libs;

import static java.nio.charset.StandardCharsets.UTF_8;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import play.Play;
import play.exceptions.UnexpectedException;

public class Signer {
  private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();

  @Nonnull private final String salt;

  public Signer(@Nonnull String salt) {
    this.salt = salt;
  }

  public @Nonnull String sign(@Nonnull String message) {
    return sign(message, Play.secretKey.getBytes(UTF_8));
  }

  private @Nonnull String sign(@Nonnull String message, byte[] key) {
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

  public boolean isValid(@Nullable String signature, @Nonnull String message) {
    return signature != null && signature.equals(sign(message));
  }
}
