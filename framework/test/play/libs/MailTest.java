package play.libs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import play.PlayBuilder;
import play.exceptions.MailException;
import play.libs.mail.MailSystem;

public class MailTest {

  private static class SpyingMailSystem implements MailSystem {
    public Email receivedEmail;

    @Override
    public boolean sendMessage(Email email) {
      receivedEmail = email;
      return true;
    }
  }

  private Email simpleEmail;
  private SpyingMailSystem spyingMailSystem;

  @BeforeEach
  public void initializeFixture() throws Exception {
    new PlayBuilder().build();

    simpleEmail =
        new SimpleEmail()
            .setFrom("from@playframework.com")
            .addTo("to@playframework.com")
            .setSubject("subject");

    spyingMailSystem = new SpyingMailSystem();
  }

  @Test
  public void buildMessageWithoutFrom() throws EmailException {
    Email emailWithoutFrom = new SimpleEmail();
    emailWithoutFrom.addTo("from@playframework.com");
    emailWithoutFrom.setSubject("subject");
    assertThatThrownBy(() -> Mail.buildMessage(new SimpleEmail()))
        .isInstanceOf(MailException.class)
        .hasMessage("Please define a 'from' email address");
  }

  @Test
  public void buildMessageWithoutRecipient() throws EmailException {
    Email emailWithoutRecipients =
        new SimpleEmail().setFrom("from@playframework.com").setSubject("subject");
    assertThatThrownBy(() -> Mail.buildMessage(emailWithoutRecipients))
        .isInstanceOf(MailException.class)
        .hasMessage("Please define a recipient email address");
  }

  @Test
  public void buildMessageWithoutSubject() throws EmailException {
    Email emailWithoutSubject = new SimpleEmail();
    emailWithoutSubject.setFrom("from@playframework.com");
    emailWithoutSubject.addTo("to@playframework.com");
    assertThatThrownBy(() -> Mail.buildMessage(emailWithoutSubject))
        .isInstanceOf(MailException.class)
        .hasMessage("Please define a subject");
  }

  @Test
  public void buildValidMessages() throws EmailException {
    Mail.buildMessage(emailWithoutRecipients().addTo("to@playframework.com"));
    Mail.buildMessage(emailWithoutRecipients().addCc("cc@playframework.com"));
    Mail.buildMessage(emailWithoutRecipients().addBcc("bcc@playframework.com"));
  }

  private Email emailWithoutRecipients() throws EmailException {
    return new SimpleEmail().setFrom("from@playframework.com").setSubject("subject");
  }

  @Test
  public void mailSystemShouldBeSubstitutable() {
    Mail.useMailSystem(spyingMailSystem);

    Mail.send(simpleEmail);

    assertThat(spyingMailSystem.receivedEmail).isEqualTo(simpleEmail);
  }
}
