package play.modules.liquibase;

import liquibase.resource.AbstractResourceAccessor;
import liquibase.resource.PathResource;
import liquibase.resource.Resource;
import liquibase.resource.URIResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.Play;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class PlayFileResourceAccessor extends AbstractResourceAccessor {
  private static final Logger logger = LoggerFactory.getLogger(PlayFileResourceAccessor.class);

  File findFile(String changeLogPath) {
    File virtualFile = findVirtualFile(changeLogPath);
    if (virtualFile == null) throw new IllegalArgumentException("Changelog file not found: app/" + changeLogPath);
    return virtualFile;
  }

  private File findVirtualFile(String path) {
    // TODO remove this hack.
    // TODO Why LiquiBase adds prefix "app/" for included files?
    return Play.getVirtualFile(path.startsWith("app/") ? path : "app/" + path);
  }
  
  private void findInClasspath(String path, List<Resource> returnList) {
    URL resource = Thread.currentThread().getContextClassLoader().getResource(path);
    if (resource != null) {
      try {
        returnList.add(new URIResource(path, resource.toURI()));
      }
      catch (URISyntaxException e) {
        logger.error("Failed to read resource {}", resource, e);
      }
    }
  }

  String getPath(String relativeTo, String streamPath) {
    if (relativeTo == null) return streamPath;
    if (relativeTo.endsWith("/")) return relativeTo + streamPath;
    int index = relativeTo.lastIndexOf('/');
    return index == -1 ? streamPath : relativeTo.substring(0, index + 1) + streamPath;
  }

  @Override
  public List<Resource> search(String path, boolean recursive) {
    throw new UnsupportedOperationException(getClass().getName() + ".search");
  }

  @Override
  public List<Resource> getAll(String path) {
    List<Resource> foundResources = new ArrayList<>();
    File virtualFile = findVirtualFile(path);
    if (virtualFile != null) {
      foundResources.add(toResource(virtualFile));
    }
    else {
      findInClasspath(path, foundResources);
    }
    return foundResources;
  }

  @Nonnull
  @CheckReturnValue
  private PathResource toResource(File virtualFile) {
    return new PathResource(Play.relativePath(virtualFile), virtualFile.toPath());
  }

  @Override public List<String> describeLocations() {
    return List.of(Play.appRoot.getAbsolutePath());
  }

  @Override
  public void close() {
  }
}
