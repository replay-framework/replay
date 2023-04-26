package play.data.binding.types;

import org.junit.jupiter.api.Test;
import play.Play;
import play.mvc.Http.Request;
import play.mvc.Scope.Session;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static play.mvc.Http.Request.createRequest;

public class LocalDateBinderTest {
  static {
    Play.configuration.setProperty("date.format", "dd.MM.yyyy");
  }
  private final LocalDateBinder binder = new LocalDateBinder();
  private final Request request = createRequest(null, "GET", "/", "", null, null, null, null, false, 80, "localhost", false, null, null);
  private final Session session = new Session();

  @Test
  public void nullLocalDate() {
    assertThat(binder.bind(request, session, "event.start", null, null, LocalDate.class, null)).isNull();
  }

  @Test
  public void emptyLocalDate() {
    assertThat(binder.bind(request, session, "event.start", null, "", LocalDate.class, null)).isNull();
  }

  @Test
  public void blankLocalDate() {
    assertThat(binder.bind(request, session, "event.start", null, " ", LocalDate.class, null)).isNull();
  }

  @Test
  public void validLocalDate() {
    LocalDate expected = LocalDate.of(2014, 3, 8);
    LocalDate actual = binder.bind(request, session, "event.start", null, "2014-03-08", LocalDate.class, null);
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void validLocalDateInLocalFormat() {
    LocalDate expected = LocalDate.of(2018, 3, 8);
    LocalDate actual = binder.bind(request, session, "event.start", null, "08.03.2018", LocalDate.class, null);
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void validLocalDateWithMilliseconds() {
    LocalDate expected = LocalDate.of(2014, 3, 8);
    LocalDate actual = binder.bind(request, session, "event.start", null, "2014-03-08", LocalDate.class, null);
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void invalidLocalDate() {
    assertThatThrownBy(() -> binder.bind(request, session, "event.start", null, "2007-13-03", LocalDate.class, null))
      .isInstanceOf(DateTimeParseException.class)
      .hasMessage("Text '2007-13-03' could not be parsed: Invalid value for MonthOfYear (valid values 1 - 12): 13");
  }
}