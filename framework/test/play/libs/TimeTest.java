package play.libs;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TimeTest {
    @Test
    public void parseGood1() {
      assertThat(Time.parseDuration("40s")).isEqualTo(40);
    }

    @Test
    public void parseGood2() {
      assertThat(Time.parseDuration("2d4h10s")).isEqualTo(187210);
    }

    @Test
    public void parseGood3() {
      assertThat(Time.parseDuration("0d4h10s")).isEqualTo(14410);
    }

    @Test
    public void parseGood4() {
      assertThat(Time.parseDuration("2h")).isEqualTo(7200);
    }

    @Test
    public void parseGood5() {
      assertThat(Time.parseDuration("120min")).isEqualTo(7200);
    }

    @Test
    public void parseGood6() {
      assertThat(Time.parseDuration("30d")).isEqualTo(2592000);
    }

    @Test
    public void nullArgumentIsNotAllowed() {
        assertThatThrownBy(() -> Time.parseDuration(null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("duration cannot be null");
    }

    @Test
    public void emptyArgumentIsNotAllowed() {
        assertThatThrownBy(() -> Time.parseDuration(""))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Invalid duration pattern: \"\"");
    }

    @Test
    public void parseBad1() {
        assertThatThrownBy(() -> Time.parseDuration("1w2d3h10s"))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Invalid duration pattern: \"1w2d3h10s\"");
    }

    @Test
    public void parseBad2() {
        assertThatThrownBy(() -> Time.parseDuration("foobar"))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Invalid duration pattern: \"foobar\"");
    }

    @Test
    public void parseBad3() {
        assertThatThrownBy(() -> Time.parseDuration("20xyz"))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Invalid duration pattern: \"20xyz\"");
    }
}
