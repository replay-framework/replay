package play.modules.liquibase;

import liquibase.resource.ClassLoaderResourceAccessor;
import org.apache.commons.io.IOUtils;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toList;

public class DuplicatesIgnoringResourceAccessor extends ClassLoaderResourceAccessor {
  public DuplicatesIgnoringResourceAccessor(ClassLoader classloader) {
    super(classloader);
  }

  @Override public Set<InputStream> getResourcesAsStream(String path) throws IOException {
    List<String> foundResources = findResourcesUrls(path);
    if (foundResources.isEmpty()) return emptySet();
    if (foundResources.size() > 1) checkThatResourcesAreUnique(foundResources);

    return singleton(openUrlInputStream(foundResources.get(0)));
  }

  private void checkThatResourcesAreUnique(List<String> resources) throws IOException {
    String first = contentOf(resources.get(0));
    for (int i = 1; i < resources.size(); i++) {
      if (!contentOf(resources.get(i)).equals(first)) {
        throw new IllegalStateException("Found multiple different resources with the same name: " + resources);
      }
    }
  }

  private String contentOf(String resource) throws IOException {
    return IOUtils.toString(new URL(resource), UTF_8);
  }

  private InputStream openUrlInputStream(String freshestResource) throws IOException {
    URLConnection connection = new URL(freshestResource).openConnection();
    connection.setUseCaches(false);
    return connection.getInputStream();
  }

  @Nonnull
  private List<String> findResourcesUrls(String path) throws IOException {
    return Collections.list(toClassLoader().getResources(path))
      .stream()
      .map(url -> url.toExternalForm())
      .collect(toList());
  }
}
