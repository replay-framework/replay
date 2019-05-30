package play.libs;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.Play;
import play.exceptions.MailException;
import play.libs.mail.AbstractMailSystemFactory;
import play.libs.mail.MailSystem;
import play.libs.mail.test.LegacyMockMailSystem;
import play.utils.Utils.Maps;
import play.utils.YesSSLSocketFactory;

import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import java.util.Date;
import java.util.Map;
import java.util.Properties;

public class Mail {
    private static final Logger logger = LoggerFactory.getLogger(Mail.class);

    private static class StaticMailSystemFactory extends AbstractMailSystemFactory {

        private final MailSystem mailSystem;

        private StaticMailSystemFactory(MailSystem mailSystem) {
            this.mailSystem = mailSystem;
        }

        @Override
        public MailSystem currentMailSystem() {
            return mailSystem;
        }

    }

    private static Session session;
    protected static AbstractMailSystemFactory mailSystemFactory = AbstractMailSystemFactory.DEFAULT;

    /**
     * Send an email
     * 
     * @param email
     *            An Email message
     * @return true if email successfully send
     */
    public static boolean send(Email email) {
        try {
            return currentMailSystem().sendMessage(buildMessage(email));
        } catch (EmailException ex) {
            throw new MailException("Cannot send email", ex);
        }
    }

    // Helper method for better readability
    protected static MailSystem currentMailSystem() {
        return mailSystemFactory.currentMailSystem();
    }

    /**
     * Through this method you can substitute the current MailSystem. This is especially helpful for testing purposes
     * like using mock libraries.
     *
     * @author Andreas Simon &lt;a.simon@quagilis.de&gt;
     * @param mailSystem
     *            The mailSystem to use
     * @see MailSystem
     */
    public static void useMailSystem(MailSystem mailSystem) {
        mailSystemFactory = new StaticMailSystemFactory(mailSystem);
    }

    public static Email buildMessage(Email email) throws EmailException {
        String from = Play.configuration.getProperty("mail.smtp.from");
        if (email.getFromAddress() == null && !StringUtils.isEmpty(from)) {
            email.setFrom(from);
        } else if (email.getFromAddress() == null) {
            throw new MailException("Please define a 'from' email address");
        }
        if ((email.getToAddresses() == null || email.getToAddresses().isEmpty())
                && (email.getCcAddresses() == null || email.getCcAddresses().isEmpty())
                && (email.getBccAddresses() == null || email.getBccAddresses().isEmpty())) {
            throw new MailException("Please define a recipient email address");
        }
        if (email.getSubject() == null) {
            throw new MailException("Please define a subject");
        }
        if (email.getReplyToAddresses() == null || email.getReplyToAddresses().isEmpty()) {
            email.addReplyTo(email.getFromAddress().getAddress());
        }

        return email;
    }

    public static Session getSession() {
        if (session == null) {
            Properties props = new Properties();
            // Put a bogus value even if we are on dev mode, otherwise JavaMail will complain
            props.setProperty("mail.smtp.host", Play.configuration.getProperty("mail.smtp.host", "localhost"));

            String channelEncryption;
            if (Play.configuration.containsKey("mail.smtp.protocol")
                    && "smtps".equals(Play.configuration.getProperty("mail.smtp.protocol", "smtp"))) {
                // Backward compatibility before stable5
                channelEncryption = "starttls";
            } else {
                channelEncryption = Play.configuration.getProperty("mail.smtp.channel", "clear");
            }

            if ("clear".equals(channelEncryption)) {
                props.setProperty("mail.smtp.port", "25");
            } else if ("ssl".equals(channelEncryption)) {
                // port 465 + setup yes ssl socket factory (won't verify that the server certificate is signed with a
                // root ca.)
                props.setProperty("mail.smtp.port", "465");
                props.setProperty("mail.smtp.socketFactory.port", "465");
                props.setProperty("mail.smtp.socketFactory.class", YesSSLSocketFactory.class.getName());
                props.setProperty("mail.smtp.socketFactory.fallback", "false");
            } else if ("starttls".equals(channelEncryption)) {
                // port 25 + enable starttls + ssl socket factory
                props.setProperty("mail.smtp.port", "25");
                props.setProperty("mail.smtp.starttls.enable", "true");
                // can't install our socket factory. will work only with server that has a signed certificate
                // story to be continued in javamail 1.4.2 : https://glassfish.dev.java.net/issues/show_bug.cgi?id=5189
            }

            // Inject additional mail.* settings declared in Play! configuration
            Map<Object, Object> additionalSettings = Maps.filterMap(Play.configuration, "^mail\\..*");
            if (!additionalSettings.isEmpty()) {
                // Remove "password" fields
                additionalSettings.remove("mail.smtp.pass");
                additionalSettings.remove("mail.smtp.password");
                props.putAll(additionalSettings);
            }

            String user = Play.configuration.getProperty("mail.smtp.user");
            String password = Play.configuration.getProperty("mail.smtp.pass");
            if (password == null) {
                // Fallback to old convention
                password = Play.configuration.getProperty("mail.smtp.password");
            }
            String authenticator = Play.configuration.getProperty("mail.smtp.authenticator");
            session = null;

            if (authenticator != null) {
                props.setProperty("mail.smtp.auth", "true");
                try {
                    session = Session.getInstance(props, (Authenticator) Class.forName(authenticator).newInstance());
                } catch (Exception e) {
                    logger.error("Cannot instantiate custom SMTP authenticator ({})", authenticator, e);
                }
            }

            if (session == null) {
                if (user != null && password != null) {
                    props.setProperty("mail.smtp.auth", "true");
                    session = Session.getInstance(props, new SMTPAuthenticator(user, password));
                } else {
                    props.remove("mail.smtp.auth");
                    session = Session.getInstance(props);
                }
            }

            if (Boolean.parseBoolean(Play.configuration.getProperty("mail.debug", "false"))) {
                session.setDebug(true);
            }
        }
        return session;
    }

    /**
     * Send a JavaMail message
     *
     * @param msg
     *            An Email message
     * @return true if email successfully send
     */
    public static boolean sendMessage(final Email msg) {
        try {
            msg.setSentDate(new Date());
            msg.send();
            return true;
        } catch (Throwable e) {
            MailException me = new MailException("Error while sending email", e);
            logger.error("The email has not been sent", me);
            return false;
        }
    }

    public static class SMTPAuthenticator extends Authenticator {

        private String user;
        private String password;

        public SMTPAuthenticator(String user, String password) {
            this.user = user;
            this.password = password;
        }

        @Override
        protected PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication(user, password);
        }
    }

    /**
     * Just kept for compatibility reasons, use test double substitution mechanism instead.
     *
     * @see Mail#useMailSystem(MailSystem)
     */
    public static LegacyMockMailSystem Mock = new LegacyMockMailSystem();
}
