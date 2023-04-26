package play.data.binding.types;

import org.junit.jupiter.api.Test;
import play.mvc.Http.Request;
import play.mvc.Scope.Session;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static play.mvc.Http.Request.createRequest;

public class LocalDateTimeBinderTest {

  private final LocalDateTimeBinder binder = new LocalDateTimeBinder();
  private final Request request = createRequest(null, "GET", "/", "", null, null, null, null, false, 80, "localhost", false, null, null);
  private final Session session = new Session();

  @Test
  public void nullLocalDateTime() {
    assertThat(binder.bind(request, session, "event.start", null, null, LocalDateTime.class, null)).isNull();
  }

  @Test
  public void emptyLocalDateTime() {
    assertThat(binder.bind(request, session, "event.start", null, "", LocalDateTime.class, null)).isNull();
  }

  @Test
  public void blankLocalDateTime() {
    assertThat(binder.bind(request, session, "event.start", null, " ", LocalDateTime.class, null)).isNull();
  }

  @Test
  public void validLocalDateTime() {
    LocalDateTime expected = LocalDateTime.of(2014, 3, 8, 12, 49, 21);
    LocalDateTime actual = binder.bind(request, session, "event.start", null, "2014-03-08T12:49:21", LocalDateTime.class, null);
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void validLocalDateTimeWithMilliseconds() {
    LocalDateTime expected = LocalDateTime.of(2014, 3, 8, 12, 49, 21, 130000000);
    LocalDateTime actual = binder.bind(request, session, "event.start", null, "2014-03-08T12:49:21.130", LocalDateTime.class, null);
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void invalidLocalDateTime() {
    assertThatThrownBy(() -> binder.bind(request, session, "event.start", null, "2007-13-03T10:15:30", LocalDateTime.class, null))
      .isInstanceOf(DateTimeParseException.class)
      .hasMessage("Text '2007-13-03T10:15:30' could not be parsed: Invalid value for MonthOfYear (valid values 1 - 12): 13");
  }
}