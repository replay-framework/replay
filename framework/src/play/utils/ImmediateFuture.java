package play.utils;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.annotation.Nonnull;

public final class ImmediateFuture implements Future<Boolean> {
  @Override
  public boolean cancel(boolean mayInterruptIfRunning) {
    return false;
  }

  @Override
  public boolean isCancelled() {
    return false;
  }

  @Override
  public boolean isDone() {
    return true;
  }

  @Override
  public Boolean get() throws InterruptedException, ExecutionException {
    return true;
  }

  @Override
  public Boolean get(long timeout, @Nonnull TimeUnit unit)
      throws InterruptedException, ExecutionException, TimeoutException {
    return true;
  }
}
