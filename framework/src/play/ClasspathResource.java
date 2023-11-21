package play;

import org.apache.commons.io.IOUtils;
import play.exceptions.UnexpectedException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.list;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

public class ClasspathResource {
  private static final Pattern RE_JAR_FILE_NAME = Pattern.compile("file:(.+\\.jar)!.+");
  private final String fileName;
  private final URL url;

  public static ClasspathResource file(String fileName) {
    return new ClasspathResource(fileName, Thread.currentThread().getContextClassLoader().getResource(fileName));
  }

  public static List<ClasspathResource> files(String fileName) {
    try {
      return list(Thread.currentThread().getContextClassLoader().getResources(fileName)).stream()
        .map(url -> new ClasspathResource(fileName, url))
        .collect(toList());
    }
    catch (IOException e) {
      throw new IllegalArgumentException("Failed to read files " + fileName);
    }
  }

  ClasspathResource(String fileName, URL url) {
    this.fileName = fileName;
    this.url = requireNonNull(url, () -> "File not found in classpath: " + fileName);
  }

  public URL url() {
    return url;
  }

  @Override
  public String toString() {
    return fileName;
  }

  public String content() {
    try {
      return IOUtils.toString(url, UTF_8);
    } catch (IOException e) {
      throw new UnexpectedException(e);
    }
  }
  
  public List<String> lines() {
    try (InputStream in = url.openStream()) {
      return IOUtils.readLines(in, UTF_8);
    }
    catch (IOException e) {
      throw new RuntimeException("Failed to read file " + url, e);
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
