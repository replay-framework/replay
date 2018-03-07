package play.mvc;

import org.junit.Before;
import org.junit.Test;
import play.Play;

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
  public void toUrlWith1Param() {
    redirector.toUrl("/url", "name", "value");

    verify(redirector).toUrl("/url?name=value");
  }

  @Test
  public void toUrlWith4Params() {
    redirector.toUrl("/url", "name", "value", "name2", 1, "name3", true, "name4", 4L);

    verify(redirector).toUrl("/url?name=value&name2=1&name3=true&name4=4");
  }
}