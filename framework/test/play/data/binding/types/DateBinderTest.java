package play.data.binding.types;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static play.mvc.Http.Request.createRequest;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.junit.jupiter.api.Test;
import play.Play;
import play.mvc.Http;
import play.mvc.Scope.Session;

public class DateBinderTest {
  private final DateBinder binder = new DateBinder();
  private final Http.Request request =
      createRequest(
          null, "GET", "/", "", null, null, null, null, false, 80, "localhost", null, null);
  private final Session session = new Session();

  @Test
  public void parses_date_in_play_format() throws ParseException {
    Play.configuration.setProperty("date.format", "dd.MM.yyyy");

    Date actual =
        binder.bind(request, session, "client.birthday", null, "31.12.1986", Date.class, null);
    Date expected = new SimpleDateFormat("MM/dd/yyyy").parse("12/31/1986");
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void parses_date_in_iso_format() throws ParseException {
    Date actual =
        binder.bind(
            request,
            session,
            "client.birthday",
            null,
            "ISO8601:1986-04-12T00:00:00+0500",
            Date.class,
            null);
    Date expected = new SimpleDateFormat("MM/dd/yyyyZ").parse("04/12/1986+0500");
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void parses_null_to_null() throws ParseException {
    assertThat(binder.bind(request, session, "client.birthday", null, null, Date.class, null))
        .isNull();
  }

  @Test
  public void parses_empty_string_to_null() throws ParseException {
    assertThat(binder.bind(request, session, "client.birthday", null, "", Date.class, null))
        .isNull();
  }

  @Test
  public void throws_ParseException_for_invalid_value() {
    assertThatThrownBy(
            () ->
                binder.bind(
                    request, session, "client.birthday", null, "12/31/1986", Date.class, null))
        .isInstanceOf(ParseException.class)
        .hasMessage("Unparseable date: \"12/31/1986\"");
  }
}
