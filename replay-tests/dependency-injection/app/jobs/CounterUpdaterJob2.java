package jobs;

import play.jobs.OnApplicationStart;
import play.jobs.SimpleJob;
import services.Counter;

import jakarta.inject.Inject;

@OnApplicationStart
public class CounterUpdaterJob2 extends SimpleJob {
  @Inject
  private Counter counter;

  @Override
  public void doJob() {
    counter.inc("counter.job.onApplicationStart");
  }
}
