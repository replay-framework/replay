package jobs;

import jakarta.inject.Inject;
import play.jobs.OnApplicationStart;
import play.jobs.SimpleJob;
import services.Counter;

@OnApplicationStart
public class CounterUpdaterJob2 extends SimpleJob {
  @Inject
  private Counter counter;

  @Override
  public void doJob() {
    counter.inc("counter.job.onApplicationStart");
  }
}
