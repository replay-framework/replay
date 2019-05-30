package play.libs.mail;

import org.apache.commons.mail.Email;

public interface MailSystem {

    boolean sendMessage(Email email);

}
