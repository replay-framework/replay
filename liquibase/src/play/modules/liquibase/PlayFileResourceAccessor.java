package play.modules.liquibase;

import liquibase.resource.AbstractResourceAccessor;
import liquibase.resource.InputStreamList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.Play;
import play.vfs.VirtualFile;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.SortedSet;
import java.util.TreeSet;

import static java.util.stream.Collectors.toCollection;

public class PlayFileResourceAccessor extends AbstractResourceAccessor {
  private static final Logger logger = LoggerFactory.getLogger(PlayFileResourceAccessor.class);

  File findFile(String changeLogPath) {
    VirtualFile virtualFile = findVirtualFile(changeLogPath);
    if (virtualFile == null) throw new IllegalArgumentException("Changelog file not found: app/" + changeLogPath);
    return virtualFile.getRealFile();
  }

  private VirtualFile findVirtualFile(String path) {
    return Play.getVirtualFile("app/" + path);
  }

  @Override public InputStreamList openStreams(String relativeTo, String streamPath) {
    String path = getPath(relativeTo, streamPath);
    InputStreamList returnList = new InputStreamList();
    VirtualFile virtualFile = findVirtualFile(path);
    if (virtualFile != null) {
      returnList.add(virtualFile.getURI(), virtualFile.inputstream());
    }
    else {
      findInClasspath(path, returnList);
    }
    return returnList;
  }

  private void findInClasspath(String path, InputStreamList returnList) {
    URL resource = Thread.currentThread().getContextClassLoader().getResource(path);
    if (resource != null) {
      try {
        returnList.add(resource.toURI(), resource.openStream());
      }
      catch (URISyntaxException | IOException e) {
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
  public SortedSet<String> list(String relativeTo, String path, boolean recursive, boolean includeFiles, boolean includeDirectories) {
    throw new UnsupportedOperationException(getClass().getName() + ".list");
  }

  @Override public SortedSet<String> describeLocations() {
    return Play.roots.stream()
      .map(vf -> vf.getRealFile().getAbsolutePath())
      .collect(toCollection(TreeSet::new));
  }
}
