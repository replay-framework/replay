package play.mvc;

import org.junit.Test;
import play.PlayBuilder;
import play.mvc.Scope.Session;

import static org.junit.Assert.*;
import static play.mvc.Scope.Session.TS_KEY;

public class SessionTest {

    @org.junit.Before
    public void playBuilderBefore() {
        new PlayBuilder().build();
        Scope.sessionStore = null;
    }

    @Test
    public void testSessionManipulationMethods() {
        Session session = new Session();
        assertFalse(session.changed);

        session.change();
        assertTrue(session.changed);

        // Reset
        session.changed = false;
        session.put("username", "Alice");
        assertTrue(session.changed);

        session.changed = false;
        session.remove("username");
        assertTrue(session.changed);
    }

    @Test
    public void clear() throws Exception {
        Session session = new Session();
        session.changed = false;
        session.put("foo", "bar");
        session.put(TS_KEY, "12/01/2017");

        session.clear();

        assertTrue(session.changed);
        assertEquals(1, session.data.size());
        assertEquals("12/01/2017", session.data.get(TS_KEY));
    }
}
