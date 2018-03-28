package play.libs;

import play.Play;

import static java.nio.charset.StandardCharsets.UTF_8;

public class Signer {
  public String sign(String message, String salt) {
    return Crypto.sign(salt + message, Play.secretKey.getBytes(UTF_8));
  }

  public boolean isValid(String signature, String message, String salt) {
    return signature != null && signature.equals(sign(message, salt));
  }
}
