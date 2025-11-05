package play.utils;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import com.google.errorprone.annotations.CheckReturnValue;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;

@NullMarked
@CheckReturnValue
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
  public Boolean get(long timeout, TimeUnit unit)
      throws InterruptedException, ExecutionException, TimeoutException {
    return true;
  }
}
