package play.modules.gtengineplugin;

import play.Play;
import play.plugins.PluginCollection;
import play.templates.TemplateLoader;

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
      GTEnginePlugin plugin = new GTEnginePlugin();
      plugin.onLoad();
      addPlugin(plugin);
    }
  }
}
