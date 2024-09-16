package play.libs;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import play.Play;
import play.exceptions.UnexpectedException;

public class Crypter {
  private final String salt;

  public Crypter(String salt) {
    this.salt = salt;
  }

  public String encryptAES(String value) {
    return encryptAES(value, Play.secretKey.substring(0, 16));
  }

  private String encryptAES(String value, String privateKey) {
    try {
      byte[] raw = privateKey.getBytes(UTF_8);
      SecretKeySpec keySpec = new SecretKeySpec(raw, "AES");
      Cipher cipher = Cipher.getInstance("AES");
      cipher.init(Cipher.ENCRYPT_MODE, keySpec);
      return Codec.byteToHexString(cipher.doFinal((salt + value).getBytes(UTF_8)));
    } catch (NoSuchAlgorithmException
        | NoSuchPaddingException
        | InvalidKeyException
        | IllegalBlockSizeException
        | BadPaddingException ex) {
      throw new UnexpectedException(ex);
    }
  }

  public String decryptAES(String value) {
    return decryptAES(value, Play.secretKey.substring(0, 16));
  }

  private String decryptAES(String value, String privateKey) {
    try {
      byte[] raw = privateKey.getBytes(UTF_8);
      SecretKeySpec keySpec = new SecretKeySpec(raw, "AES");
      Cipher cipher = Cipher.getInstance("AES");
      cipher.init(Cipher.DECRYPT_MODE, keySpec);
      return new String(cipher.doFinal(Codec.hexStringToByte(value)), UTF_8)
          .substring(salt.length());
    } catch (NoSuchAlgorithmException
        | NoSuchPaddingException
        | InvalidKeyException
        | IllegalBlockSizeException
        | BadPaddingException ex) {
      throw new UnexpectedException(ex);
    }
  }
}
