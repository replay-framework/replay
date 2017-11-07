package play.modules.liquibase;

import liquibase.resource.ClassLoaderResourceAccessor;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;

public class DuplicatesIgnoringResourceAccessor extends ClassLoaderResourceAccessor {
  public DuplicatesIgnoringResourceAccessor(ClassLoader classloader) {
    super(classloader);
  }

  @Override public Set<InputStream> getResourcesAsStream(String path) throws IOException {
    Set<String> foundResources = findResourcesUrls(path);
    if (foundResources == null || foundResources.isEmpty()) return emptySet();

    return singleton(openUrlInputStream(findNewestResource(foundResources)));
  }

  private String findNewestResource(Set<String> foundResources) {
    SortedSet<String> sortedUrls = new TreeSet<>(new UrlComparatorByLastModificationTime());
    sortedUrls.addAll(foundResources);

    return sortedUrls.iterator().next();
  }

  private InputStream openUrlInputStream(String freshestResource) throws IOException {
    URLConnection connection = new URL(freshestResource).openConnection();
    connection.setUseCaches(false);
    return connection.getInputStream();
  }

  private Set<String> findResourcesUrls(String path) throws IOException {
    Enumeration<URL> resources = toClassLoader().getResources(path);
    if (resources == null || !resources.hasMoreElements()) {
      return null;
    }
    Set<String> allUrls = new HashSet<>();
    while (resources.hasMoreElements()) {
      URL url = resources.nextElement();
      allUrls.add(url.toExternalForm());
    }
    
    return allUrls;
  }
}
