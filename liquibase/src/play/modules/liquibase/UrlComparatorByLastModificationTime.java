package play.modules.liquibase;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Comparator;

public class UrlComparatorByLastModificationTime implements Comparator<String> {
  @Override public int compare(String url1, String url2) {
    if (!url1.startsWith("file:") && !url2.startsWith("file:")) return 0;
    if (!url1.startsWith("file:")) return 1;
    if (!url2.startsWith("file:")) return -1;

    return Long.compare(getLastModificationTime(url2), getLastModificationTime(url1));
  }

  private long getLastModificationTime(String url2) {
    try {
      return new File(new URL(url2).toURI()).lastModified();
    }
    catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
    catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
  }
}
