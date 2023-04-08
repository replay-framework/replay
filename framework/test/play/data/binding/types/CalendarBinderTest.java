package play.data.binding.types;

import org.junit.Test;
import play.Play;
import play.mvc.Http.Request;
import play.mvc.Scope.Session;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static play.mvc.Http.Request.createRequest;

public class CalendarBinderTest {

    private final CalendarBinder binder = new CalendarBinder();
    private final Request request = createRequest(null, "GET", "/", "", null, null, null, null, false, 80, "localhost", false, null, null);
    private final Session session = new Session();

    @Test
    public void parses_date_to_calendar() throws ParseException {
        Play.configuration.setProperty("date.format", "dd.MM.yyyy");
        Date expected = new SimpleDateFormat("dd.MM.yyyy").parse("31.12.1986");
        Calendar actual = binder.bind(request, session, "client.birthday", null, "31.12.1986", Calendar.class, null);
        assertEquals(expected, actual.getTime());
    }
    
    @Test
    public void parses_null_to_null() throws ParseException {
        assertNull(binder.bind(request, session, "client.birthday", null, null, Calendar.class, null));
    }
    
    @Test
    public void parses_empty_string_to_null() throws ParseException {
        assertNull(binder.bind(request, session, "client.birthday", null, "", Calendar.class, null));
    }

    @Test(expected = ParseException.class)
    public void throws_ParseException_for_invalid_value() throws ParseException {
        binder.bind(request, session, "client.birthday", null, "12/31/1986", Calendar.class, null);
    }
}