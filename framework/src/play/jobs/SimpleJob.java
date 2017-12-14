package play.jobs;

public abstract class SimpleJob extends Job<Void> {
  public abstract void doJob() throws Exception;

  @Override public final Void doJobWithResult() throws Exception {
    doJob();
    return null;
  }
}
