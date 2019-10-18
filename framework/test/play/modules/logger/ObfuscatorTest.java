package play.modules.logger;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ObfuscatorTest {
  private Obfuscator obfuscator = new Obfuscator();

  @Test
  public void nullSafe() {
    assertThat(obfuscator.isLikeCardNumber(null)).isEqualTo(false);
    assertThat(obfuscator.maskCardNumber(null)).isNull();
  }

  @Test
  public void emptyString() {
    assertThat(obfuscator.isLikeCardNumber("")).isFalse();
    assertThat(obfuscator.maskCardNumber("")).isEqualTo("");
  }

  @Test
  public void masksCardNumbers() {
    assertThat(obfuscator.isLikeCardNumber("4797707124015750")).isTrue();
    assertThat(obfuscator.maskCardNumber("4797707124015750")).isEqualTo("479770xxxxxx5750");

    assertThat(obfuscator.maskCardNumber("foo 4797707124015750 bar")).isEqualTo("foo 479770xxxxxx5750 bar");
    assertThat(obfuscator.maskCardNumber("foo4797707124015750bar")).isEqualTo("foo479770xxxxxx5750bar");
    assertThat(obfuscator.maskCardNumber("foo4797707124015750")).isEqualTo("foo479770xxxxxx5750");
    assertThat(obfuscator.maskCardNumber("4797707124015750bar")).isEqualTo("479770xxxxxx5750bar");
  }

  @Test
  public void doesNotMaskAccountNumbers() {
    assertThat(obfuscator.isLikeCardNumber("40702810090240700028")).isFalse();
    assertThat(obfuscator.maskCardNumber("40702810090240700028")).isEqualTo("40702810090240700028");
  }

  @Test
  public void doesNotMaskPhoneNumbers() {
    assertThat(obfuscator.isLikeCardNumber("7916000000")).isFalse();
    assertThat(obfuscator.maskCardNumber("7916000000")).isEqualTo("7916000000");
  }

  @Test
  public void masksMultipleCardNumbersInText() {
    assertThat(obfuscator.maskCardNumber("4797707124015750 tere foo4797707124015750bar"))
    .isEqualTo("479770xxxxxx5750 tere foo479770xxxxxx5750bar");
  }

  @Test
  public void masksFormattedCardNumber() {
    String log = "Получатель: 5536913733566699";

    assertThat(obfuscator.maskCardNumber(log)).doesNotContain("66699");
    assertThat(obfuscator.maskCardNumber(log)).doesNotContain("6 6699");
    assertThat(obfuscator.maskCardNumber(log)).contains("xxxxxx6699");
  }
}