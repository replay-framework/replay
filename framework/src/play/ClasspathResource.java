package play;

import org.apache.commons.io.IOUtils;
import play.libs.IO;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Properties;
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
    return read(in -> IOUtils.toString(url, UTF_8));
  }
  
  public List<String> lines() {
    return read(in -> IOUtils.readLines(in, UTF_8));
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

  public Properties toProperties() {
    return read(IO::readUtf8Properties);
  }
  
  private <T> T read(IOFunction<InputStream, T> lambda) {
    try (InputStream in = new BufferedInputStream(url.openStream())) {
      return lambda.apply(in);
    } catch (IOException e) {
      throw new IllegalArgumentException("Failed to read file " + fileName + " content from " + url, e);
    }
  }

  @FunctionalInterface
  private interface IOFunction<Input, Output> {
    Output apply(Input input) throws IOException;
  }
}
