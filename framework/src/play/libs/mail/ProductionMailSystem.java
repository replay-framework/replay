package play.libs.mail;

import org.apache.commons.mail.Email;
import play.libs.Mail;

import java.util.concurrent.Future;

class ProductionMailSystem implements MailSystem {

    @Override
    public Future<Boolean> sendMessage(Email email) {
        email.setMailSession(Mail.getSession());
        return Mail.sendMessage(email);
    }

}
