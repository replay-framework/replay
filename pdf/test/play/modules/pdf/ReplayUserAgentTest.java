package play.modules.pdf;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xhtmlrenderer.pdf.ITextOutputDevice;

import java.io.File;
import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ReplayUserAgentTest {

  private final ITextOutputDevice outputDevice = mock();
  private final FileSearcher fileSearcher = mock();
  private final ReplayUserAgent userAgent = new ReplayUserAgent(outputDevice, fileSearcher);

  @BeforeEach
  public void setUp() {
    userAgent.setBaseURL("http://myserver.com");
  }

  @Test
  public void resolveUrlToLocalFile() throws Exception {
    URI uri = getClass().getResource("ReplayUserAgentTest.class").toURI();
    when(fileSearcher.searchFor("org/blah/ReplayUserAgentTest.class")).thenReturn(new File(uri));

    assertThat(userAgent.resolveURI("org/blah/ReplayUserAgentTest.class")).isEqualTo(uri.toURL().toString());
  }

  @Test
  public void ignoresUrlParamsWhenResolvingToLocalFile() throws Exception {
    URI uri = getClass().getResource("ReplayUserAgentTest.class").toURI();
    when(fileSearcher.searchFor("org/blah/ReplayUserAgentTest.class")).thenReturn(new File(uri));

    assertThat(userAgent.resolveURI("org/blah/ReplayUserAgentTest.class?123213231")).isEqualTo(uri.toURL().toString());
  }

  @Test
  public void resolvesToExternalUrlIfLocalFileNotFound() {
    assertThat(userAgent.resolveURI("/favicon.ico")).isEqualTo("http://myserver.com/favicon.ico");
  }
}