package play.libs;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TimeTest {
    @Test
    public void parseWithNullArgument() {
        assertEquals(2592000, Time.parseDuration(null));
    }

    @Test
    public void parseGood1() {
        assertEquals(40, Time.parseDuration("40s"));
    }

    @Test
    public void parseGood2() {
        assertEquals(187210, Time.parseDuration("2d4h10s"));
    }

    @Test
    public void parseGood3() {
        assertEquals(14410, Time.parseDuration("0d4h10s"));
    }

    @Test
    public void parseGood4() {
        assertEquals(7200, Time.parseDuration("2h"));
    }

    @Test
    public void parseGood5() {
        assertEquals(7200, Time.parseDuration("120min"));
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
