package play.modules.logger;

import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.ThrowableInformation;

public class CardNumberFilteringLayout extends ExtendedPatternLayout {
  Obfuscator obfuscator = new Obfuscator();

  @Override
  public String format(LoggingEvent event) {
    if (event.getMessage() instanceof String) {
      String message = event.getRenderedMessage();
      String maskedMessage = obfuscator.maskCardNumber(message);

      if (!message.equals(maskedMessage)) {
        ThrowableInformation throwableInformation = event.getThrowableInformation();
        Throwable throwable = throwableInformation != null ? throwableInformation.getThrowable() : null;
        LoggingEvent maskedEvent = new LoggingEvent(event.fqnOfCategoryClass,
          Logger.getLogger(event.getLoggerName()), event.timeStamp,
          event.getLevel(), maskedMessage, throwable);

        return superFormat(maskedEvent);
      }
    }
    return superFormat(event);
  }

  String superFormat(LoggingEvent maskedEvent) {
    return super.format(maskedEvent);
  }

  @Override protected RequestIdPatternConverter createRequestIdPatternConverter() {
    return new CardNumberAwareRequestIdPatternConverter();
  }

  private static class CardNumberAwareRequestIdPatternConverter extends RequestIdPatternConverter {
    private final Obfuscator obfuscator = new Obfuscator();

    @Override protected String currentThreadName() {
      return obfuscator.maskCardNumber(super.currentThreadName());
    }
  }
}