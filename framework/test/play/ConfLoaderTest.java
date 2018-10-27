package play;

import org.junit.After;
import org.junit.Test;
import play.utils.OrderSafeProperties;

import java.util.Optional;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

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
    assertThat(props.getProperty("hello")).isNull();
  }

  @Test
  public void override() {
    Play.id = "web";
    Properties props = new OrderSafeProperties();
    props.setProperty("hello", "initial");
    props.setProperty("%web.hello", "web");
    props = loader.resolvePlayIdOverrides(props, null);
    assertThat(props.getProperty("hello")).isEqualTo("web");
  }

  @Test
  public void playIdIsMoreSpecificAndWins() {
    Play.id = "web";
    Properties props = new OrderSafeProperties();
    props.setProperty("%web.hello", "web");
    props.setProperty("%prod.hello", "prod");
    props = loader.resolvePlayIdOverrides(props, "prod");
    assertThat(props.getProperty("hello")).isEqualTo("web");
  }

  @Test
  public void inheritIndependentOfOrder() {
    Play.id = "web";
    Properties props = new OrderSafeProperties();
    props.setProperty("%prod.hello", "prod");
    props = loader.resolvePlayIdOverrides(props, "prod");
    assertThat(props.getProperty("hello")).isEqualTo("prod");
  }

  @Test
  public void extractsPort() {
    assertThat(loader.extractHttpPort("java -Xmx256m")).isEqualTo(Optional.empty());
    assertThat(loader.extractHttpPort("java -Xmx256m --http.port=9666")).isEqualTo(Optional.of("9666"));

  }
}