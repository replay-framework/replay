package org.apache.log4j;

import org.apache.log4j.helpers.ThreadLocalMap;

/**
 * A fix for log4j 1.x problem with MDC and Java 9.
 *
 * In case of Java 9, log4j parses "java.version" system property incorrectly.
 * As a result, it doesn't initialize `MDC.mdc.tlm` and `MDC.mdc.java1`, and any calls to `MDC.put` do not take effect.
 */
public class Log4jJava9Support {
  public static void initMDC() {
    if (MDC.mdc.tlm == null) {
      MDC.mdc.java1 = false;
      MDC.mdc.tlm = new ThreadLocalMap();
    }
  }
}