package play.libs;

import java.util.*;
import java.util.concurrent.*;

public class F {
    public static class Promise<V> implements Future<V>, F.Action<V> {

        protected final CountDownLatch taskLock = new CountDownLatch(1);
        protected boolean cancelled = false;

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

        public V getOrNull() {
            return result;
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
        protected List<F.Action<Promise<V>>> callbacks = new ArrayList<>();
        protected boolean invoked = false;
        protected V result = null;
        protected Throwable exception = null;

        @Override
        public void invoke(V result) {
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
            for (F.Action<Promise<V>> callback : callbacks) {
                callback.invoke(this);
            }
        }

        public void onRedeem(F.Action<Promise<V>> callback) {
            synchronized (this) {
                if (!invoked) {
                    callbacks.add(callback);
                }
            }
            if (invoked) {
                callback.invoke(this);
            }
        }

        public static <T> Promise<List<T>> waitAll(Promise<T>... promises) {
            return waitAll(Arrays.asList(promises));
        }

        public static <T> Promise<List<T>> waitAll(final Collection<Promise<T>> promises) {
            final CountDownLatch waitAllLock = new CountDownLatch(promises.size());
            final Promise<List<T>> result = new Promise<List<T>>() {

                @Override
                public boolean cancel(boolean mayInterruptIfRunning) {
                    boolean r = true;
                    for (Promise<T> f : promises) {
                        r = r & f.cancel(mayInterruptIfRunning);
                    }
                    return r;
                }

                @Override
                public boolean isCancelled() {
                    boolean r = true;
                    for (Promise<T> f : promises) {
                        r = r & f.isCancelled();
                    }
                    return r;
                }

                @Override
                public boolean isDone() {
                    boolean r = true;
                    for (Promise<T> f : promises) {
                        r = r & f.isDone();
                    }
                    return r;
                }

                @Override
                public List<T> get() throws InterruptedException, ExecutionException {
                    waitAllLock.await();
                    List<T> r = new ArrayList<>();
                    for (Promise<T> f : promises) {
                        r.add(f.get());
                    }
                    return r;
                }

                @Override
                public List<T> get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
                    if(!waitAllLock.await(timeout, unit)) {
                      throw new TimeoutException(String.format("Promises didn't redeem in %s %s", timeout, unit));
                    }
                    
                    return get();
                }
            };
            F.Action<Promise<T>> action = new F.Action<Promise<T>>() {

                @Override
                public void invoke(Promise<T> completed) {
                    waitAllLock.countDown();
                    if (waitAllLock.getCount() == 0) {
                        try {
                            result.invoke(result.get());
                        } catch (Exception e) {
                            result.invokeWithException(e);
                        }
                    }
                }
            };
            for (Promise<T> f : promises) {
                f.onRedeem(action);
            }
            if(promises.isEmpty()) {
              result.invoke(Collections.<T>emptyList());
            }
            return result;
        }

        public static <A, B> Promise<F.Tuple<A, B>> wait2(Promise<A> tA, Promise<B> tB) {
            final Promise<F.Tuple<A, B>> result = new Promise<>();
            Promise<List<Object>> t = waitAll(new Promise[]{tA, tB});
            t.onRedeem(new F.Action<Promise<List<Object>>>() {

                @Override
                public void invoke(Promise<List<Object>> completed) {
                    List<Object> values = completed.getOrNull();
                    if(values != null) {
                        result.invoke(new F.Tuple((A) values.get(0), (B) values.get(1)));
                    }
                    else {
                        result.invokeWithException(completed.exception);
                    }
                }
            });
            return result;
        }

        public static <A, B, C> Promise<F.T3<A, B, C>> wait3(Promise<A> tA, Promise<B> tB, Promise<C> tC) {
            final Promise<F.T3<A, B, C>> result = new Promise<>();
            Promise<List<Object>> t = waitAll(new Promise[]{tA, tB, tC});
            t.onRedeem(new F.Action<Promise<List<Object>>>() {

                @Override
                public void invoke(Promise<List<Object>> completed) {
                    List<Object> values = completed.getOrNull();
                    if(values != null) {
                        result.invoke(new F.T3((A) values.get(0), (B) values.get(1), (C) values.get(2)));
                    }
                    else {
                        result.invokeWithException(completed.exception);
                    }
                }
            });
            return result;
        }

