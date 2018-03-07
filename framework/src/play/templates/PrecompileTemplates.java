package play.templates;

import play.Play;

public class PrecompileTemplates {
  public static void main(String[] args) {
    System.setProperty("precompile", "true");
    loadPrecompiledJavaClasses();
    precompileTemplates();
  }

  private static void loadPrecompiledJavaClasses() {
    System.setProperty("precompiled", "true");
    Play play = new Play();
    play.init("prod");
    Play.usePrecompiled = false;
  }

  private static void precompileTemplates() {
    TemplateLoader.getAllTemplate();
  }
}
