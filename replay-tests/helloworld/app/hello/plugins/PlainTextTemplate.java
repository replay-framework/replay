package hello.plugins;

import play.templates.Template;

import java.io.File;
import java.util.Map;

import static play.libs.IO.contentAsString;

public class PlainTextTemplate extends Template {
  private final File source;

  public PlainTextTemplate(File source) {
    super(source.getName());
    this.source = source;
  }

  @Override
  public void compile() {
  }

  @Override
  protected String internalRender(Map<String, Object> args) {
    String result = contentAsString(source);
    for (Map.Entry<String, Object> arg : args.entrySet()) {
      result = result.replace("${" + arg.getKey() + "}", arg.getValue().toString());
    }
    return result;
  }
}
