package play.modules.logger;

import org.apache.log4j.Category;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.Test;

import static org.apache.log4j.Priority.WARN;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

public class ExtendedPatternLayoutTest {
  @Test
  public void requestIdPatternConverterIsUsed() {
    Thread.currentThread().setName("thread name to be used");
    ExtendedPatternLayout pattern = new ExtendedPatternLayout("[%R] %m");
    LoggingEvent info = new LoggingEvent("INFO", mock(Category.class), WARN, "message", null);

    assertThat(pattern.format(info), is("[thread name to be used] message"));
  }
}