package play.modules.guice;

import static java.lang.System.nanoTime;
import static java.util.Collections.singletonList;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.stream.Collectors.joining;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import java.util.List;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.inject.BeanSource;

public class GuiceBeanSource implements BeanSource {
  private static final Logger logger = LoggerFactory.getLogger(GuiceBeanSource.class);

  @Nonnull private final Injector injector;

  public GuiceBeanSource(@Nonnull Module module) {
    this(singletonList(module));
  }

  public GuiceBeanSource(@Nonnull List<Module> modules) {
    long start = nanoTime();
    logger.info(
        "Initializing guice modules: {}",
        modules.stream().map(m -> m.getClass().getSimpleName()).collect(joining(", ")));
    this.injector = Guice.createInjector(modules);
    logger.info("Initialized guice in {} ms", NANOSECONDS.toMillis(nanoTime() - start));
  }

  @Override
  public <T> T getBeanOfType(Class<T> clazz) {
    return injector.getInstance(clazz);
  }
}