        public static <A, B, C, D> Promise<F.T4<A, B, C, D>> wait4(Promise<A> tA, Promise<B> tB, Promise<C> tC, Promise<D> tD) {
            final Promise<F.T4<A, B, C, D>> result = new Promise<>();
            Promise<List<Object>> t = waitAll(new Promise[]{tA, tB, tC, tD});
            t.onRedeem(new F.Action<Promise<List<Object>>>() {

                @Override
                public void invoke(Promise<List<Object>> completed) {
                    List<Object> values = completed.getOrNull();
                    if(values != null) {
                        result.invoke(new F.T4((A) values.get(0), (B) values.get(1), (C) values.get(2), (D) values.get(3)));
                    }
                    else {
                        result.invokeWithException(completed.exception);
                    }
                }
            });
            return result;
        }

        public static <A, B, C, D, E> Promise<F.T5<A, B, C, D, E>> wait5(Promise<A> tA, Promise<B> tB, Promise<C> tC, Promise<D> tD, Promise<E> tE) {
            final Promise<F.T5<A, B, C, D, E>> result = new Promise<>();
            Promise<List<Object>> t = waitAll(new Promise[]{tA, tB, tC, tD, tE});
            t.onRedeem(new F.Action<Promise<List<Object>>>() {

                @Override
                public void invoke(Promise<List<Object>> completed) {
                    List<Object> values = completed.getOrNull();
                    if(values != null) {
                        result.invoke(new F.T5((A) values.get(0), (B) values.get(1), (C) values.get(2), (D) values.get(3), (E) values.get(4)));
                    }
                    else {
                        result.invokeWithException(completed.exception);
                    }
                }
            });
            return result;
        }

        private static Promise<F.Tuple<Integer, Promise<Object>>> waitEitherInternal(Promise<?>... futures) {
            final Promise<F.Tuple<Integer, Promise<Object>>> result = new Promise<>();
            for (int i = 0; i < futures.length; i++) {
                final int index = i + 1;
                ((Promise<Object>) futures[i]).onRedeem(new F.Action<Promise<Object>>() {

                    @Override
                    public void invoke(Promise<Object> completed) {
                        result.invoke(new F.Tuple(index, completed));
                    }
                });
            }
            return result;
        }

        public static <A, B> Promise<F.Either<A, B>> waitEither(Promise<A> tA, Promise<B> tB) {
            final Promise<F.Either<A, B>> result = new Promise<>();
            Promise<F.Tuple<Integer, Promise<Object>>> t = waitEitherInternal(tA, tB);

            t.onRedeem(new F.Action<Promise<F.Tuple<Integer, Promise<Object>>>>() {

                @Override
                public void invoke(Promise<F.Tuple<Integer, Promise<Object>>> completed) {
                    F.Tuple<Integer, Promise<Object>> value = completed.getOrNull();
                    switch (value._1) {
                        case 1:
                            result.invoke(F.Either.<A, B>_1((A) value._2.getOrNull()));
                            break;
                        case 2:
                            result.invoke(F.Either.<A, B>_2((B) value._2.getOrNull()));
                            break;
                    }

                }
            });

            return result;
        }

        public static <A, B, C> Promise<F.E3<A, B, C>> waitEither(Promise<A> tA, Promise<B> tB, Promise<C> tC) {
            final Promise<F.E3<A, B, C>> result = new Promise<>();
            Promise<F.Tuple<Integer, Promise<Object>>> t = waitEitherInternal(tA, tB, tC);

            t.onRedeem(new F.Action<Promise<F.Tuple<Integer, Promise<Object>>>>() {

                @Override
                public void invoke(Promise<F.Tuple<Integer, Promise<Object>>> completed) {
                    F.Tuple<Integer, Promise<Object>> value = completed.getOrNull();
                    switch (value._1) {
                        case 1:
                            result.invoke(F.E3.<A, B, C>_1((A) value._2.getOrNull()));
                            break;
                        case 2:
                            result.invoke(F.E3.<A, B, C>_2((B) value._2.getOrNull()));
                            break;
                        case 3:
                            result.invoke(F.E3.<A, B, C>_3((C) value._2.getOrNull()));
                            break;
                    }

                }
            });

            return result;
        }

