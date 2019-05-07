package play.modules.liquibase;

import liquibase.lockservice.StandardLockService;

public class NonLockingLockService extends StandardLockService {
  @Override public void waitForLock() {
    // don't waste time for in-memory database
  }
}
