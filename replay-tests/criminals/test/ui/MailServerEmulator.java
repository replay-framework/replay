package ui;

import jakarta.inject.Singleton;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import org.subethamail.smtp.helper.SimpleMessageListener;
import org.subethamail.smtp.helper.SimpleMessageListenerAdapter;
import org.subethamail.smtp.server.SMTPServer;
import play.Play;

@Singleton
public class MailServerEmulator {
  private SMTPServer smtpServer;
  private final List<String> messages = new ArrayList<>();

  public void start() {
    smtpServer =
        new SMTPServer(
            new SimpleMessageListenerAdapter(
                new SimpleMessageListener() {
                  @Override
                  public boolean accept(String from, String recipient) {
                    return true;
                  }

                  @Override
                  public void deliver(String from, String recipient, InputStream data)
                      throws IOException {
                    try {
                      MimeMessage mimeMessage =
                          new MimeMessage(Session.getDefaultInstance(new Properties()), data);
                      messages.add((String) mimeMessage.getContent());
                    } catch (MessagingException e) {
                      throw new RuntimeException(
                          "Failed to read message from " + from + " to " + recipient, e);
                    }
                  }
                }));

    smtpServer.setPort(Integer.parseInt(Play.configuration.getProperty("mail.smtp.port", "0")));
    smtpServer.start();
    Play.configuration.setProperty("mail.smtp.port", String.valueOf(smtpServer.getPort()));
  }

  public void shutdown() {
    if (smtpServer != null) {
      smtpServer.stop();
      smtpServer = null;
    }
  }

  public List<String> getMessages() {
    return messages;
  }
}
