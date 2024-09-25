package play.jobs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import org.junit.jupiter.api.Test;

public class JobsPluginTest {
  private final JobsPlugin plugin = new JobsPlugin();

  @Test
  public void runScheduledJobOnceNow() {
    Job<?> job = mock(Job.class);
    JobsPlugin.executor = mock(ScheduledThreadPoolExecutor.class);

    JobsPlugin.runScheduledJobOnceNow(job);

    assertThat(job.runOnce).isTrue();
    verify(JobsPlugin.executor).submit((Callable<?>) job);
  }

  @Test
  public void requireNotNull() {
    try {
      JobsPlugin.requireNotNull(null, "cron.blah", DummyJob.class, On.class);
      fail("expected IllegalArgumentException in case of misconfigured cron property");
    } catch (IllegalArgumentException expected) {
      assertThat(expected.getMessage())
          .isEqualTo(
              "Misconfigured setting 'cron.blah' in class 'play.jobs.DummyJob' annotation '@On'");
    }
  }
}
