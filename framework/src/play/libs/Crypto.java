package play.libs;

/**
 * Cryptography utils
 */
public class Crypto {
    private static final Crypter crypter = new Crypter("");

    /**
     * Encrypt a String with the AES encryption standard using the application secret
     *
     * @param value
     *            The String to encrypt
     * @return An hexadecimal encrypted string
     */
    @Deprecated
    public static String encryptAES(String value) {
        return crypter.encryptAES(value);
    }

    /**
     * Decrypt a String with the AES encryption standard using the application secret
     *
     * @param value
     *            An hexadecimal encrypted string
     * @return The decrypted String
     */
    @Deprecated
    public static String decryptAES(String value) {
        return crypter.decryptAES(value);
    }
}
