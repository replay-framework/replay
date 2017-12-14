package play.jobs;

import org.junit.Test;
import play.Play;

import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.*;

@SuppressWarnings("AssignmentToStaticFieldFromInstanceMethod")
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

  @SuppressWarnings("rawtypes")
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
