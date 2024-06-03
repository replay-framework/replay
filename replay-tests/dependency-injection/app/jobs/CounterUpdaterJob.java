package jobs;

import play.jobs.Every;
import play.jobs.SimpleJob;
import services.Counter;

import jakarta.inject.Inject;

@Every("1s")
public class CounterUpdaterJob extends SimpleJob {
  @Inject
  private Counter counter;

  @Override
  public void doJob() {
    counter.inc("counter.job.every");
  }
}
