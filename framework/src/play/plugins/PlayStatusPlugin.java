package play.plugins;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.annotation.ParametersAreNonnullByDefault;
import org.apache.commons.lang3.StringUtils;
import play.Invoker;
import play.Play;
import play.PlayPlugin;
import play.mvc.Http.Header;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.mvc.Scope.Flash;
import play.mvc.Scope.RenderArgs;
import play.mvc.Scope.Session;

@ParametersAreNonnullByDefault
public class PlayStatusPlugin extends PlayPlugin {
  /**
   * Get the application status
   *
   * @param json true if the status should be return in JSON
   * @return application status
   */
  public String computeApplicationStatus(boolean json) {
    if (json) {
      JsonObject o = new JsonObject();
      Play.pluginCollection
          .getEnabledPlugins()
          .forEach(
              plugin -> {
                try {
                  JsonObject status = plugin.getJsonStatus();
                  if (status != null) {
                    o.add(plugin.getClass().getName(), status);
                  }
                } catch (Throwable e) {
                  JsonObject error = new JsonObject();
                  error.add("error", new JsonPrimitive(e.getMessage()));
                  o.add(plugin.getClass().getName(), error);
                }
              });
      return o.toString();
    }
    StringBuilder dump = new StringBuilder(16);
    Play.pluginCollection
        .getEnabledPlugins()
        .forEach(
            plugin -> {
              try {
                String status = plugin.getStatus();
                if (status != null) {
                  dump.append(status);
                  dump.append("\n");
                }
              } catch (Throwable e) {
                dump.append(plugin.getClass().getName())
                    .append(".getStatus() has failed (")
                    .append(e.getMessage())
                    .append(")");
              }
            });
    return dump.toString();
  }

  /**
   * Intercept /@status and check that the Authorization header is valid. Then ask each plugin for a
   * status dump and send it over the HTTP response.
   *
   * <p>You can ask the /@status using the authorization header and putting your status secret key
   * in it. Prior to that you would be required to start play with a {@code -DstatusKey=yourkey}
   */
  @Override
  public boolean rawInvocation(
      Request request, Response response, Session session, RenderArgs renderArgs, Flash flash)
      throws Exception {
    if (request.path.equals("/@status") || request.path.equals("/@status.json")) {
      if (!Play.started) {
        response.print("Application is not started");
        response.status = 503;
        return true;
      }
      response.contentType = request.path.contains(".json") ? "application/json" : "text/plain";
      Header authorization = request.headers.get("authorization");
      String statusKey =
          Play.configuration.getProperty("application.statusKey", System.getProperty("statusKey"));
      if (authorization != null && statusKey != null && statusKey.equals(authorization.value())) {
        response.print(computeApplicationStatus(request.path.contains(".json")));
        response.status = 200;
        return true;
      }
      response.status = 401;
      if (response.contentType.equals("application/json")) {
        response.print("{\"error\": \"Not authorized\"}");
      } else {
        response.print("Not authorized");
      }
      return true;
    }
    return super.rawInvocation(request, response, session, renderArgs, flash);
  }

