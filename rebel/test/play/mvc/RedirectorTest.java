package play.mvc;

import org.junit.Before;
import org.junit.Test;
import play.Play;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

import static java.util.Collections.singletonMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class RedirectorTest {
  Redirector redirector;

  @Before
  public void setUp() {
    Play.configuration.clear();
    redirector = spy(new Redirector());
    doNothing().when(redirector).toUrl(anyString());
  }

  @Test
  public void toUrl_noParams() {
    redirector.toUrl("/foo/bar");

    verify(redirector).toUrl("/foo/bar");
  }

  @Test
  public void toUrl_byUrl() {
    redirector.toUrl(new Url("/foo/bar"));

    verify(redirector).toUrl("/foo/bar");
  }

  @Test
  public void toUrlWithParams_byMap() {
    redirector.toUrl("/foo/bar", singletonMap("a", "1"));

    verify(redirector).toUrl("/foo/bar?a=1");
  }

  @Test
  public void toUrlWithParams_urlWithParams() {
    redirector.toUrl("/foo/bar?a=5", singletonMap("b", "6"));

    verify(redirector).toUrl("/foo/bar?a=5&b=6");
  }

  @Test
  public void toUrlWith1Param() {
    redirector.toUrl("/url", "name", "value");

    verify(redirector).toUrl("/url?name=value");
  }

  @Test
  public void toUrlWith2Params() {
    redirector.toUrl("/url", "name", "value", "name2", 1);

    verify(redirector).toUrl("/url?name=value&name2=1");
  }

  @Test
  public void toUrlWith3Params() {
    redirector.toUrl("/url", "name", "value", "name2", 1, "name3", true);

    verify(redirector).toUrl("/url?name=value&name2=1&name3=true");
  }

  @Test
  public void toUrlWith4Params() {
    redirector.toUrl("/url", "name", "value", "name2", 1, "name3", true, "name4", 4L);

    verify(redirector).toUrl("/url?name=value&name2=1&name3=true&name4=4");
  }

  @Test
  public void toUrlWith5Params() {
    Date date = Date.from(LocalDate.of(2000, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant());

    redirector.toUrl("/url", "name", "value", "name2", 1, "name3", true, "name4", 4L, "date", date);

    verify(redirector).toUrl("/url?name=value&name2=1&name3=true&name4=4&date=01.01.2000");
  }
}