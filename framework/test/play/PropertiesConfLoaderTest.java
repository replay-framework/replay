package play;

import org.junit.After;
import org.junit.Test;
import org.mockito.Mockito;
import play.utils.OrderSafeProperties;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

public class PropertiesConfLoaderTest {
  PropertiesConfLoader loader = new PropertiesConfLoader();

  @After
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
  public void envVarInterpolation() {
    PropertiesConfLoader propsConfLoaderSpy = Mockito.spy(loader);
    Mockito.when(propsConfLoaderSpy.getEnvVar("set_env_var")).thenReturn("correct_value");
    Properties props = new OrderSafeProperties();
    props.setProperty("interpolated", "${set_env_var}");
    props.setProperty("not_interpolated", "${unset_env_var}");
    propsConfLoaderSpy.resolveEnvironmentVariables(props, null);
    assertThat(props.getProperty("interpolated")).isEqualTo("correct_value");
    assertThat(props.getProperty("not_interpolated")).isEqualTo("${unset_env_var}");
  }
}