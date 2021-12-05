package play.modules.logger;

import org.apache.log4j.Category;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.MDC;
import play.mvc.Http;

import static java.lang.Thread.currentThread;
import static org.apache.log4j.Level.WARN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class RequestIdPatternConverterTest {
  private final String currentThreadName = currentThread().getName();

  @After
  public final void restoreThreadName() {
    currentThread().setName(currentThreadName);
  }

  @Before
  @After
  public final void cleanup() {
    MDC.clear();
    Http.Request.removeCurrent();
  }

  @Test
  public void returnRequestIdFromMDCIfRequestIsMissing() {
    MDC.put("requestId", "job-exec-273");

    LoggingEvent event = new LoggingEvent("INFO", mock(Category.class), WARN, "message", null);

    assertThat(new RequestIdPatternConverter().convert(event)).isEqualTo("job-exec-273");
  }

  @Test
  public void returnsThreadNameIfRequestAndMDCIsMissing() {
    currentThread().setName("Thread name");
    LoggingEvent event = new LoggingEvent("INFO", mock(Category.class), WARN, "message", null);

    assertThat(new RequestIdPatternConverter().convert(event)).isEqualTo("Thread name");
  }

  @Test
  @SuppressWarnings("deprecation")
  public void returnsRequestIdIfRequestIsPresent() {
    Http.Request request = new Http.Request();
    request.args.put("requestId", "123");
    Http.Request.setCurrent(request);
    LoggingEvent event = new LoggingEvent("INFO", mock(Category.class), WARN, "message", null);

    assertThat(new RequestIdPatternConverter().convert(event)).isEqualTo("123");
  }

  @Test
  @SuppressWarnings("deprecation")
  public void returnsThreadNameIfRequestIdInRequestIsMissing() {
    currentThread().setName("Thread name");
    Http.Request.setCurrent(new Http.Request());
    LoggingEvent event = new LoggingEvent("INFO", mock(Category.class), WARN, "message", null);

    assertThat(new RequestIdPatternConverter().convert(event)).isEqualTo("Thread name");
  }
}