package play.rebel;

import java.util.Map;
import com.google.errorprone.annotations.CheckReturnValue;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
@CheckReturnValue
public class RenderView extends View {
  public RenderView() {}

  public RenderView(String templateName) {
    super(templateName);
  }

  public RenderView(String templateName, Map<String, Object> arguments) {
    super(templateName, arguments);
  }

  @Override
  public RenderView with(String name, @Nullable Object value) {
    return (RenderView) super.with(name, value);
  }
}
