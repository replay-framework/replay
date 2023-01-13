package controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.data.validation.Email;
import play.data.validation.Required;
import play.data.validation.Validation;
import play.mvc.Controller;
import play.mvc.results.Redirect;
import play.mvc.results.Result;
import play.rebel.View;
import services.OtpCodeService;

import javax.inject.Inject;

public class Login extends Controller {
  private static final Logger log = LoggerFactory.getLogger(Login.class);

  @Inject
  OtpCodeService otpCodeService;

  public Result loginForm() {
    return new View("login/form.html");
  }

  public Result firstStep(@Required @Email String username, @Required String password) throws Exception {
    if (Validation.hasErrors()) {
      return new View("login/form.html")
          .with("username", username)
          .with("password", password);
    }

    if (!username.equals(password)) {
      return new View("login/form.html")
          .with("username", username)
          .with("wrongCredentials", true);
    }
    log.info("Logging in as {} ...", username);

    String otpLabel = otpCodeService.generateOtpLabel();
    String otpCode = otpCodeService.generateOtpCode();
    otpCodeService.sendOtpCode(username, otpCode);

    session.put("login.username", username);
    session.put("login.otpLabel", otpLabel);
    session.put("login.otpCode", otpCode);

    log.info("Generated OTP code {} for {}, label: {}", otpCode, username, otpLabel);
    return new View("login/otpCodeForm.html").with("otpLabel", otpLabel);
  }

  public Result secondStep(String otpCode) {
    if (!otpCode.equals(session.get("login.otpCode"))) {
      log.info("Wrong otp code");
      return new View("login/otpCodeForm.html").with("otpLabel", session.get("login.otpLabel"));
    }

    session.put("username", session.get("login.username"));
    session.remove("login.username");

    log.info("Logged in as {}", session.get("username"));
    return new Redirect("/dashboard");
  }
}
