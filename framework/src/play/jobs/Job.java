package play.jobs;

import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.Invoker;
import play.Invoker.InvocationContext;
import play.Play;
import play.PlayPlugin;
import play.exceptions.JavaExecutionException;
import play.exceptions.PlayException;
import play.exceptions.UnexpectedException;
import play.libs.F;
import play.libs.F.Promise;
import play.libs.SupplierWithException;
import play.libs.Time;

import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A job is an asynchronously executed unit of work
 * 
 * @param <V>
 *            The job result type (if any)
 */
public class Job<V> extends Invoker.Invocation implements Callable<V> {
    private static final Logger logger = LoggerFactory.getLogger(Job.class);

    public static final String invocationType = "Job";

    protected ExecutorService executor;
    protected long lastRun;
    protected boolean wasError;
    protected Throwable lastException;
    boolean runOnce;

    Date nextPlannedExecution;

    @Override
    public InvocationContext getInvocationContext() {
        return new InvocationContext(invocationType, this.getClass().getAnnotations());
    }

    /**
     * Here you do the job
     */
    public void doJob() throws Exception {
    }

    /**
     * Here you do the job and return a result
     * 
     * @return The job result
     */
    public V doJobWithResult() throws Exception {
        doJob();
        return null;
    }

    @Override
    public void execute() {
    }

    /**
     * Start this job now (well ASAP)
     * 
     * @return the job completion
     */
    public Promise<V> now() {
        Promise<V> smartFuture = new Promise<>();
        JobsPlugin.executor.submit(getJobCallingCallable(smartFuture));
        return smartFuture;
    }

    /**
     * Start this job in several seconds
     * 
     * @param delay
     *            time in seconds
     * @return the job completion
     */
    public Promise<V> in(String delay) {
        return in(Time.parseDuration(delay));
    }

    /**
     * Start this job in several seconds
     * 
     * @param seconds
     *            time in seconds
     * @return the job completion
     */
    public Promise<V> in(int seconds) {
        Promise<V> smartFuture = new Promise<>();
        JobsPlugin.executor.schedule(getJobCallingCallable(smartFuture), seconds, TimeUnit.SECONDS);
        return smartFuture;
    }

    private Callable<V> getJobCallingCallable(final Promise<V> smartFuture) {
        return new Callable<V>() {
            @Override
            public V call() throws Exception {
                try {
                    V result = Job.this.call();
                    if (smartFuture != null) {
                        smartFuture.accept(result);
                    }
                    return result;
                } catch (Exception e) {
                    if (smartFuture != null) {
                        smartFuture.invokeWithException(e);
                    }
                    return null;
                }
            }
        };
    }

    /**
     * Run this job every n seconds
     * 
     * @param delay
     *            time in seconds
     */
    public void every(String delay) {
        every(Time.parseDuration(delay));
    }

    /**
     * Run this job every n seconds
     * 
     * @param seconds
     *            time in seconds
     */
    public void every(int seconds) {
        JobsPlugin.executor.scheduleWithFixedDelay(this, seconds, seconds, TimeUnit.SECONDS);
        JobsPlugin.scheduledJobs.add(this);
    }

    // Customize Invocation
    @Override
    public void onException(Throwable e) {
        wasError = true;
        lastException = e;
        try {
            super.onException(e);
        } catch (Throwable ex) {
            logger.error("Error during job execution ({})", this, ex);
            throw new UnexpectedException(unwrap(e));
        }
    }

    private Throwable unwrap(Throwable e) {
        while ((e instanceof UnexpectedException || e instanceof PlayException) && e.getCause() != null) {
            e = e.getCause();
        }
        return e;
    }

    @Override
    public void run() {
        call();
    }

    private V withinFilter(SupplierWithException<V> fct) throws Exception {
        F.Option<PlayPlugin.Filter<V>> filters = Play.pluginCollection.composeFilters();
        if (!filters.isDefined()) {
            return null;
        } else {
            return filters.get().withinFilter(fct);
        }
    }

    @Override
    public V call() {
        Monitor monitor = null;
        try {
            if (init()) {
                before();
                V result;

                try {
                    lastException = null;
                    lastRun = System.currentTimeMillis();
                    monitor = MonitorFactory.start(this + ".doJob()");

                    // If we have a plugin, get him to execute the job within the filter.
                    final AtomicBoolean executed = new AtomicBoolean(false);
                    result = this.withinFilter(() -> {
                        executed.set(true);
                        return doJobWithResult();
                    });

                    // No filter function found => we need to execute anyway( as before the use of withinFilter )
                    if (!executed.get()) {
                        result = doJobWithResult();
                    }

                    monitor.stop();
                    monitor = null;
                    wasError = false;
                } catch (PlayException e) {
                    throw e;
                } catch (Exception e) {
                    StackTraceElement element = PlayException.getInterestingStackTraceElement(e);
                    if (element != null) {
                        throw new JavaExecutionException(element.getLineNumber(), e);
                    }
                    throw e;
                }
                after();
                return result;
            }
        } catch (Throwable e) {
            onException(e);
        } finally {
            if (monitor != null) {
                monitor.stop();
            }
            _finally();
        }
        return null;
    }

    @Override
    public void _finally() {
        super._finally();
        synchronized (this) {
            try {
                if (executor == JobsPlugin.executor && !runOnce) {
                    JobsPlugin.scheduleForCRON(this);
                }
            }
            finally {
                runOnce = false;
            }
        }
    }

    @Override
    public String toString() {
        return this.getClass().getName();
    }

}
