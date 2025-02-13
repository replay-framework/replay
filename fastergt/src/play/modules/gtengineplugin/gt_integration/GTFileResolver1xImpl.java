package play.modules.gtengineplugin.gt_integration;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import play.ClasspathResource;
import play.Play;
import play.template2.GTFileResolver;
import play.template2.GTTemplateLocationReal;
import play.templates.TemplateLoader;

public class GTFileResolver1xImpl implements GTFileResolver.Resolver {
  private final List<File> templateFolders;

  public GTFileResolver1xImpl(List<File> templatesPaths) {
    templateFolders = new ArrayList<>(templatesPaths);
  }

  @Nullable
  @Override
  public GTTemplateLocationReal getTemplateLocationReal(String queryPath) {
    // look for template file in all folders in templateFolders-list
    for (File folder : templateFolders) {

      if (folder == null) {
        // look for template in working dir.
        File file = new File(queryPath);
        if (file.exists() && file.isFile()) {
          try {
            return new GTTemplateLocationReal(Play.relativePath(file), file.toURI().toURL());
          } catch (MalformedURLException e) {
            throw new RuntimeException("Failed to read template from " + file.getAbsolutePath(), e);
          }
        }
      } else {

        File file = new File(folder, queryPath);
        if (file.exists() && file.isFile()) {
          try {
            return new GTTemplateLocationReal(Play.relativePath(file), file.toURI().toURL());
          } catch (MalformedURLException e) {
            throw new RuntimeException("Failed to read template from " + file.getAbsolutePath(), e);
          }
        }
      }
    }

    // try to find it directly on the app-root before we give up
    File tf = Play.file(queryPath);
    if (tf != null && tf.exists() && !tf.isDirectory()) {
      try {
        return new GTTemplateLocationReal(Play.relativePath(tf), tf.toURI().toURL());
      } catch (MalformedURLException e) {
        throw new RuntimeException("Failed to read template from " + tf.getAbsolutePath(), e);
      }
    }

    // try to find in classpath (e.g. "tags/select.tag" is located in "io.github.replay-framework:framework.jar")
    URL fromClasspath =
        Thread.currentThread().getContextClassLoader().getResource("views/" + queryPath);
    if (fromClasspath != null) {
      return new GTTemplateLocationReal(queryPath, fromClasspath);
    }

    URL resource = Thread.currentThread().getContextClassLoader().getResource(queryPath);
    if (resource != null) {
      return new GTTemplateLocationReal(queryPath, resource);
    }

    // didn't find it
    return null;
  }

  @Nullable
  @Override
  public GTTemplateLocationReal getTemplateLocationFromRelativePath(String relativePath) {

    // TODO find in classpath?
    File vf = Play.file(relativePath);
    if (vf != null && vf.isDirectory()) {
      return null;
    }

    URL url = null;
    if (vf == null) {
      try {
        final String classloadedPrefix = "/" + Play.tmpDir.getName() + "/";
        if (relativePath.startsWith(classloadedPrefix)) {
          relativePath = relativePath.split(classloadedPrefix, 2) [1];
        }
        ClasspathResource cf = null;
        // do the search as the TemplateLoader.loadTemplateFromClasspath()'s usage:
        // first with "views/" prefix
        try {
          cf = ClasspathResource.file("views/" + relativePath);
        } catch (Exception ignored) {
          cf = ClasspathResource.file(relativePath);
        }
        url = cf.url();
      } catch (Exception e) {
        return null;
      }
    }
    try {
      return new GTTemplateLocationReal(relativePath, url == null ? vf.toURI().toURL() : url);
    } catch (MalformedURLException e) {
      throw new RuntimeException("Failed to read template from " + vf.getAbsolutePath(), e);
    }
  }
}
