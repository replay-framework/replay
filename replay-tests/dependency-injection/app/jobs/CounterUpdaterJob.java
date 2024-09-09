package jobs;

import jakarta.inject.Inject;
import play.jobs.Every;
import play.jobs.SimpleJob;
import services.Counter;

@Every("1s")
public class CounterUpdaterJob extends SimpleJob {
  @Inject
  private Counter counter;

  @Override
  public void doJob() {
    counter.inc("counter.job.every");
  }
}
