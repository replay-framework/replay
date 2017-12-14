package play.libs;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class F {
    public static class Promise<V> implements Future<V>, Consumer<V> {

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
                // The result of the promise is an exception - throw it
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
                // The result of the promise is an exception - throw it
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

    public static class Tuple<A, B> {

        public final A _1;
        public final B _2;

        public Tuple(A _1, B _2) {
            this._1 = _1;
            this._2 = _2;
        }

        @Override
        public String toString() {
            return "T2(_1: " + _1 + ", _2: " + _2 + ")";
        }
    }
}