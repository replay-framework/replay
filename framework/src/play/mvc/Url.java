package play.mvc;

import play.Play;

import javax.annotation.Nullable;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.joining;
import static org.apache.commons.lang3.StringUtils.isBlank;

public class Url {
  private final String url;

  public Url(String url) {
    this.url = url;
  }

  public Url(String urlDomainPath, String paramName, @Nullable Object paramValue) {
    this(build(urlDomainPath, paramName, paramValue));
  }

  public Url(String urlDomainPath,
             String param1Name, @Nullable Object param1value,
             String param2name, @Nullable Object param2value) {
    this(build(urlDomainPath,
      param1Name, param1value,
      param2name, param2value));
  }

  public Url(String urlDomainPath,
             String param1Name, @Nullable Object param1value,
             String param2name, @Nullable Object param2value,
             String param3name, @Nullable Object param3value) {
    this(build(urlDomainPath,
      param1Name, param1value,
      param2name, param2value,
      param3name, param3value));
  }

  public Url(String urlDomainPath,
             String param1Name, @Nullable Object param1value,
             String param2name, @Nullable Object param2value,
             String param3name, @Nullable Object param3value,
             String param4name, @Nullable Object param4value) {
    this(build(urlDomainPath,
      param1Name, param1value,
      param2name, param2value,
      param3name, param3value,
      param4name, param4value));
  }

  public Url(String urlDomainPath,
             String param1Name, @Nullable Object param1value,
             String param2name, @Nullable Object param2value,
             String param3name, @Nullable Object param3value,
             String param4name, @Nullable Object param4value,
             String param5name, @Nullable Object param5value) {
    this(build(urlDomainPath,
      param1Name, param1value,
      param2name, param2value,
      param3name, param3value,
      param4name, param4value,
      param5name, param5value));
  }

  public Url(String url, Map<String, Object> parameters) {
    this(build(url, parameters));
  }

  private static String build(String url, Map<String, Object> parameters) {
    if (parameters.entrySet().stream().anyMatch(e -> isBlank(e.getKey()))) {
      throw new IllegalArgumentException("Paramater name can not me Blank");
    }
    String parametersPart = parameters.entrySet().stream()
      .filter(e -> e.getValue() != null)
      .map(e -> encode(e.getKey()) + "=" + encode(paramToString(e.getValue())))
      .collect(joining("&"));
    String separator = parametersPart.isEmpty() ? "" : url.contains("?") ? "&" : "?";
    return url + separator + parametersPart;
  }

  private static String build(String url, Object... args) {
    Map<String, Object> parameters = new LinkedHashMap<>(args.length / 2);
    for (int i = 0; i < args.length - 1; i += 2) {
      parameters.put((String) args[i], args[i + 1]);
    }
    return build(url, parameters);
  }

  private static String encode(String parameter) {
    return parameter == null ? "" : URLEncoder.encode(parameter, UTF_8);
  }

  private static SimpleDateFormat getDateFormat() {
    return new SimpleDateFormat(Play.configuration.getProperty("date.format", "dd.MM.yyyy"));
  }

  private static String paramToString(Object value) {
    if (value instanceof Date) {
      return getDateFormat().format((Date) value);
    }
    return String.valueOf(value);
  }

  @Override public String toString() {
    return url;
  }

  @Override public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Url url1 = (Url) o;
    return url.equals(url1.url);
  }

  @Override public int hashCode() {
    return url.hashCode();
  }
}

