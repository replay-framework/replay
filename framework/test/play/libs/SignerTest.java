package play.libs;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import play.Play;

public class SignerTest {
  private final Signer signer = new Signer("my-salt");

  @BeforeEach
  public void setUp() {
    Play.secretKey = "secret-secret-secret-secret";
  }

  @Test
  public void sign() {
    assertThat(signer.sign("blah")).isEqualTo("45a433ae8c152c697d03b3bb43e44a030a91b19f");
    assertThat(signer.sign("пароль")).isEqualTo("537bd701688169490c14316044e2819051722300");
  }

  @Test
  public void isValid() {
    assertThat(signer.isValid("45a433ae8c152c697d03b3bb43e44a030a91b19f", "blah")).isTrue();
    assertThat(signer.isValid("537bd701688169490c14316044e2819051722300", "пароль")).isTrue();
    assertThat(signer.isValid("incorrectSignature", "пароль")).isFalse();
  }
}
