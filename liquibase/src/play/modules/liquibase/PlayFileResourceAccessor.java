package play.modules.liquibase;

import liquibase.resource.AbstractResourceAccessor;
import play.Play;
import play.vfs.VirtualFile;

import java.io.File;
import java.io.InputStream;
import java.util.Set;

import static java.util.Collections.emptySet;

public class PlayFileResourceAccessor extends AbstractResourceAccessor {
  public File findFile(String changeLogPath) {
    VirtualFile virtualFile = findVirtualFile(changeLogPath);
    if (virtualFile == null) throw new IllegalArgumentException("Changelog file not found: app/" + changeLogPath);
    return virtualFile.getRealFile();
  }

  @Override public Set<InputStream> getResourcesAsStream(String path) {
    VirtualFile virtualFile = findVirtualFile(path);
    return virtualFile == null ? emptySet() : Set.of(virtualFile.inputstream());
  }

  private VirtualFile findVirtualFile(String path) {
    return Play.getVirtualFile("app/" + path);
  }

  @Override protected void init() {
  }

  @Override
  public Set<String> list(String relativeTo, String path, boolean includeFiles, boolean includeDirectories, boolean recursive) {
    throw new UnsupportedOperationException(getClass().getName() + ".list");
  }

  @Override public ClassLoader toClassLoader() {
    throw new UnsupportedOperationException(getClass().getName() + ".toClassLoader");
  }
}