        public static <A, B, C, D> Promise<F.E4<A, B, C, D>> waitEither(Promise<A> tA, Promise<B> tB, Promise<C> tC, Promise<D> tD) {
            final Promise<F.E4<A, B, C, D>> result = new Promise<>();
            Promise<F.Tuple<Integer, Promise<Object>>> t = waitEitherInternal(tA, tB, tC, tD);

            t.onRedeem(new F.Action<Promise<F.Tuple<Integer, Promise<Object>>>>() {

                @Override
                public void invoke(Promise<F.Tuple<Integer, Promise<Object>>> completed) {
                    F.Tuple<Integer, Promise<Object>> value = completed.getOrNull();
                    switch (value._1) {
                        case 1:
                            result.invoke(F.E4.<A, B, C, D>_1((A) value._2.getOrNull()));
                            break;
                        case 2:
                            result.invoke(F.E4.<A, B, C, D>_2((B) value._2.getOrNull()));
                            break;
                        case 3:
                            result.invoke(F.E4.<A, B, C, D>_3((C) value._2.getOrNull()));
                            break;
                        case 4:
                            result.invoke(F.E4.<A, B, C, D>_4((D) value._2.getOrNull()));
                            break;
                    }

                }
            });

            return result;
        }

        public static <A, B, C, D, E> Promise<F.E5<A, B, C, D, E>> waitEither(Promise<A> tA, Promise<B> tB, Promise<C> tC, Promise<D> tD, Promise<E> tE) {
            final Promise<F.E5<A, B, C, D, E>> result = new Promise<>();
            Promise<F.Tuple<Integer, Promise<Object>>> t = waitEitherInternal(tA, tB, tC, tD, tE);

            t.onRedeem(new F.Action<Promise<F.Tuple<Integer, Promise<Object>>>>() {

                @Override
                public void invoke(Promise<F.Tuple<Integer, Promise<Object>>> completed) {
                    F.Tuple<Integer, Promise<Object>> value = completed.getOrNull();
                    switch (value._1) {
                        case 1:
                            result.invoke(F.E5.<A, B, C, D, E>_1((A) value._2.getOrNull()));
                            break;
                        case 2:
                            result.invoke(F.E5.<A, B, C, D, E>_2((B) value._2.getOrNull()));
                            break;
                        case 3:
                            result.invoke(F.E5.<A, B, C, D, E>_3((C) value._2.getOrNull()));
                            break;
                        case 4:
                            result.invoke(F.E5.<A, B, C, D, E>_4((D) value._2.getOrNull()));
                            break;
                        case 5:
                            result.invoke(F.E5.<A, B, C, D, E>_5((E) value._2.getOrNull()));
                            break;

                    }

                }
            });

            return result;
        }

        public static <T> Promise<T> waitAny(Promise<T>... futures) {
            final Promise<T> result = new Promise<>();

            F.Action<Promise<T>> action = new F.Action<Promise<T>>() {

                @Override
                public void invoke(Promise<T> completed) {
                    synchronized (this) {
                        if (result.isDone()) {
                            return;
                        }
                    }
                    T resultOrNull = completed.getOrNull();
                    if(resultOrNull != null) {
                      result.invoke(resultOrNull);
                    }
                    else {
                      result.invokeWithException(completed.exception);
                    }
                }
            };

            for (Promise<T> f : futures) {
                f.onRedeem(action);
            }

            return result;
        }
    }

    public interface Action<T> {

        void invoke(T result);
    }

    public abstract static class Option<T> implements Iterable<T> {

        public abstract boolean isDefined();

        public abstract T get();

        public static <T> None<T> None() {
            return (None<T>) (Object) None;
        }

        public static <T> Some<T> Some(T value) {
            return new Some<>(value);
        }
    }

    public static <A> Some<A> Some(A a) {
        return new Some(a);
    }

    public static class None<T> extends Option<T> {

        @Override
        public boolean isDefined() {
            return false;
        }

        @Override
        public T get() {
            throw new IllegalStateException("No value");
        }

        @Override
        public Iterator<T> iterator() {
            return Collections.<T>emptyList().iterator();
        }

        @Override
        public String toString() {
            return "None";
        }
    }
    public static None<Object> None = new None<>();

    public static class Some<T> extends Option<T> {

        final T value;

        public Some(T value) {
            this.value = value;
        }

