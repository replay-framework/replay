package play;

import static java.lang.Integer.parseInt;

import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.AbortPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.utils.PThreadFactory;

/** Run some code in a Play! context */
public class Invoker {
  private static final Logger log = LoggerFactory.getLogger(Invoker.class);

  /** Main executor for requests invocations. */
  public static ScheduledThreadPoolExecutor executor;

  static {
    String configuredPoolSize = Play.configuration.getProperty("play.pool");
    int poolSize = configuredPoolSize != null ? parseInt(configuredPoolSize) : defaultPoolSize();
    log.info("Replay thread pool size: {}, mode: {}", poolSize, Play.mode);
    executor =
        new ScheduledThreadPoolExecutor(poolSize, new PThreadFactory("play"), new AbortPolicy());
  }

  private static int defaultPoolSize() {
    return Integer.max(Runtime.getRuntime().availableProcessors() + 1, 4);
  }

  /**
   * Run the code in a new thread took from a thread pool.
   *
   * @param invocation The code to run
   * @return The future object, to know when the task is completed
   */
  public Future<?> invoke(Invocation invocation) {
    return executor.submit(invocation);
  }
}
