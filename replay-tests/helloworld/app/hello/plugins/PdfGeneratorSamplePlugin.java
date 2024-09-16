package hello.plugins;

import java.io.File;
import java.util.Optional;
import play.PlayPlugin;
import play.templates.Template;

public class PdfGeneratorSamplePlugin extends PlayPlugin {
  @Override
  public Optional<Template> loadTemplate(File file) {
    return file.getName().endsWith(".txt")
        ? Optional.of(new PlainTextTemplate(file))
        : Optional.empty();
  }
}
