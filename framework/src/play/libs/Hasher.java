package play.libs;

import org.apache.commons.codec.binary.Base64;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static java.nio.charset.StandardCharsets.UTF_8;

public class Hasher {
  private final String salt;

  public Hasher(String salt) {
    this.salt = salt;
  }

  public String md5Hash(String input) {
    return hash(input, HashType.MD5);
  }

  public String hash(String input, HashType hashType) {
    try {
      MessageDigest m = MessageDigest.getInstance(hashType.toString());
      byte[] out = m.digest((salt+ input).getBytes(UTF_8));
      return new String(Base64.encodeBase64(out), UTF_8);
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }


  private enum HashType {
    MD5("MD5"), SHA1("SHA-1"), SHA256("SHA-256"), SHA512("SHA-512");
    private final String algorithm;

    HashType(String algorithm) {
      this.algorithm = algorithm;
    }

    @Override
    public String toString() {
      return this.algorithm;
    }
  }
}
