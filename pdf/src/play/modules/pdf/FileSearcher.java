package play.modules.pdf;

import play.Play;

import java.io.File;

public class FileSearcher {
  public File searchFor(String filePath) {
    return Play.getVirtualFile(filePath);
  }
}
