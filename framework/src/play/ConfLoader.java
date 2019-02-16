package play;

import java.util.Properties;

public interface ConfLoader {
  Properties readConfiguration(String playId);
}
