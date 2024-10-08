package play.libs.mail.test;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.mail.Email;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.libs.Mail;
import play.libs.mail.MailSystem;

/**
 * Just kept for compatibility reasons, use test double substitution mechanism instead.
 *
 * @see Mail#Mock
 * @see Mail#useMailSystem(MailSystem)
 */
public class LegacyMockMailSystem implements MailSystem {
  private static final Logger logger = LoggerFactory.getLogger(LegacyMockMailSystem.class);

  private final Map<String, String> emails = new HashMap<>();

  @Override
  public boolean sendMessage(Email email) {
    try {
      StringBuilder content = new StringBuilder();
      Properties props = new Properties();
      props.setProperty("mail.smtp.host", "myfakesmtpserver.com");

      Session session = Session.getInstance(props);
      email.setMailSession(session);

      email.buildMimeMessage();

      MimeMessage msg = email.getMimeMessage();
      msg.saveChanges();

      String body = getContent(msg);

      content.append("From Mock Mailer\n\tNew email received by");
      content.append("\n\tFrom: ").append(email.getFromAddress().getAddress());
      content.append("\n\tReplyTo: ").append(email.getReplyToAddresses().get(0).getAddress());

      addAddresses(content, "To", email.getToAddresses());
      addAddresses(content, "Cc", email.getCcAddresses());
      addAddresses(content, "Bcc", email.getBccAddresses());

      content.append("\n\tSubject: ").append(email.getSubject());
      content.append("\n\t").append(body);

      content.append("\n");
      logger.info(content.toString());

      for (InternetAddress add : email.getToAddresses()) {
        content.append(", ").append(add);
        emails.put(add.getAddress(), content.toString());
      }
      return true;

    } catch (Exception e) {
      logger.error("error sending mock email", e);
      return false;
    }
  }

  private static String getContent(Part message) throws MessagingException, IOException {

    if (message.getContent() instanceof String) {
      return message.getContentType() + ": " + message.getContent() + " \n\t";
    } else if (message.getContent() != null && message.getContent() instanceof Multipart part) {
      StringBuilder text = new StringBuilder();
      for (int i = 0; i < part.getCount(); i++) {
        BodyPart bodyPart = part.getBodyPart(i);
        if (!Part.ATTACHMENT.equals(bodyPart.getDisposition())) {
          text.append(getContent(bodyPart));
        } else {
          text.append("attachment: \n" + "\t\t name: ")
              .append(StringUtils.isEmpty(bodyPart.getFileName()) ? "none" : bodyPart.getFileName())
              .append("\n").append("\t\t disposition: ").append(bodyPart.getDisposition())
              .append("\n").append("\t\t description: ")
              .append(StringUtils.isEmpty(bodyPart.getDescription())
                  ? "none"
                  : bodyPart.getDescription()).append("\n\t");
        }
      }
      return text.toString();
    }
    if (message.getContent() != null && message.getContent() instanceof Part) {
      if (!Part.ATTACHMENT.equals(message.getDisposition())) {
        return getContent((Part) message.getContent());
      } else {
        return "attachment: \n"
            + "\t\t name: "
            + (StringUtils.isEmpty(message.getFileName()) ? "none" : message.getFileName())
            + "\n"
            + "\t\t disposition: "
            + message.getDisposition()
            + "\n"
            + "\t\t description: "
            + (StringUtils.isEmpty(message.getDescription()) ? "none" : message.getDescription())
            + "\n\t";
      }
    }

    return "";
  }

  private static void addAddresses(StringBuilder content, String header, List<?> ccAddresses) {
    if (ccAddresses != null && !ccAddresses.isEmpty()) {
      content.append("\n\t").append(header).append(": ");
      for (Object add : ccAddresses) {
        content.append(add).append(", ");
      }
      removeTheLastComma(content);
    }
  }

  private static void removeTheLastComma(StringBuilder content) {
    content.delete(content.length() - 2, content.length());
  }

  public String getLastMessageReceivedBy(String email) {
    return emails.get(email);
  }

  public void reset() {
    emails.clear();
  }
}
