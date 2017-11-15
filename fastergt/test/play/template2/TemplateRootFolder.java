package play.template2;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

public class TemplateRootFolder extends File {
  public TemplateRootFolder() {
    super(getTemplateRootFolder());
  }

  private static URI getTemplateRootFolder() {
    try {
      return Thread.currentThread().getContextClassLoader().getResource("template_root").toURI();
    }
    catch (URISyntaxException e) {
      throw new IllegalStateException("Folder template_root is not found in classpath");
    }
  }
}
