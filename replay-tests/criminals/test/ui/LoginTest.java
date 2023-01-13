package ui;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.codeborne.selenide.Condition.appear;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.open;

public class LoginTest extends BaseUITest {
  private final MailServerEmulator mailServer = new MailServerEmulator();

  @Test
  public void loginWithoutUsernameAndPassword() {
    LoginPage page = open("/", LoginPage.class);
    page.login("", "");

    page.verifyUsernameWarning("validation.required");
    page.verifyPasswordWarning("validation.required");
  }

  @Test
  public void loginWithUsernameWhichDoesNotResembleAnEmailAddress() {
    LoginPage page = open("/", LoginPage.class);
    page.login("admin", "admin");
    page.verifyUsernameWarning("validation.email");
  }

  @Test
  public void loginWithOtpCode() {
    LoginPage loginPage = open("/", LoginPage.class);
    OtpCodePage otpPage = loginPage.login("admin@mail.ee", "admin@mail.ee");
    otpPage.otpCode.should(appear);

    String firstEmail = mailServer.getMessages().get(0);
    String otpCode = firstEmail.replaceFirst("Хочешь залогиниться\\? Введи этот код: (.*)", "$1");

    DashboardPage dashboardPage = otpPage.confirm(otpCode);
    dashboardPage.header.shouldHave(text("Hello, admin@mail.ee!"));
  }

  @Before
  public void setUp() {
    mailServer.start();
  }

  @After
  public void tearDown() {
    mailServer.shutdown();
  }
}
