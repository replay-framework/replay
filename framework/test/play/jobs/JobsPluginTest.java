package play.jobs;

import org.junit.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class JobsPluginTest {
  JobsPlugin plugin = new JobsPlugin();

  @Test
  public void runScheduledJobOnceNow() {
    Job<?> job = mock(Job.class);
    plugin.executor = mock(ScheduledThreadPoolExecutor.class);

    plugin.runScheduledJobOnceNow(job);

    assertThat(job.runOnce).isTrue();
    verify(plugin.executor).submit((Callable<?>) job);
  }

  @Test
  public void requireNotNull() {
    try {
      JobsPlugin.requireNotNull(null, "cron.blah", DummyJob.class, On.class);
      fail("expected IllegalArgumentException in case of misconfigured cron property");
    }
    catch (IllegalArgumentException expected) {
      assertThat(expected.getMessage()).isEqualTo("Misconfigured setting 'cron.blah' in class 'play.jobs.DummyJob' annotation '@On'");
    }
  }
}
