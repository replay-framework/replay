package play.template2;

import groovy.lang.Binding;
import play.modules.gtengineplugin.gt_integration.GTGroovyBase1xImpl;
import play.modules.gtengineplugin.gt_integration.GTJavaBase1xImpl;

public class TestTemplate extends GTJavaBase1xImpl {
  public TestTemplate(String path) {
    super(GTGroovyBase1xImpl.class, new GTTemplateLocation(path));
    binding = new Binding();
  }

  @Override protected void _renderTemplate() {
  }

  @Override public void insertOutput(GTRenderingResult otherTemplate) {
  }
}