        @Override
        public boolean isDefined() {
            return true;
        }

        @Override
        public T get() {
            return value;
        }

        @Override
        public Iterator<T> iterator() {
            return Collections.singletonList(value).iterator();
        }

        @Override
        public String toString() {
            return "Some(" + value + ")";
        }
    }

    public static class Either<A, B> {

        public final Option<A> _1;
        public final Option<B> _2;

        private Either(Option<A> _1, Option<B> _2) {
            this._1 = _1;
            this._2 = _2;
        }

        public static <A, B> Either<A, B> _1(A value) {
            return new Either(Some(value), None);
        }

        public static <A, B> Either<A, B> _2(B value) {
            return new Either(None, Some(value));
        }

        @Override
        public String toString() {
            return "E2(_1: " + _1 + ", _2: " + _2 + ")";
        }
    }

    public static class E2<A, B> extends Either<A, B> {

        private E2(Option<A> _1, Option<B> _2) {
            super(_1, _2);
        }
    }

    public static class E3<A, B, C> {

        public final Option<A> _1;
        public final Option<B> _2;
        public final Option<C> _3;

        private E3(Option<A> _1, Option<B> _2, Option<C> _3) {
            this._1 = _1;
            this._2 = _2;
            this._3 = _3;
        }

        public static <A, B, C> E3<A, B, C> _1(A value) {
            return new E3(Some(value), None, None);
        }

        public static <A, B, C> E3<A, B, C> _2(B value) {
            return new E3(None, Some(value), None);
        }

        public static <A, B, C> E3<A, B, C> _3(C value) {
            return new E3(None, None, Some(value));
        }

        @Override
        public String toString() {
            return "E3(_1: " + _1 + ", _2: " + _2 + ", _3:" + _3 + ")";
        }
    }

    public static class E4<A, B, C, D> {

        public final Option<A> _1;
        public final Option<B> _2;
        public final Option<C> _3;
        public final Option<D> _4;

        private E4(Option<A> _1, Option<B> _2, Option<C> _3, Option<D> _4) {
            this._1 = _1;
            this._2 = _2;
            this._3 = _3;
            this._4 = _4;
        }

        public static <A, B, C, D> E4<A, B, C, D> _1(A value) {
            return new E4(Option.Some(value), None, None, None);
        }

        public static <A, B, C, D> E4<A, B, C, D> _2(B value) {
            return new E4(None, Some(value), None, None);
        }

        public static <A, B, C, D> E4<A, B, C, D> _3(C value) {
            return new E4(None, None, Some(value), None);
        }

        public static <A, B, C, D> E4<A, B, C, D> _4(D value) {
            return new E4(None, None, None, Some(value));
        }

        @Override
        public String toString() {
            return "E4(_1: " + _1 + ", _2: " + _2 + ", _3:" + _3 + ", _4:" + _4 + ")";
        }
    }

    public static class E5<A, B, C, D, E> {

        public final Option<A> _1;
        public final Option<B> _2;
        public final Option<C> _3;
        public final Option<D> _4;
        public final Option<E> _5;

        private E5(Option<A> _1, Option<B> _2, Option<C> _3, Option<D> _4, Option<E> _5) {
            this._1 = _1;
            this._2 = _2;
            this._3 = _3;
            this._4 = _4;
            this._5 = _5;
        }

        public static <A, B, C, D, E> E5<A, B, C, D, E> _1(A value) {
            return new E5(Option.Some(value), None, None, None, None);
        }

        public static <A, B, C, D, E> E5<A, B, C, D, E> _2(B value) {
            return new E5(None, Option.Some(value), None, None, None);
        }

        public static <A, B, C, D, E> E5<A, B, C, D, E> _3(C value) {
            return new E5(None, None, Option.Some(value), None, None);
        }

        public static <A, B, C, D, E> E5<A, B, C, D, E> _4(D value) {
            return new E5(None, None, None, Option.Some(value), None);
        }

        public static <A, B, C, D, E> E5<A, B, C, D, E> _5(E value) {
            return new E5(None, None, None, None, Option.Some(value));
        }

