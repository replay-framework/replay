package play;

import org.junit.After;
import org.junit.Test;
import play.utils.OrderSafeProperties;

import java.util.Properties;

import static org.junit.Assert.*;

public class ConfLoaderTest {
  ConfLoader loader = new ConfLoader();

  @After
  public void tearDown() {
    Play.id = "";
  }

  @Test
  public void noOverriding() {
    Play.id = "";
    Properties props = new OrderSafeProperties();
    props.setProperty("%web.hello", "web");
    props = loader.resolvePlayIdOverrides(props, null);
    assertNull(props.getProperty("hello"));
  }

  @Test
  public void override() {
    Play.id = "web";
    Properties props = new OrderSafeProperties();
    props.setProperty("hello", "initial");
    props.setProperty("%web.hello", "web");
    props = loader.resolvePlayIdOverrides(props, null);
    assertEquals("web", props.getProperty("hello"));
  }

  @Test
  public void playIdIsMoreSpecificAndWins() {
    Play.id = "web";
    Properties props = new OrderSafeProperties();
    props.setProperty("%web.hello", "web");
    props.setProperty("%prod.hello", "prod");
    props = loader.resolvePlayIdOverrides(props, "prod");
    assertEquals("web", props.getProperty("hello"));
  }

  @Test
  public void inheritIndependentOfOrder() {
    Play.id = "web";
    Properties props = new OrderSafeProperties();
    props.setProperty("%prod.hello", "prod");
    props = loader.resolvePlayIdOverrides(props, "prod");
    assertEquals("prod", props.getProperty("hello"));
  }
}