package play.data.binding.types;

import org.junit.Test;
import play.Play;
import play.mvc.Http.Request;
import play.mvc.Scope.Session;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static play.mvc.Http.Request.createRequest;

public class LocalDateBinderTest {
  static {
    Play.configuration.setProperty("date.format", "dd.MM.yyyy");
  }
  private LocalDateBinder binder = new LocalDateBinder();
  Request request = createRequest(null, "GET", "/", "", null, null, null, null, false, 80, "localhost", false, null, null);
  Session session = new Session();

  @Test
  public void nullLocalDate() {
    assertNull(binder.bind(request, session, "event.start", null, null, LocalDate.class, null));
  }

  @Test
  public void emptyLocalDate() {
    assertNull(binder.bind(request, session, "event.start", null, "", LocalDate.class, null));
  }

  @Test
  public void blankLocalDate() {
    assertNull(binder.bind(request, session, "event.start", null, " ", LocalDate.class, null));
  }

  @Test
  public void validLocalDate() {
    LocalDate expected = LocalDate.of(2014, 3, 8);
    LocalDate actual = binder.bind(request, session, "event.start", null, "2014-03-08", LocalDate.class, null);
    assertEquals(expected, actual);
  }

  @Test
  public void validLocalDateInLocalFormat() {
    LocalDate expected = LocalDate.of(2018, 3, 8);
    LocalDate actual = binder.bind(request, session, "event.start", null, "08.03.2018", LocalDate.class, null);
    assertEquals(expected, actual);
  }

  @Test
  public void validLocalDateWithMilliseconds() {
    LocalDate expected = LocalDate.of(2014, 3, 8);
    LocalDate actual = binder.bind(request, session, "event.start", null, "2014-03-08", LocalDate.class, null);
    assertEquals(expected, actual);
  }

  @Test(expected = DateTimeParseException.class)
  public void invalidLocalDate() {
    binder.bind(request, session, "event.start", null, "2007-13-03", LocalDate.class, null);
  }
}