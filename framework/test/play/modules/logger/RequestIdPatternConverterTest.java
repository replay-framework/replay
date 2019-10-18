package play.modules.logger;

import org.apache.log4j.Category;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.MDC;
import play.mvc.Http;

import static org.apache.log4j.Level.WARN;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

public class RequestIdPatternConverterTest {

  @Before
  public void setUp() {
    play.Logger.init();
  }

  @After
  public void tearDown() {
    MDC.clear();
    Http.Request.removeCurrent();
  }

  @Test
  public void returnRequestIdFromMDCIfRequestIsMissing() {
    Http.Request.removeCurrent();
    MDC.put("requestId", "job-exec-273");

    LoggingEvent event = new LoggingEvent("INFO", mock(Category.class), WARN, "message", null);

    assertThat(new RequestIdPatternConverter().convert(event), is("job-exec-273"));
  }

  @Test
  public void returnsThreadNameIfRequestAndMDCIsMissing() {
    Thread.currentThread().setName("Thread name");
    LoggingEvent event = new LoggingEvent("INFO", mock(Category.class), WARN, "message", null);

    assertThat(new RequestIdPatternConverter().convert(event), is("Thread name"));
  }

  @Test
  @SuppressWarnings("deprecation")
  public void returnsRequestIdIfRequestIsPresent() {
    Http.Request request = new Http.Request();
    request.args.put("requestId", "123");
    Http.Request.setCurrent(request);
    LoggingEvent event = new LoggingEvent("INFO", mock(Category.class), WARN, "message", null);

    assertThat(new RequestIdPatternConverter().convert(event), is("123"));
  }

  @Test
  @SuppressWarnings("deprecation")
  public void returnsThreadNameIfRequestIdInRequestIsMissing() {
    Thread.currentThread().setName("Thread name");
    Http.Request.setCurrent(new Http.Request());
    LoggingEvent event = new LoggingEvent("INFO", mock(Category.class), WARN, "message", null);

    assertThat(new RequestIdPatternConverter().convert(event), is("Thread name"));
  }
}