  /** Retrieve status about play core. */
  @Override
  public String getStatus() {
    StringWriter sw = new StringWriter();
    PrintWriter out = new PrintWriter(sw);
    out.println("Java:");
    out.println("~~~~~");
    out.println("Version: " + System.getProperty("java.version"));
    out.println("Home: " + System.getProperty("java.home"));
    out.println("Max memory: " + Runtime.getRuntime().maxMemory());
    out.println("Free memory: " + Runtime.getRuntime().freeMemory());
    out.println("Total memory: " + Runtime.getRuntime().totalMemory());
    out.println("Available processors: " + Runtime.getRuntime().availableProcessors());
    out.println();
    out.println("Play framework:");
    out.println("~~~~~~~~~~~~~~~");
    out.println("ID: " + (StringUtils.isEmpty(Play.id) ? "(not set)" : Play.id));
    out.println("Mode: " + Play.mode);
    out.println("Tmp dir: " + (Play.tmpDir == null ? "(no tmp dir)" : Play.tmpDir));
    out.println();
    out.println("Application:");
    out.println("~~~~~~~~~~~~");
    out.println("Path: " + Play.appRoot);
    out.println("Name: " + Play.configuration.getProperty("application.name", "(not set)"));
    out.println(
        "Started at: "
            + (Play.started
                ? new SimpleDateFormat("MM/dd/yyyy HH:mm").format(new Date(Play.startedAt))
                : "Not yet started"));
    out.println();
    out.println("Loaded plugins:");
    out.println("~~~~~~~~~~~~~~");
    Play.pluginCollection
        .getAllPlugins()
        .forEach(
            plugin ->
                out.println(
                    plugin.index
                        + ":"
                        + plugin.getClass().getName()
                        + " ["
                        + (plugin.isEnabled() ? "enabled" : "disabled")
                        + "]"));
    out.println();
    out.println("Threads:");
    out.println("~~~~~~~~");
    try {
      visit(out, getRootThread(), 0);
    } catch (Throwable e) {
      out.println("Oops; " + e.getMessage());
    }
    out.println();
    out.println("Requests execution pool:");
    out.println("~~~~~~~~~~~~~~~~~~~~~~~~");
    out.println("Pool size: " + Invoker.executor.getPoolSize());
    out.println("Active count: " + Invoker.executor.getActiveCount());
    out.println("Scheduled task count: " + Invoker.executor.getTaskCount());
    out.println("Queue size: " + Invoker.executor.getQueue().size());
    out.println();
    return sw.toString();
  }

  @Override
  public JsonObject getJsonStatus() {
    JsonObject status = new JsonObject();

    {
      JsonObject java = new JsonObject();
      java.addProperty("version", System.getProperty("java.version"));
      status.add("java", java);
    }

    {
      JsonObject memory = new JsonObject();
      memory.addProperty("max", Runtime.getRuntime().maxMemory());
      memory.addProperty("free", Runtime.getRuntime().freeMemory());
      memory.addProperty("total", Runtime.getRuntime().totalMemory());
      status.add("memory", memory);
    }

    {
      JsonObject application = new JsonObject();
      application.addProperty(
          "uptime", Play.started ? System.currentTimeMillis() - Play.startedAt : -1);
      application.addProperty("path", Play.appRoot.getAbsolutePath());
      status.add("application", application);
    }

    {
      JsonObject pool = new JsonObject();
      pool.addProperty("size", Invoker.executor.getPoolSize());
      pool.addProperty("active", Invoker.executor.getActiveCount());
      pool.addProperty("scheduled", Invoker.executor.getTaskCount());
      pool.addProperty("queue", Invoker.executor.getQueue().size());
      status.add("pool", pool);
    }

    return status;
  }

  /** Recursively visit all JVM threads */
  private void visit(PrintWriter out, ThreadGroup group, int level) {
    // Get threads in 'group'
    int numThreads = group.activeCount();
    Thread[] threads = new Thread[numThreads * 2];
    numThreads = group.enumerate(threads, false);

    // Enumerate each thread in "group"
    for (int i = 0; i < numThreads; i++) {
      // Get thread
      Thread thread = threads[i];
      out.println(thread + " " + thread.getState());
    }

    // Get thread subgroups of "group"
    int numGroups = group.activeGroupCount();
    ThreadGroup[] groups = new ThreadGroup[numGroups * 2];
    numGroups = group.enumerate(groups, false);

    // Recursively visit each subgroup
    for (int i = 0; i < numGroups; i++) {
      visit(out, groups[i], level + 1);
    }
  }

  /** Retrieve the JVM root thread group. */
  private ThreadGroup getRootThread() {
    ThreadGroup root = Thread.currentThread().getThreadGroup().getParent();
    while (root.getParent() != null) {
      root = root.getParent();
    }
    return root;
  }
}
