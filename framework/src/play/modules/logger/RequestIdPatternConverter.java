package play.modules.logger;

import org.apache.log4j.helpers.PatternConverter;
import org.apache.log4j.spi.LoggingEvent;
import org.slf4j.MDC;
import play.mvc.Http;

class RequestIdPatternConverter extends PatternConverter {

  @Override protected String convert(LoggingEvent event) {
    Http.Request request = Http.Request.current();
    Object rid = request != null ? request.args.get("requestId") : MDC.get("requestId");
    return rid == null ? currentThreadName() : rid.toString();
  }

  protected String currentThreadName() {
    return Thread.currentThread().getName();
  }
}
