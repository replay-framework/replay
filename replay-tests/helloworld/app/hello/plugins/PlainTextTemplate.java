package hello.plugins;

import play.templates.Template;
import play.vfs.VirtualFile;

import java.util.Map;

public class PlainTextTemplate extends Template {
  private final VirtualFile source;

  public PlainTextTemplate(VirtualFile source) {
    this.source = source;
  }

  @Override
  public void compile() {
  }

  @Override
  public String internalRender(Map<String, Object> args) {
    String result = source.contentAsString();
    for (Map.Entry<String, Object> arg : args.entrySet()) {
      result = result.replace("${" + arg.getKey() + "}", arg.getValue().toString());
    }
    return result;
  }
}
