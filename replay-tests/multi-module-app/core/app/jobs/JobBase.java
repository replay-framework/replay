package jobs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.jobs.SimpleJob;

abstract class JobBase extends SimpleJob {

  private static final Logger logger = LoggerFactory.getLogger(JobBase.class);

  @Override
  public void doJob() {
    logger.info("Job {} just started.", getClass().getName());
  }
}
