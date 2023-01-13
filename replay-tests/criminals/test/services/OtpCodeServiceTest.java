package services;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class OtpCodeServiceTest {
  private final OtpCodeService service = new OtpCodeService();

  @Test
  public void otp_code_consists_of_4_digits() {
    assertThat(service.generateOtpCode()).matches("\\d{4}");
  }
}