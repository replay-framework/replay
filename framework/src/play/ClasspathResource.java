package play;

import org.apache.commons.io.IOUtils;
import play.exceptions.UnexpectedException;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.UTF_8;

public class ClasspathResource {
  private static final Pattern RE_JAR_FILE_NAME = Pattern.compile("file:(.+\\.jar)!.+");
  private final URL url;

  public static ClasspathResource file(String fileName) {
    return new ClasspathResource(Thread.currentThread().getContextClassLoader().getResource(fileName));
  }

  ClasspathResource(URL url) {
    this.url = url;
  }

  @Override
  public String toString() {
    return "File " + url;
  }

  public String content() {
    try {
      return IOUtils.toString(url, UTF_8);
    } catch (IOException e) {
      throw new UnexpectedException(e);
    }
  }
  
  public boolean isModifiedAfter(long timestamp) {
    switch (url.getProtocol()) {
      case "file": {
        return new File(url.getFile()).lastModified() > timestamp;
      }
      case "jar": {
        return new File(getJarFilePath()).lastModified() > timestamp;
      }
      default: {
        return false;
      }
    }
  }

  String getJarFilePath() {
    return RE_JAR_FILE_NAME.matcher(url.getPath()).replaceFirst("$1");
  }
}
