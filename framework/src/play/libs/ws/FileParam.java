package play.libs.ws;

import java.io.File;

public class FileParam {
  public File file;
  public String paramName;

  public FileParam(File file, String name) {
    this.file = file;
    this.paramName = name;
  }

  public static FileParam[] getFileParams(File[] files) {
    FileParam[] fileParams = new FileParam[files.length];
    for (int i = 0; i < files.length; i++) {
      fileParams[i] = new FileParam(files[i], files[i].getName());
    }
    return fileParams;
  }
}
