package play.jobs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.Play;
import play.PlayPlugin;
import play.exceptions.PlayException;
import play.exceptions.UnexpectedException;
import play.inject.Injector;
import play.libs.CronExpression;
import play.libs.Expression;
import play.libs.Time;
import play.utils.PThreadFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.*;

public class JobsPlugin extends PlayPlugin {
    private static final Logger logger = LoggerFactory.getLogger(JobsPlugin.class);

    public static ScheduledThreadPoolExecutor executor;
    public static List<Job<?>> scheduledJobs = new ArrayList<>();

    @Override
    public String getStatus() {
        StringWriter sw = new StringWriter();
        PrintWriter out = new PrintWriter(sw);
        if (executor == null) {
            out.println("Jobs execution pool:");
            out.println("~~~~~~~~~~~~~~~~~~~");
            out.println("(not yet started)");
            return sw.toString();
        }
        out.println("Jobs execution pool:");
        out.println("~~~~~~~~~~~~~~~~~~~");
        out.println("Pool size: " + executor.getPoolSize());
        out.println("Active count: " + executor.getActiveCount());
        out.println("Scheduled task count: " + executor.getTaskCount());
        out.println("Queue size: " + executor.getQueue().size());
        SimpleDateFormat df = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
        if (!scheduledJobs.isEmpty()) {
            out.println();
            out.println("Scheduled jobs (" + scheduledJobs.size() + "):");
            out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~");
            for (Job<?> job : scheduledJobs) {
                out.print(job);
                if (job.getClass().isAnnotationPresent(OnApplicationStart.class)
                        && !(job.getClass().isAnnotationPresent(On.class) || job.getClass().isAnnotationPresent(Every.class))) {
                    OnApplicationStart appStartAnnotation = job.getClass().getAnnotation(OnApplicationStart.class);
                    out.print(" run at application start" + (appStartAnnotation.async() ? " (async)" : "") + ".");
                }

                if (job.getClass().isAnnotationPresent(On.class)) {

                    String cron = job.getClass().getAnnotation(On.class).value();
                    if (cron.startsWith("cron.")) {
                        cron = requireNotNull(Play.configuration.getProperty(cron), cron, job.getClass(), On.class);
                    }
                    out.print(" run with cron expression " + cron + ".");
                }
                if (job.getClass().isAnnotationPresent(Every.class)) {
                    out.print(" run every " + job.getClass().getAnnotation(Every.class).value() + ".");
                }
                if (job.lastRun > 0) {
                    out.print(" (last run at " + df.format(new Date(job.lastRun)));
                    if (job.wasError) {
                        out.print(" with error)");
                    } else {
                        out.print(")");
                    }
                } else {
                    out.print(" (has never run)");
                }
                out.println();
            }
        }
        if (!executor.getQueue().isEmpty()) {
            out.println();
            out.println("Waiting jobs:");
            out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~");
            ScheduledFuture[] q = executor.getQueue().toArray(new ScheduledFuture[executor.getQueue().size()]);

            for (ScheduledFuture task : q) {
                out.println(extractUnderlyingCallable((FutureTask<?>) task) + " will run in " + task.getDelay(TimeUnit.SECONDS)
                        + " seconds");
            }
        }
        return sw.toString();
    }

    @Override
    public void afterApplicationStart() {
        for (Class<?> clazz : Play.classes.getAssignableClasses(Job.class)) {
            // @OnApplicationStart
            if (clazz.isAnnotationPresent(OnApplicationStart.class)) {
                // check if we're going to run the job sync or async
                OnApplicationStart appStartAnnotation = clazz.getAnnotation(OnApplicationStart.class);
                if (!appStartAnnotation.async()) {
                    // run job sync
                    try {
                        Job<?> job = createJob(clazz);
                        job.run();
                        if (job.wasError) {
                            if (job.lastException != null) {
                                throw job.lastException;
                            }
                            throw new RuntimeException("@OnApplicationStart Job has failed");
                        }
                    } catch (InstantiationException | IllegalAccessException e) {
                        throw new UnexpectedException("Job could not be instantiated", e);
                    } catch (PlayException ex) {
                        throw ex;
                    } catch (Throwable ex) {
                        throw new UnexpectedException(ex);
                    }
                } else {
                    // run job async
                    Job<?> job = createJob(clazz);
                    // start running job now in the background
                    @SuppressWarnings("unchecked")
                    Callable<Job> callable = (Callable<Job>) job;
                    executor.submit(callable);
                }
            }

            // @On
            if (clazz.isAnnotationPresent(On.class)) {
                Job<?> job = createJob(clazz);
                scheduleForCRON(job);
            }
            // @Every
            if (clazz.isAnnotationPresent(Every.class)) {
                Job job = createJob(clazz);
                String value = clazz.getAnnotation(Every.class).value();
                if (value.startsWith("cron.")) {
                    value = requireNotNull(Play.configuration.getProperty(value), value, clazz, Every.class);
                }
                value = Expression.evaluate(value, value).toString();
                if (!"never".equalsIgnoreCase(value)) {
                    executor.scheduleWithFixedDelay(job, Time.parseDuration(value), Time.parseDuration(value), TimeUnit.SECONDS);
                }
            }
        }
    }

