package play.modules.logger;

import play.PlayPlugin;
import play.exceptions.ActionNotFoundException;
import play.exceptions.UnexpectedException;

import javax.persistence.PersistenceException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.Integer.compare;
import static java.lang.String.format;
import static java.util.Collections.sort;
import static org.apache.commons.lang3.StringUtils.split;

public class ExceptionsMonitoringPlugin extends PlayPlugin {

  private static final ConcurrentHashMap<String, AtomicInteger> exceptions = new ConcurrentHashMap<>();

  public static void register(String source, Throwable e) {
    if (e instanceof ActionNotFoundException) return;
    if (e instanceof UnexpectedException ||
      e instanceof InvocationTargetException ||
      e instanceof PersistenceException) {
      if (e.getCause() != null) e = e.getCause();
    }
    register(source, key(e));
  }

  public static void register(String source, String message) {
    String key = "[" + source + "] " + message;
    AtomicInteger value = exceptions.get(key);
    if (value == null) exceptions.put(key, value = new AtomicInteger());
    value.incrementAndGet();
  }

  static String key(Throwable e) {
    return split(e.toString(), '\n')[0]
      .replaceAll("@[0-9a-f]{4,}", "@*")
      .replaceAll("\\{\\{.*\\}\\}", "*")
      .replaceAll("[\\d*]{3,}", "*");
  }

  @Override public String getStatus() {
    StringWriter sw = new StringWriter();
    try (PrintWriter out = new PrintWriter(sw)) {

      out.println("Exception statistics:");
      out.println("~~~~~~~~~~~~~~~~~~~~~~");

      List<Map.Entry<String, AtomicInteger>> sorted = new ArrayList<>(exceptions.entrySet());
      sort(sorted, (o1, o2) -> compare(o2.getValue().get(), o1.getValue().get()));

      for (Map.Entry<String, AtomicInteger> entry : sorted) {
        out.println(format("%4d : %s", entry.getValue().get(), entry.getKey()));
      }
    }

    return sw.toString();
  }

  public static Map<String, AtomicInteger> getExceptions() {
    return exceptions;
  }

  public static void resetExceptions() {
    exceptions.clear();
  }
}