        @Override
        public String toString() {
            return "E5(_1: " + _1 + ", _2: " + _2 + ", _3:" + _3 + ", _4:" + _4 + ", _5:" + _5 + ")";
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

    public static <A, B> Tuple<A, B> Tuple(A a, B b) {
        return new Tuple(a, b);
    }

    public static class T2<A, B> extends Tuple<A, B> {

        public T2(A _1, B _2) {
            super(_1, _2);
        }
    }

    public static <A, B> T2<A, B> T2(A a, B b) {
        return new T2(a, b);
    }

    public static class T3<A, B, C> {

        public final A _1;
        public final B _2;
        public final C _3;

        public T3(A _1, B _2, C _3) {
            this._1 = _1;
            this._2 = _2;
            this._3 = _3;
        }

        @Override
        public String toString() {
            return "T3(_1: " + _1 + ", _2: " + _2 + ", _3:" + _3 + ")";
        }
    }

    public static <A, B, C> T3<A, B, C> T3(A a, B b, C c) {
        return new T3(a, b, c);
    }

    public static class T4<A, B, C, D> {

        public final A _1;
        public final B _2;
        public final C _3;
        public final D _4;

        public T4(A _1, B _2, C _3, D _4) {
            this._1 = _1;
            this._2 = _2;
            this._3 = _3;
            this._4 = _4;
        }

        @Override
        public String toString() {
            return "T4(_1: " + _1 + ", _2: " + _2 + ", _3:" + _3 + ", _4:" + _4 + ")";
        }
    }

    public static <A, B, C, D> T4<A, B, C, D> T4(A a, B b, C c, D d) {
        return new T4<>(a, b, c, d);
    }

    public static class T5<A, B, C, D, E> {

        public final A _1;
        public final B _2;
        public final C _3;
        public final D _4;
        public final E _5;

        public T5(A _1, B _2, C _3, D _4, E _5) {
            this._1 = _1;
            this._2 = _2;
            this._3 = _3;
            this._4 = _4;
            this._5 = _5;
        }

        @Override
        public String toString() {
            return "T5(_1: " + _1 + ", _2: " + _2 + ", _3:" + _3 + ", _4:" + _4 + ", _5:" + _5 + ")";
        }
    }

    public static <A, B, C, D, E> T5<A, B, C, D, E> T5(A a, B b, C c, D d, E e) {
        return new T5<>(a, b, c, d, e);
    }

    public abstract static class Matcher<T, R> {

        public abstract Option<R> match(T o);

        public Option<R> match(Option<T> o) {
            if (o.isDefined()) {
                return match(o.get());
            }
            return Option.None();
        }

        public <NR> Matcher<T, NR> and(final Matcher<R, NR> nextMatcher) {
            final Matcher<T, R> firstMatcher = this;
            return new Matcher<T, NR>() {

                @Override
                public Option<NR> match(T o) {
                    for (R r : firstMatcher.match(o)) {
                        return nextMatcher.match(r);
                    }
                    return Option.None();
                }
            };
        }
        public static Matcher<Object, String> String = new Matcher<Object, String>() {

            @Override
            public Option<String> match(Object o) {
                if (o instanceof String) {
                    return Option.Some((String) o);
                }
                return Option.None();
            }
        };

        public static <K> Matcher<Object, K> ClassOf(final Class<K> clazz) {
            return new Matcher<Object, K>() {

                @Override
                public Option<K> match(Object o) {
                    if (o instanceof Option && ((Option) o).isDefined()) {
                        o = ((Option) o).get();
                    }
                    if (clazz.isInstance(o)) {
                        return Option.Some((K) o);
                    }
                    return Option.None();
                }
            };
        }

        public static Matcher<String, String> StartsWith(final String prefix) {
            return new Matcher<String, String>() {

                @Override
                public Option<String> match(String o) {
                    if (o.startsWith(prefix)) {
                        return Option.Some(o);
                    }
                    return Option.None();
                }
            };
        }

        public static Matcher<String, String> Re(final String pattern) {
            return new Matcher<String, String>() {

                @Override
                public Option<String> match(String o) {
                    if (o.matches(pattern)) {
                        return Option.Some(o);
                    }
                    return Option.None();
                }
            };
        }

        public static <X> Matcher<X, X> Equals(final X other) {
            return new Matcher<X, X>() {

                @Override
                public Option<X> match(X o) {
                    if (o.equals(other)) {
                        return Option.Some(o);
                    }
                    return Option.None();
                }
            };
        }
    }
}