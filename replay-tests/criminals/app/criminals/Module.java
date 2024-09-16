package criminals;

import static com.google.inject.name.Names.named;

import com.google.inject.AbstractModule;
import java.util.Properties;

public class Module extends AbstractModule {
  private final Properties configuration;

  public Module(Properties configuration) {
    this.configuration = configuration;
  }

  @Override
  protected void configure() {
    bind(String.class)
        .annotatedWith(named("criminal-records.service.url"))
        .toInstance(configuration.getProperty("criminal-records.service.url"));
  }
}
