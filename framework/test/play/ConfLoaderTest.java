package play;

import org.junit.Test;

import java.util.Properties;

import static org.junit.Assert.*;

public class ConfLoaderTest {
  ConfLoader loader = new ConfLoader();

  @Test
  public void inheritConfigProperties() {
    String oldId = Play.id;
    try {
      Play.id = "web";
      Properties props = new Properties();
      props.setProperty("%prod.hello", "world");

      loader.addInheritedConfKeys(props);
      assertNull(props.getProperty("%web.hello"));

      props.setProperty("%web", "%prod");
      loader.addInheritedConfKeys(props);
      assertEquals("world", props.getProperty("%web.hello"));

      props.setProperty("%web.hello", "web");
      loader.addInheritedConfKeys(props);
      assertEquals("web", props.getProperty("%web.hello"));
    }
    finally {
      Play.id = oldId;
    }
  }
}