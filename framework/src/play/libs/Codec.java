package play.libs;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.github.f4b6a3.ulid.UlidCreator;
import java.security.MessageDigest;
import java.util.UUID;
import com.google.errorprone.annotations.CheckReturnValue;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.jspecify.annotations.NullMarked;
import play.exceptions.UnexpectedException;

@NullMarked
@CheckReturnValue
public class Codec {
  public static String ulid() {
    return UlidCreator.getUlid().toLowerCase();
  }

  public static String UUID() {
    return UUID.randomUUID().toString();
  }

  public static String encodeBASE64(String value) {
    return new String(Base64.encodeBase64(value.getBytes(UTF_8)), UTF_8);
  }

  public static String encodeBASE64(byte[] value) {
    return new String(Base64.encodeBase64(value), UTF_8);
  }

  public static byte[] decodeBASE64(String value) {
    return Base64.decodeBase64(value.getBytes(UTF_8));
  }

  public static String hexMD5(String value) {
    try {
      MessageDigest messageDigest = MessageDigest.getInstance("MD5");
      messageDigest.reset();
      messageDigest.update(value.getBytes(UTF_8));
      byte[] digest = messageDigest.digest();
      return byteToHexString(digest);
    } catch (Exception ex) {
      throw new UnexpectedException(ex);
    }
  }

  public static String hexSHA1(String value) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-1");
      md.update(value.getBytes(UTF_8));
      byte[] digest = md.digest();
      return byteToHexString(digest);
    } catch (Exception ex) {
      throw new UnexpectedException(ex);
    }
  }

  public static String byteToHexString(byte[] bytes) {
    return String.valueOf(Hex.encodeHex(bytes));
  }

  public static byte[] hexStringToByte(String hexString) {
    try {
      return Hex.decodeHex(hexString.toCharArray());
    } catch (DecoderException e) {
      throw new UnexpectedException(e);
    }
  }
}
