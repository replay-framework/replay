package play.modules.logger;

import org.apache.log4j.DailyRollingFileAppender;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.ThrowableInformation;

/**
 * Special file appender for general log, which registers exceptions with monitor
 */
public class PlayPreprocessingRollingFileAppender extends DailyRollingFileAppender {
  @Override public void append(LoggingEvent event) {
    ThrowableInformation ti = event.getThrowableInformation();
    if (ti != null) {
      Throwable throwable = ti.getThrowable();
      ExceptionsMonitoringPlugin.register(event.getLoggerName(), throwable);
    }
    super.append(event);
  }
}
