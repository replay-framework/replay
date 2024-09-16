package play.jobs;

@On("cron.blah")
public class DummyJob extends SimpleJob {
  @Override
  public void doJob() {}
}
