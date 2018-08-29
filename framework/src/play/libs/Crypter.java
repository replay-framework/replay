package play.libs;

import play.Play;
import play.exceptions.UnexpectedException;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import static java.nio.charset.StandardCharsets.UTF_8;

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
            SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
            return Codec.byteToHexString(cipher.doFinal((salt + value).getBytes(UTF_8)));
        } catch (Exception ex) {
            throw new UnexpectedException(ex);
        }
    }

    public String decryptAES(String value) {
        return decryptAES(value, Play.secretKey.substring(0, 16));
    }

    private String decryptAES(String value, String privateKey) {
        try {
            byte[] raw = privateKey.getBytes(UTF_8);
            SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, skeySpec);
            return new String(cipher.doFinal(Codec.hexStringToByte(value)), UTF_8).substring(salt.length());
        } catch (Exception ex) {
            throw new UnexpectedException(ex);
        }
    }
}
