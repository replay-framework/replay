package play.libs;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import play.exceptions.UnexpectedException;

import javax.annotation.Nonnull;
import java.security.MessageDigest;
import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;

public class Codec {
    @Nonnull
    public static String UUID() {
        return UUID.randomUUID().toString();
    }

    @Nonnull
    public static String encodeBASE64(@Nonnull String value) {
        return new String(Base64.encodeBase64(value.getBytes(UTF_8)), UTF_8);
    }

    @Nonnull
    public static String encodeBASE64(@Nonnull byte[] value) {
        return new String(Base64.encodeBase64(value), UTF_8);
    }

    @Nonnull
    public static byte[] decodeBASE64(@Nonnull String value) {
        return Base64.decodeBase64(value.getBytes(UTF_8));
    }

    @Nonnull
    public static String hexMD5(@Nonnull String value) {
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

    @Nonnull
    public static String hexSHA1(@Nonnull String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            md.update(value.getBytes(UTF_8));
            byte[] digest = md.digest();
            return byteToHexString(digest);
        } catch (Exception ex) {
            throw new UnexpectedException(ex);
        }
    }

    @Nonnull
    public static String byteToHexString(@Nonnull byte[] bytes) {
        return String.valueOf(Hex.encodeHex(bytes));
    }

    @Nonnull
    public static byte[] hexStringToByte(@Nonnull String hexString) {
        try {
            return Hex.decodeHex(hexString.toCharArray());
        } catch (DecoderException e) {
            throw new UnexpectedException(e);
        }
    }
}
