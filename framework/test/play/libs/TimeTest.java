package play.libs;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

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

    @Test(expected = IllegalArgumentException.class)
    public void nullArgumentIsNotAllowed() {
        Time.parseDuration(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void emptyArgumentIsNotAllowed() {
        Time.parseDuration("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void parseBad1() {
        Time.parseDuration("1w2d3h10s");
    }

    @Test(expected = IllegalArgumentException.class)
    public void parseBad2() {
        Time.parseDuration("foobar");
    }

    @Test(expected = IllegalArgumentException.class)
    public void parseBad3() {
        Time.parseDuration("20xyz");
    }
}
