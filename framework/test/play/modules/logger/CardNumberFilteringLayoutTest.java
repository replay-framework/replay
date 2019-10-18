package play.modules.logger;

import org.apache.log4j.Category;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import static org.apache.log4j.Level.WARN;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class CardNumberFilteringLayoutTest {
  CardNumberFilteringLayout cardNumberFilteringLayout = Mockito.spy(new CardNumberFilteringLayout());

  @Test
  public void cardNumbersAreMasked() {
    LoggingEvent event = new LoggingEvent("", Logger.getRootLogger(), Level.ALL,
      "Hello card 1234123412341234 and longer card 1234123412341234123, but not account DE123412341234123412341234", null);

    assertThat(cardNumberFilteringLayout.format(event),
      is("Hello card 123412xxxxxx1234 and longer card 123412xxxxxx4123, but not account DE123412341234123412341234\n"));
  }

  @Test
  public void doNotCreateNewEventIfNoCardNumbersWereFound() {
    LoggingEvent event = new LoggingEvent("", Logger.getRootLogger(), Level.ALL, "foo", null);
    ArgumentCaptor<LoggingEvent> eventCaptor = ArgumentCaptor.forClass(LoggingEvent.class);

    assertThat(cardNumberFilteringLayout.format(event), is("foo\n"));

    verify(cardNumberFilteringLayout).superFormat(eventCaptor.capture());
    assertThat(eventCaptor.getValue(), is(sameInstance(event)));
  }

  @Test
  public void cardNumbersInThreadNameAreMasked() {
    Thread.currentThread().setName("thread name with PAN 1234123412341234");
    CardNumberFilteringLayout layout = new CardNumberFilteringLayout();
    layout.setConversionPattern("[%R] %m");
    LoggingEvent info = new LoggingEvent("INFO", mock(Category.class), WARN, "message", null);

    assertThat(layout.format(info), is("[thread name with PAN 123412xxxxxx1234] message"));
    assertThat(layout.format(info), is("[thread name with PAN 123412xxxxxx1234] message"));
  }
}