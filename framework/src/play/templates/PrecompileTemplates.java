package play.templates;

import play.Play;

import java.io.File;

public class PrecompileTemplates {
  public static void main(String[] args) {
    System.setProperty("precompile", "true");
    loadPrecompiledJavaClasses();
    precompileTemplates();
  }

  private static void loadPrecompiledJavaClasses() {
    Play.usePrecompiled = true;
    Play play = new Play();
    play.init(new File("."), "prod");
    Play.usePrecompiled = false;
  }

  private static void precompileTemplates() {
    TemplateLoader.getAllTemplate();
  }
}
