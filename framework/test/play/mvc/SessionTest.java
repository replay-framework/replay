package play.mvc;

import static org.assertj.core.api.Assertions.assertThat;
import static play.mvc.Scope.Session.TS_KEY;

import org.junit.jupiter.api.Test;
import play.PlayBuilder;
import play.mvc.Scope.Session;

public class SessionTest {

  @org.junit.jupiter.api.BeforeEach
  public void playBuilderBefore() {
    new PlayBuilder().build();
  }

  @Test
  public void testSessionManipulationMethods() {
    Session session = new Session();
    assertThat(session.changed).isFalse();

    session.change();
    assertThat(session.changed).isTrue();

    // Reset
    session.changed = false;
    session.put("username", "Alice");
    assertThat(session.changed).isTrue();

    session.changed = false;
    session.remove("username");
    assertThat(session.changed).isTrue();
  }

  @Test
  public void clear() {
    Session session = new Session();
    session.changed = false;
    session.put("foo", "bar");
    session.put(TS_KEY, "12/01/2017");

    session.clear();

    assertThat(session.changed).isTrue();
    assertThat(session.data.size()).isEqualTo(1);
    assertThat(session.data.get(TS_KEY)).isEqualTo("12/01/2017");
  }
}
