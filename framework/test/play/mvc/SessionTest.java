package play.mvc;

import org.junit.Test;
import play.PlayBuilder;
import play.mvc.Scope.Session;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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

        session.changed = false;
        session.clear();
        assertTrue(session.changed);
    }
}
