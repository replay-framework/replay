package play.templates;

import java.util.Map;

public abstract class BaseTemplate extends Template {
    public static final ThreadLocal<BaseTemplate> layout = new ThreadLocal<>();
    public static final ThreadLocal<Map<Object, Object>> layoutData = new ThreadLocal<>();
    public static final ThreadLocal<BaseTemplate> currentTemplate = new ThreadLocal<>();

  protected BaseTemplate(String name) {
    super(name);
  }

  public static final class RawData {

        public String data;

        public RawData(Object val) {
            if (val == null) {
                data = "";
            } else {
                data = val.toString();
            }
        }

        @Override
        public String toString() {
            return data;
        }
    }
}
