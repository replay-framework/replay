package play.modules.pdf;

import java.io.File;
import play.Play;

public class FileSearcher {
  public File searchFor(String filePath) {
    return Play.file(filePath);
  }
}
