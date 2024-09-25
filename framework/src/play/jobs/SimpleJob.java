package play.jobs;

/** Base class for jobs that don't return any result */
public abstract class SimpleJob extends Job<Void> {
  public abstract void doJob() throws Exception;

  @Override
  public final Void doJobWithResult() throws Exception {
    doJob();
    return null;
  }
}
