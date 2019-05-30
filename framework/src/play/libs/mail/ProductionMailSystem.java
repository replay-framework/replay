package play.libs.mail;

import org.apache.commons.mail.Email;
import play.libs.Mail;

class ProductionMailSystem implements MailSystem {

    @Override
    public boolean sendMessage(Email email) {
        email.setMailSession(Mail.getSession());
        return Mail.sendMessage(email);
    }

}
