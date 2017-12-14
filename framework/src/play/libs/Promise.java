package play.libs;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class Promise<V> implements Future<V>, Consumer<V> {

    protected final CountDownLatch taskLock = new CountDownLatch(1);
    protected boolean cancelled;

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
        return invoked;
    }

    @Override
    public V get() throws InterruptedException, ExecutionException {
        taskLock.await();
        if (exception != null) {
            throw new ExecutionException(exception);
        }
        return result;
    }

    @Override
    public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        if(!taskLock.await(timeout, unit)) {
          throw new TimeoutException(String.format("Promise didn't redeem in %s %s", timeout, unit));
        }

        if (exception != null) {
            throw new ExecutionException(exception);
        }
        return result;
    }
    protected List<Consumer<Promise<V>>> callbacks = new ArrayList<>();
    protected boolean invoked;
    protected V result;
    protected Throwable exception;

    @Override
    public void accept(V result) {
        invokeWithResultOrException(result, null);
    }

    public void invokeWithException(Throwable t) {
        invokeWithResultOrException(null, t);
    }

    protected void invokeWithResultOrException(V result, Throwable t) {
        synchronized (this) {
            if (!invoked) {
                invoked = true;
                this.result = result;
                this.exception = t;
                taskLock.countDown();
            } else {
                return;
            }
        }
        for (Consumer<Promise<V>> callback : callbacks) {
            callback.accept(this);
        }
    }
}
