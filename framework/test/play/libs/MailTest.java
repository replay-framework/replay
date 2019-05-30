package play.libs;

import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;
import org.junit.Before;
import org.junit.Test;
import play.PlayBuilder;
import play.exceptions.MailException;
import play.libs.mail.MailSystem;

import static org.junit.Assert.assertEquals;

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

    @Before
    public void initializeFixture() throws Exception {
        new PlayBuilder().build();

        simpleEmail =
                new SimpleEmail()
                        .setFrom("from@playframework.com")
                        .addTo("to@playframework.com")
                        .setSubject("subject");

        spyingMailSystem = new SpyingMailSystem();
    }

    @Test(expected = MailException.class)
    public void buildMessageWithoutFrom() throws EmailException {
        Email emailWithoutFrom = new SimpleEmail();
        emailWithoutFrom.addTo("from@playframework.com");
        emailWithoutFrom.setSubject("subject");
        Mail.buildMessage(new SimpleEmail());
    }

    @Test(expected = MailException.class)
    public void buildMessageWithoutRecipient() throws EmailException {
        Email emailWithoutRecipients =
                new SimpleEmail()
                        .setFrom("from@playframework.com")
                        .setSubject("subject");
        Mail.buildMessage(emailWithoutRecipients);
    }

    @Test(expected = MailException.class)
    public void buildMessageWithoutSubject() throws EmailException {
        Email emailWithoutSubject = new SimpleEmail();
        emailWithoutSubject.setFrom("from@playframework.com");
        emailWithoutSubject.addTo("to@playframework.com");
        Mail.buildMessage(emailWithoutSubject);
    }

    @Test
    public void buildValidMessages() throws EmailException {
        Mail.buildMessage(
                emailWithoutRecipients().addTo("to@playframework.com"));
        Mail.buildMessage(
                emailWithoutRecipients().addCc("cc@playframework.com"));
        Mail.buildMessage(
                emailWithoutRecipients().addBcc("bcc@playframework.com"));
    }

    private Email emailWithoutRecipients() throws EmailException {
        return
                new SimpleEmail()
                        .setFrom("from@playframework.com")
                        .setSubject("subject");
    }

    @Test
    public void mailSystemShouldBeSubstitutable() {
        Mail.useMailSystem(spyingMailSystem);

        Mail.send(simpleEmail);

        assertEquals(simpleEmail, spyingMailSystem.receivedEmail);
    }

}
