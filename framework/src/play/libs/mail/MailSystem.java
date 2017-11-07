package play.libs.mail;

import org.apache.commons.mail.Email;

import java.util.concurrent.Future;

public interface MailSystem {

    Future<Boolean> sendMessage(Email email);

}
