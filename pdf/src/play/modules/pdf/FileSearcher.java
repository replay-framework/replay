package play.modules.pdf;

import play.Play;
import play.vfs.VirtualFile;

public class FileSearcher {
  public VirtualFile searchFor(String filePath) {
    return Play.getVirtualFile(filePath);
  }
}
