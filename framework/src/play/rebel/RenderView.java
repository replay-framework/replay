package play.rebel;

import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class RenderView extends View {
  public RenderView() {}

  public RenderView(@Nonnull String templateName) {
    super(templateName);
  }

  public RenderView(@Nonnull String templateName, @Nonnull Map<String, Object> arguments) {
    super(templateName, arguments);
  }

  @Override
  public RenderView with(@Nonnull String name, @Nullable Object value) {
    return (RenderView) super.with(name, value);
  }
}
