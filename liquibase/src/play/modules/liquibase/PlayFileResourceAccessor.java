package play.modules.liquibase;

import liquibase.resource.AbstractResourceAccessor;
import liquibase.resource.InputStreamList;
import play.Play;
import play.vfs.VirtualFile;

import java.io.File;
import java.util.SortedSet;
import java.util.TreeSet;

import static java.util.stream.Collectors.toCollection;

public class PlayFileResourceAccessor extends AbstractResourceAccessor {
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
    VirtualFile virtualFile = findVirtualFile(path);
    InputStreamList returnList = new InputStreamList();
    if (virtualFile != null) {
      returnList.add(virtualFile.getURI(), virtualFile.inputstream());
    }
    return returnList;
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
