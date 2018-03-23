package play.libs;

import play.Play;

public class Signer {
  public String sign(String message, String salt) {
    return Crypto.sign(salt + message, Play.secretKey.getBytes());
  }

  public boolean isValid(String signature, String message, String salt) {
    return signature != null && signature.equals(sign(message, salt));
  }
}
