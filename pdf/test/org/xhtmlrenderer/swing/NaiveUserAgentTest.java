package org.xhtmlrenderer.swing;

import org.junit.Before;
import org.junit.Test;
import play.Play;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static play.vfs.VirtualFile.fromRelativePath;

public class NaiveUserAgentTest {

  private NaiveUserAgent naiveUserAgent = new NaiveUserAgent();

  @Before
  public void setUp() {
    Play.roots = asList(fromRelativePath("src"), fromRelativePath("test"), fromRelativePath("conf"));
    naiveUserAgent.setBaseURL("http://myserver.com");
  }

  @Test
  public void resolveUrlToLocalFile() {
    assertEquals("file:" + System.getProperty("user.dir") + "/conf/dependencies.yml", 
        naiveUserAgent.resolveURI("dependencies.yml"));
  }

  @Test
  public void ignoresUrlParamsWhenResolvingToLocalFile() {
    assertEquals("file:" + System.getProperty("user.dir") + "/conf/dependencies.yml", 
        naiveUserAgent.resolveURI("dependencies.yml?123213231"));
  }

  @Test
  public void resolvesToExternalUrlIfLocalFileNotFound() {
    assertEquals("http://myserver.com/favicon.ico", naiveUserAgent.resolveURI("/favicon.ico"));
  }
}