package play;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import play.libs.IO;
import play.utils.OrderSafeProperties;

import java.io.IOException;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class PropertiesConfLoaderTest {
  private final PropertiesConfLoader loader = spy(new PropertiesConfLoader());

  @BeforeEach
  @AfterEach
  public void tearDown() {
    Play.id = "";
  }

  @Test
  public void noOverriding() {
    Properties props = new OrderSafeProperties();
    props.setProperty("%web.hello", "web");
    props = loader.resolvePlayIdOverrides(props, "", null);
    assertThat(props.getProperty("hello")).isNull();
  }

  @Test
  public void override() {
    Properties props = new OrderSafeProperties();
    props.setProperty("hello", "initial");
    props.setProperty("%web.hello", "web");
    props = loader.resolvePlayIdOverrides(props, "web", null);
    assertThat(props.getProperty("hello")).isEqualTo("web");
  }

  @Test
  public void playIdIsMoreSpecificAndWins() {
    Properties props = new OrderSafeProperties();
    props.setProperty("%web.hello", "web");
    props.setProperty("%prod.hello", "prod");
    props = loader.resolvePlayIdOverrides(props, "web", "prod");
    assertThat(props.getProperty("hello")).isEqualTo("web");
  }

  @Test
  public void inheritIndependentOfOrder() {
    Properties props = new OrderSafeProperties();
    props.setProperty("%prod.hello", "prod");
    props = loader.resolvePlayIdOverrides(props, "web", "prod");
    assertThat(props.getProperty("hello")).isEqualTo("prod");
  }

  @Test
  public void envVarInterpolation() throws IOException {
    when(loader.getEnvVar("username")).thenReturn("john");
    when(loader.getEnvVar("password")).thenReturn("secret");
    Properties props = IO.readUtf8Properties("/play/conf-with-env-vars.properties");

    loader.resolveEnvironmentVariables(props, null);

    assertThat(props.getProperty("not_interpolated")).isEqualTo("${unset_env_var}");
    assertThat(props.getProperty("interpolated")).isEqualTo("john");
    assertThat(props.getProperty("interpolated_double")).isEqualTo("john-john");
    assertThat(props.getProperty("interpolated_multiple")).isEqualTo("john-secret-john");
    assertThat(props.getProperty("url")).isEqualTo("jdbc:john:secret@db");
  }
}