package jobs;

import jakarta.inject.Inject;
import play.jobs.On;
import play.jobs.SimpleJob;
import services.Counter;

@On("cron.counterUpdater")
public class CounterUpdaterJob3 extends SimpleJob {
  @Inject private Counter counter;

  @Override
  public void doJob() {
    counter.inc("counter.job.on");
  }
}