    private Job<?> createJob(Class<?> clazz) {
        Job<?> job = (Job<?>) Injector.getBeanOfType(clazz);
        if (!job.getClass().equals(clazz)) {
            throw new RuntimeException("Enhanced job are not allowed: " + clazz.getName() + " vs. " + job.getClass().getName());
        }
        scheduledJobs.add(job);
        return job;
    }

    @Override
    public void onApplicationStart() {
        int core = Integer.parseInt(Play.configuration.getProperty("play.jobs.pool", "10"));
        executor = new ScheduledThreadPoolExecutor(core, new PThreadFactory("jobs"), new ThreadPoolExecutor.AbortPolicy());
        scheduledJobs.clear();
    }

    public static <V> void scheduleForCRON(Job<V> job) {
        if (!job.getClass().isAnnotationPresent(On.class)) {
            return;
        }
        String cron = job.getClass().getAnnotation(On.class).value();
        if (cron.startsWith("cron.")) {
            cron = requireNotNull(Play.configuration.getProperty(cron), cron, job.getClass(), On.class);
        }
        cron = Expression.evaluate(cron, cron).toString();
        if (cron == null || cron.isEmpty() || "never".equalsIgnoreCase(cron)) {
            logger.info("Skipping job {}, cron expression is not defined", job.getClass().getName());
            return;
        }
        try {
            Date now = new Date();
            cron = Expression.evaluate(cron, cron).toString();
            CronExpression cronExp = new CronExpression(cron);
            Date nextDate = cronExp.getNextValidTimeAfter(now);
            if (nextDate == null) {
                logger.warn("The cron expression for job {} doesn't have any match in the future, will never be executed",
                        job.getClass().getName());
                return;
            }
            if (nextDate.equals(job.nextPlannedExecution)) {
                // Bug #13: avoid running the job twice for the same time
                // (happens when we end up running the job a few minutes before
                // the planned time)
                Date nextInvalid = cronExp.getNextInvalidTimeAfter(nextDate);
                nextDate = cronExp.getNextValidTimeAfter(nextInvalid);
            }
            job.nextPlannedExecution = nextDate;
            executor.schedule((Callable<V>) job, nextDate.getTime() - now.getTime(), TimeUnit.MILLISECONDS);
            job.executor = executor;
        } catch (Exception ex) {
            throw new UnexpectedException(ex);
        }
    }

    @Override
    public void onApplicationStop() {

        List<Class<? extends Job>> jobs = Play.classes.getAssignableClasses(Job.class);

        for (Class<? extends Job> clazz : jobs) {
            // @OnApplicationStop
            if (clazz.isAnnotationPresent(OnApplicationStop.class)) {
                try {
                    Job<?> job = createJob(clazz);
                    job.run();
                    if (job.wasError) {
                        if (job.lastException != null) {
                            throw job.lastException;
                        }
                        throw new RuntimeException("@OnApplicationStop Job has failed");
                    }
                } catch (InstantiationException | IllegalAccessException e) {
                    throw new UnexpectedException("Job could not be instantiated", e);
                } catch (PlayException ex) {
                    throw ex;
                } catch (Throwable ex) {
                    throw new UnexpectedException(ex);
                }
            }
        }

        executor.shutdownNow();
        executor.getQueue().clear();
    }

    /**
     * Try to discover what is hidden under a FutureTask (hack)
     * <p>
     * Field sync first, if not present will try field callable
     * </p>
     *
     * @param futureTask
     *            The given tack
     * @return Field sync first, if not present will try field callable
     */
    public Object extractUnderlyingCallable(FutureTask<?> futureTask) {
        try {
            Object callable = null;
            // Try to search for the Field sync first, if not present will try field callable
            try {
                Field syncField = FutureTask.class.getDeclaredField("sync");
                syncField.setAccessible(true);
                Object sync = syncField.get(futureTask);
                if (sync != null) {
                    Field callableField = sync.getClass().getDeclaredField("callable");
                    callableField.setAccessible(true);
                    callable = callableField.get(sync);
                }
            } catch (NoSuchFieldException ex) {
                Field callableField = FutureTask.class.getDeclaredField("callable");
                callableField.setAccessible(true);
                callable = callableField.get(futureTask);
            }
            if (callable != null && callable.getClass().getSimpleName().equals("RunnableAdapter")) {
                Field taskField = callable.getClass().getDeclaredField("task");
                taskField.setAccessible(true);
                return taskField.get(callable);
            }
            return callable;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Future<?> runScheduledJobOnceNow(Job<?> job) {
        job.runOnce = true;
        return executor.submit((Callable<?>) job);
    }

    static String requireNotNull(String value, String cronSettingName, Class<?> clazz, Class<?> annotation) {
        if (value == null) {
            throw new IllegalArgumentException(String.format("Misconfigured setting '%s' in class '%s' annotation '@%s'",
              cronSettingName, clazz.getName(), annotation.getSimpleName()));
        }
        return value;
    }
}
