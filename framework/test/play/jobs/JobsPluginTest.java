package play.jobs;

import org.junit.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class JobsPluginTest {
  JobsPlugin plugin = new JobsPlugin();

  @SuppressWarnings("AssignmentToStaticFieldFromInstanceMethod")
  @Test
  public void runScheduledJobOnceNow() {
    Job<?> job = mock(Job.class);
    plugin.executor = mock(ScheduledThreadPoolExecutor.class);

    plugin.runScheduledJobOnceNow(job);

    assertThat(job.runOnce).isTrue();
    verify(plugin.executor).submit((Callable<?>) job);
  }
}
