package play.rebel;

import java.util.Map;

public class RenderView extends View {
  public RenderView() {
  }

  public RenderView(String templateName) {
    super(templateName);
  }

  public RenderView(String templateName, Map<String, Object> arguments) {
    super(templateName, arguments);
  }

  @Override public RenderView with(String name, Object value) {
    return (RenderView) super.with(name, value);
  }
}

