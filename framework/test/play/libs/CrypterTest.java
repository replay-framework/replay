package play.libs;

import org.junit.Before;
import org.junit.Test;
import play.Play;

import static org.assertj.core.api.Assertions.assertThat;

public class CrypterTest {
  Crypter crypter = new Crypter("my-salt");

  @Before
  public void setUp() {
    Play.secretKey = "secret-secret-secret-secret";
  }

  @Test
  public void encrypt() {
    assertThat(crypter.encryptAES("blah")).isEqualTo("cb2d035cc33a930bf00c621bdb035a80");
    assertThat(crypter.encryptAES("пароль")).isEqualTo("2763d737e2c4f896eb28017daf4a06f9a792a7bd77e57f70dbaccb4d2f3b4fe7");
  }

  @Test
  public void decrypt() {
    assertThat(crypter.decryptAES("cb2d035cc33a930bf00c621bdb035a80")).isEqualTo("blah");
    assertThat(crypter.decryptAES("2763d737e2c4f896eb28017daf4a06f9a792a7bd77e57f70dbaccb4d2f3b4fe7")).isEqualTo("пароль");
  }
}