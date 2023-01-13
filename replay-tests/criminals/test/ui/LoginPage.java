package ui;

import com.codeborne.selenide.SelenideElement;
import org.openqa.selenium.By;
import play.i18n.Messages;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.page;

public class LoginPage {
  private final SelenideElement username = $(By.name("username"));
  private final SelenideElement password = $(By.name("password"));

  public OtpCodePage login(String username, String password) {
    this.username.setValue(username);
    this.password.setValue(password).pressEnter();
    return page(OtpCodePage.class);
  }

  public void verifyUsernameWarning(String messageKey) {
    fieldError(username).shouldHave(text(Messages.get(messageKey, "username")));
  }

  public void verifyPasswordWarning(String messageKey) {
    fieldError(password).shouldHave(text(Messages.get(messageKey, "password")));
  }

  private SelenideElement fieldError(SelenideElement username) {
    return username.closest(".form-group").find(".text-danger");
  }
}
