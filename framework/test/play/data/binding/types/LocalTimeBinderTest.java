package play.data.binding.types;

import org.junit.Test;
import play.mvc.Http.Request;
import play.mvc.Scope.Session;

import java.time.LocalTime;
import java.time.format.DateTimeParseException;

import static org.assertj.core.api.Assertions.assertThat;
import static play.mvc.Http.Request.createRequest;

public class LocalTimeBinderTest {

  private final LocalTimeBinder binder = new LocalTimeBinder();
  private final Request request = createRequest(null, "GET", "/", "", null, null, null, null, false, 80, "localhost", false, null, null);
  private final Session session = new Session();

  @Test
  public void nullLocalTime() {
    assertThat(binder.bind(request, session, "event.start", null, null, LocalTime.class, null)).isNull();
  }

  @Test
  public void emptyLocalTime() {
    assertThat(binder.bind(request, session, "event.start", null, "", LocalTime.class, null)).isNull();
  }

  @Test
  public void blankLocalTime() {
    assertThat(binder.bind(request, session, "event.start", null, " ", LocalTime.class, null)).isNull();
  }

  @Test
  public void validLocalTime() {
    LocalTime expected = LocalTime.of(23, 49, 21);
    LocalTime actual = binder.bind(request, session, "event.start", null, "23:49:21", LocalTime.class, null);
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void validLocalTimeWithMilliseconds() {
    LocalTime expected = LocalTime.of(23, 59, 21, 130000000);
    LocalTime actual = binder.bind(request, session, "event.start", null, "23:59:21.130", LocalTime.class, null);
    assertThat(actual).isEqualTo(expected);
  }

  @Test(expected = DateTimeParseException.class)
  public void invalidLocalTime() {
    binder.bind(request, session, "event.start", null, "61:15:30", LocalTime.class, null);
  }
}