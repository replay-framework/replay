package play.modules.gtengineplugin;

import play.Play;
import play.PlayPlugin;
import play.plugins.PluginCollection;
import play.templates.Template;
import play.templates.TemplateLoader;
import play.vfs.VirtualFile;

import java.util.Map;
import java.util.Optional;

public class PrecompileTemplates {
  public static void main(String[] args) {
    System.setProperty("precompile", "true");
    loadPrecompiledJavaClasses();
    precompileTemplates();
  }

  private static void loadPrecompiledJavaClasses() {
    System.setProperty("precompiled", "true");
    Play play = new Play();
    play.pluginCollection = new SinglePluginCollection();
    play.init("prod");
    Play.usePrecompiled = false;
  }

  private static void precompileTemplates() {
    TemplateLoader.getAllTemplate();
  }

  private static class SinglePluginCollection extends PluginCollection {
    @Override public void loadPlugins() {
      IgnoreOtherTemplatesPlugin otherTemplatesPlugin = new IgnoreOtherTemplatesPlugin();
      otherTemplatesPlugin.index = 1;
      addPlugin(otherTemplatesPlugin);

      GTEnginePlugin plugin = new GTEnginePlugin();
      plugin.index = 2;
      plugin.onLoad();
      addPlugin(plugin);
    }
  }

  private static class IgnoreOtherTemplatesPlugin extends PlayPlugin {
    private final DummyTemplate dummyTemplate = new DummyTemplate();

    @Override public Optional<Template> loadTemplate(VirtualFile file) {
      if (file.getName().endsWith(".html") || file.getName().endsWith(".tag")) {
        // will be precompiled by GTEnginePlugin
        return Optional.empty();
      }
      if (file.getName().endsWith(".xls") || file.getName().endsWith(".js")) {
        // no need to precompile
        return Optional.of(dummyTemplate);
      }

      System.out.println("Unknown template type to precompile: " + file.getName());
      return Optional.empty();
    }
  }

  private static class DummyTemplate extends Template {
    @Override public void compile() {
    }

    @Override protected String internalRender(Map<String, Object> args) {
      throw new UnsupportedOperationException();
    }
  }
}
