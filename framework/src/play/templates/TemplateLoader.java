package play.templates;

import static java.lang.System.nanoTime;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static org.apache.commons.io.FileUtils.copyURLToFile;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.Play;
import play.exceptions.TemplateNotFoundException;

public class TemplateLoader {

  private static final Logger logger = LoggerFactory.getLogger(TemplateLoader.class);

  private static final Map<String, BaseTemplate> templates = new HashMap<>();
  private static final Pattern CURLEY_WRAPPED = Pattern.compile("\\{(.*)}");

  /**
   * Load a template from a file
   *
   * @param file A File
   * @return The executable template
   */
  public static Template load(File file) {
    return Play.pluginCollection
        .loadTemplate(file)
        .orElseThrow(() -> new TemplateNotFoundException(file.getAbsolutePath()));
  }

  /** Cleans the cache for all templates */
  public static void cleanCompiledCache() {
    templates.clear();
  }

  /**
   * Load a template
   *
   * @param path The path of the template (ex: Application/index.html)
   * @return The executable template
   */
  public static Template load(String path) {
    for (File vf : Play.templatesPath) {
      if (vf == null) {
        continue;
      }
      File tf = new File(vf, path);
      boolean templateExists = tf.exists();
      if (!templateExists && Play.usePrecompiled) {
        String name = CURLEY_WRAPPED
                .matcher(Play.relativePath(tf))
                .replaceAll("from_$1")
                .replace(":", "_")
                .replace("..", "parent");
        templateExists = Play.getFile("precompiled/templates/" + name).exists();
      }
      if (templateExists) {
        return TemplateLoader.load(tf);
      }
    }

    File tf = Play.file(path);
    if (tf != null) {
      return TemplateLoader.load(tf);
    }

    URL fromClasspath = Thread.currentThread().getContextClassLoader().getResource("views/" + path);
    if (fromClasspath != null) {
      return loadTemplateFromClasspath(path, fromClasspath);
    }

    URL resource = Thread.currentThread().getContextClassLoader().getResource(path);
    if (resource != null) {
      return loadTemplateFromClasspath(path, resource);
    }

    throw new TemplateNotFoundException(path);
  }

  private static Template loadTemplateFromClasspath(String path, URL resource) {
    File templateFile = new File(Play.tmpDir, path);
    try {
      copyURLToFile(resource, templateFile);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return TemplateLoader.load(Play.file("tmp/" + path));
  }

  /**
   * List all found templates
   *
   * @return A list of executable templates
   */
  public static List<Template> getAllTemplate() {
    List<Template> res = new ArrayList<>();
    for (File virtualFile : Play.templatesPath) {
      scan(res, virtualFile);
    }
    return res;
  }

  private static void scan(List<Template> templates, File current) {
    if (!current.isDirectory() && !current.getName().startsWith(".")) {
      long start = nanoTime();
      Template template = load(current);
      if (template != null) {
        template.compile();
        logger.trace(
            "{} ms to load {}", NANOSECONDS.toMillis(nanoTime() - start), current.getName());
        templates.add(template);
      }
    } else if (current.isDirectory() && !current.getName().startsWith(".")) {
      File[] files = current.listFiles();
      if (files != null) {
        for (File virtualFile : files) {
          scan(templates, virtualFile);
        }
      }
    }
  }
}
