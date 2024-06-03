package jobs;

import play.jobs.On;
import play.jobs.SimpleJob;
import services.Counter;

import jakarta.inject.Inject;

@On("cron.counterUpdater")
public class CounterUpdaterJob3 extends SimpleJob {
  @Inject
  private Counter counter;

  @Override
  public void doJob() {
    counter.inc("counter.job.on");
  }
}
