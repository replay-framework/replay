package play.jobs;

import org.junit.Test;
import play.Play;

import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class JobTest {
  @Test
  public void callScheduledJob() {
    mockPlay();
    ScheduledThreadPoolExecutor executor = mock(ScheduledThreadPoolExecutor.class);
    JobsPlugin.executor = executor;

    Job<Void> job = new TestJob();
    job.executor = executor;

    job.call();

    assertThat(job.runOnce).isFalse();
    verify(executor).schedule(eq((Callable<?>) job), anyLong(), eq(TimeUnit.MILLISECONDS));
  }

  @Test
  public void callJob() {
    mockPlay();
    ScheduledThreadPoolExecutor executor = mock(ScheduledThreadPoolExecutor.class);
    JobsPlugin.executor = executor;

    Job<Void> job = new TestJob();

    job.call();

    assertThat(job.runOnce).isFalse();
    verify(executor, never()).schedule(any(Callable.class), anyLong(), any());
  }

  @Test
  public void callScheduledJobOnce() {
    mockPlay();
    ScheduledThreadPoolExecutor executor = mock(ScheduledThreadPoolExecutor.class);
    JobsPlugin.executor = executor;

    Job<Void> job = new TestJob();
    job.executor = executor;
    job.runOnce = true;

    job.call();

    assertThat(job.runOnce).isFalse();
    verify(executor, never()).schedule(any(Callable.class), anyLong(), any());
  }

  private void mockPlay() {
    Play.mode = Play.Mode.PROD;
    Play.started = true;
  }

  @On("0 0 10 * * ?")
  private static class TestJob extends SimpleJob {
    @Override public void doJob() {
    }
  }
}
