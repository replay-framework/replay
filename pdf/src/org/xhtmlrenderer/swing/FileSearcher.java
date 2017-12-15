package org.xhtmlrenderer.swing;

import play.Play;
import play.vfs.VirtualFile;

public class FileSearcher {
  public VirtualFile searchFor(String filePath) {
    return Play.getVirtualFile(filePath);
  }
}
