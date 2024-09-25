package play.jobs;

import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.Invocation;
import play.InvocationContext;
import play.Play;
import play.db.jpa.JPA;
import play.exceptions.PlayException;
import play.exceptions.UnexpectedException;
import play.libs.Promise;
import play.libs.Time;

/**
 * A job is an asynchronously executed unit of work
 *
 * @param <V> The job result type (if any)
 */
public abstract class Job<V> extends Invocation implements Callable<V> {
  private static final Logger logger = LoggerFactory.getLogger(Job.class);

  public static final String invocationType = "Job";

  protected ExecutorService executor;
  protected long lastRun;
  protected boolean wasError;
  protected Throwable lastException;
  boolean runOnce;

  Date nextPlannedExecution;

  @Override
  public InvocationContext getInvocationContext() {
    return new InvocationContext(invocationType, this.getClass().getAnnotations());
  }

  /**
   * Here you do the job and return a result
   *
   * @return The job result
   */
  public abstract V doJobWithResult() throws Exception;

  @Override
  public void execute() {}

  /**
   * Start this job now (well ASAP)
   *
   * @return the job completion
   */
  public Promise<V> now() {
    Promise<V> smartFuture = new Promise<>();
    JobsPlugin.executor.submit(getJobCallingCallable(smartFuture));
    return smartFuture;
  }

  /**
   * Start this job in several seconds
   *
   * @param delay time in seconds
   * @return the job completion
   */
  public Promise<V> in(String delay) {
    return in(Time.parseDuration(delay));
  }

  /**
   * Start this job in several seconds
   *
   * @param seconds time in seconds
   * @return the job completion
   */
  public Promise<V> in(int seconds) {
    Promise<V> smartFuture = new Promise<>();
    JobsPlugin.executor.schedule(getJobCallingCallable(smartFuture), seconds, TimeUnit.SECONDS);
    return smartFuture;
  }

  private Callable<V> getJobCallingCallable(final Promise<V> smartFuture) {
    return () -> {
      try {
        V result = this.call();
        if (smartFuture != null) {
          smartFuture.accept(result);
        }
        return result;
      } catch (Exception e) {
        if (smartFuture != null) {
          smartFuture.invokeWithException(e);
        }
        return null;
      }
    };
  }

  /**
   * Run this job every n seconds
   *
   * @param delay time in seconds
   */
  public void every(String delay) {
    every(Time.parseDuration(delay));
  }

  /**
   * Run this job every n seconds
   *
   * @param seconds time in seconds
   */
  public void every(int seconds) {
    JobsPlugin.executor.scheduleWithFixedDelay(this, seconds, seconds, TimeUnit.SECONDS);
    JobsPlugin.scheduledJobs.add(this);
  }

  private void onJobInvocationException(Throwable e) {
    wasError = true;
    lastException = e;
    try {
      logger.error("Unexpected exception in job {}", this, e);
      Play.pluginCollection.onJobInvocationException(e);
    } catch (Throwable ex) {
      logger.error("Error during job exception handling ({})", this, ex);
      throw new UnexpectedException(unwrap(e));
    }
  }

  private Throwable unwrap(Throwable e) {
    while ((e instanceof PlayException)
        && e.getCause() != null) {
      e = e.getCause();
    }
    return e;
  }

  @Override
  public void run() {
    call();
  }

  @Override
  public V call() {
    try {
      if (init()) {
        before();
        lastException = null;
        lastRun = System.currentTimeMillis();
        V result = JPA.withinFilter(() -> doJobWithResult());
        wasError = false;
        after();
        return result;
      }
    } catch (Throwable e) {
      onJobInvocationException(e);
    } finally {
      _finally();
    }
    return null;
  }

  protected void _finally() {
    Play.pluginCollection.onJobInvocationFinally();
    InvocationContext.current.remove();

    synchronized (this) {
      try {
        if (executor == JobsPlugin.executor && !runOnce) {
          JobsPlugin.scheduleForCRON(this);
        }
      } finally {
        runOnce = false;
      }
    }
  }

  @Override
  public String toString() {
    return this.getClass().getName();
  }
}
