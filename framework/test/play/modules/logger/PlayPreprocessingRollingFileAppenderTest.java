package play.modules.logger;

import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.ThrowableInformation;
import org.junit.Before;
import org.junit.Test;

import java.net.SocketTimeoutException;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

public class PlayPreprocessingRollingFileAppenderTest {

  PlayPreprocessingRollingFileAppender appender = new PlayPreprocessingRollingFileAppender();

  @Before
  public void setUp() {
    ExceptionsMonitoringPlugin.resetExceptions();
  }

  @Test
  public void extractsErrorSourceFromLoggerName() {
    LoggingEvent event = mock(LoggingEvent.class);
    doReturn("soap").when(event).getLoggerName();
    ThrowableInformation ti = mock(ThrowableInformation.class);
    doReturn(ti).when(event).getThrowableInformation();
    doReturn(new SocketTimeoutException("blah")).when(ti).getThrowable();

    appender.append(event);
    appender.append(event);
    appender.append(event);

    assertEquals(1, ExceptionsMonitoringPlugin.getExceptions().size());
    assertEquals(3, ExceptionsMonitoringPlugin.getExceptions().get("[soap] java.net.SocketTimeoutException: blah").intValue());
  }
}