package ui;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.page;

import com.codeborne.selenide.SelenideElement;
import org.openqa.selenium.By;

public class OtpCodePage {
  final SelenideElement otpCode = $(By.name("otpCode"));

  public DashboardPage confirm(String otpCode) {
    this.otpCode.val(otpCode).pressEnter();
    return page(DashboardPage.class);
  }
}
