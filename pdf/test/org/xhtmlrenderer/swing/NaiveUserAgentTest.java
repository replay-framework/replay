package org.xhtmlrenderer.swing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import play.vfs.VirtualFile;

import java.io.File;
import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class NaiveUserAgentTest {

  private final FileSearcher fileSearcher = mock(FileSearcher.class);
  private final NaiveUserAgent naiveUserAgent = new NaiveUserAgent(16, fileSearcher);

  @BeforeEach
  public void setUp() {
    naiveUserAgent.setBaseURL("http://myserver.com");
  }

  @Test
  public void resolveUrlToLocalFile() throws Exception {
    URI uri = getClass().getResource("NaiveUserAgentTest.class").toURI();
    when(fileSearcher.searchFor("org/blah/NaiveUserAgentTest.class")).thenReturn(VirtualFile.open(new File(uri)));

    assertThat(naiveUserAgent.resolveURI("org/blah/NaiveUserAgentTest.class")).isEqualTo(uri.toURL().toString());
  }

  @Test
  public void ignoresUrlParamsWhenResolvingToLocalFile() throws Exception {
    URI uri = getClass().getResource("NaiveUserAgentTest.class").toURI();
    when(fileSearcher.searchFor("org/blah/NaiveUserAgentTest.class")).thenReturn(VirtualFile.open(new File(uri)));

    assertThat(naiveUserAgent.resolveURI("org/blah/NaiveUserAgentTest.class?123213231")).isEqualTo(uri.toURL().toString());
  }

  @Test
  public void resolvesToExternalUrlIfLocalFileNotFound() {
    assertThat(naiveUserAgent.resolveURI("/favicon.ico")).isEqualTo("http://myserver.com/favicon.ico");
  }

  @Test
  public void _imageCache_shouldNotBePrivate() {
    assertThat(naiveUserAgent._imageCache)
      .as("it's directly accessed by ITextUserAgent.java:69")
      .isNotNull();
  }
}