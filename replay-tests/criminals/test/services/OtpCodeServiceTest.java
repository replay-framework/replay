package services;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class OtpCodeServiceTest {
  private final OtpCodeService service = new OtpCodeService();

  @Test
  public void otp_code_consists_of_4_digits() {
    assertThat(service.generateOtpCode()).matches("\\d{4}");
  }
}
