package play.utils;

import org.apache.commons.codec.DecoderException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import play.Play;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ErrorsCookieCrypterTest {

  private final ErrorsCookieCrypter crypter = new ErrorsCookieCrypter();

  @BeforeEach
  public void setUp() {
    Play.secretKey = "secret-secret-secret-secret";
  }

  @Test
  public void encryptDecryptProcess() {
    assertThat(crypter.decrypt(crypter.encrypt("test"))).isEqualTo("test");
  }

  @Test
  public void decryptFail() {
    assertThatThrownBy(() -> crypter.decrypt("e08d471c01b42cf0c5b67e13c65ec67"))
      .rootCause()
      .isInstanceOf(DecoderException.class)
      .hasMessage("Odd number of characters.");
  }
}