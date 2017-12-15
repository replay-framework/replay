package org.xhtmlrenderer.swing;

import org.junit.Before;
import org.junit.Test;
import play.vfs.VirtualFile;

import java.io.File;
import java.net.URI;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class NaiveUserAgentTest {

  FileSearcher fileSearcher = mock(FileSearcher.class);
  NaiveUserAgent naiveUserAgent = new NaiveUserAgent(16, fileSearcher);

  @Before
  public void setUp() {
    naiveUserAgent.setBaseURL("http://myserver.com");
  }

  @Test
  public void resolveUrlToLocalFile() throws Exception {
    URI uri = getClass().getResource("NaiveUserAgentTest.class").toURI();
    when(fileSearcher.searchFor("org/blah/NaiveUserAgentTest.class")).thenReturn(VirtualFile.open(new File(uri)));

    assertEquals(uri.toURL().toString(), naiveUserAgent.resolveURI("org/blah/NaiveUserAgentTest.class"));
  }

  @Test
  public void ignoresUrlParamsWhenResolvingToLocalFile() throws Exception {
    URI uri = getClass().getResource("NaiveUserAgentTest.class").toURI();
    when(fileSearcher.searchFor("org/blah/NaiveUserAgentTest.class")).thenReturn(VirtualFile.open(new File(uri)));

    assertEquals(uri.toURL().toString(), naiveUserAgent.resolveURI("org/blah/NaiveUserAgentTest.class?123213231"));
  }

  @Test
  public void resolvesToExternalUrlIfLocalFileNotFound() {
    assertEquals("http://myserver.com/favicon.ico", naiveUserAgent.resolveURI("/favicon.ico"));
  }
}