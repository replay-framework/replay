package play.template2;


import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import play.template2.exceptions.GTCompilationException;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.io.FileUtils.readFileToByteArray;

public abstract class IO {
  public static String readContentAsString(URL fileURL) {
    try {
      return IOUtils.toString(fileURL, UTF_8);
    }
    catch (IOException e) {
      throw new GTCompilationException("Error reading resource " + fileURL, e);
    }
  }

  public static void write(byte[] data, File file) {
    try {
      FileUtils.writeByteArrayToFile(file, data);
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static byte[] readContent(File file) {
    try {
      return readFileToByteArray(file);
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }


  /**
   * If url points to a real file on disk, we return the File-object pointing to this file.
   * if not, we return null
   *
   * @param urlFile url to file
   */
  public static File getFileFromURL(URL urlFile) {
    if ("file".equals(urlFile.getProtocol())) {
      return new File(urlFile.getFile());
    }
    else {
      return null;
    }
  }

  public static class FileInfo {
    public final long lastModified;
    public final long size;

    public FileInfo(long lastModified, long size) {
      this.lastModified = lastModified;
      this.size = size;
    }
  }

  /**
   * Returns fileInfo for the file pointed to by the url.
   * If file is inside a jar, then lastModified is set to the date of the jar.
   */
  public static FileInfo getFileInfo(URL fileURL) {
    File file = getFileFromURL(fileURL);
    if (file == null) {
      return new FileInfo(0, 0);
    }

    return new FileInfo(file.lastModified(), file.length());
  }
}
