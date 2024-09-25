package play.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.HashMap;

/** like Properties, but with: encoding generic type helper */
public class Properties extends HashMap<String, String> {

  private static final long serialVersionUID = 1L;

  public synchronized void load(InputStream is) throws IOException {
    load(is, "utf-8");
  }

  public synchronized void load(InputStream is, String encoding) throws IOException {
    if (is == null) {
      throw new NullPointerException("Can't read from null stream");
    }
    try (BufferedReader rd = new BufferedReader(new InputStreamReader(is, encoding))) {
      while (true) {
        String tmp = rd.readLine();
        if (tmp == null) {
          break;
        }
        tmp = tmp.trim();

        if (tmp.startsWith("#")) {
          continue;
        }
        if (!tmp.contains("=")) {
          put(tmp, "");
          continue;
        }

        String[] kv = tmp.split("=", 2);
        if (kv.length == 2) {
          put(kv[0], kv[1]);
        } else {
          put(kv[0], "");
        }
      }
    }
  }

  public String get(String key, String defaultValue) {
    if (containsKey(key)) {
      return get(key);
    } else {
      return defaultValue;
    }
  }

  public synchronized void store(OutputStream out) throws IOException {
    store(out, "utf-8");
  }

  public synchronized void store(OutputStream out, String encoding) throws IOException {
    if (out == null) {
      throw new NullPointerException("Can't store to null stream");
    }
    BufferedWriter wr = new BufferedWriter(new OutputStreamWriter(out, encoding));
    for (String key : keySet()) {
      if (key.length() > 0) {
        wr.write(key + "=" + get(key) + System.getProperties().getProperty("line.separator"));
      }
    }
    wr.flush();
    wr.close();
  }

  public double getDouble(String key) throws IllegalArgumentException {
    String s = get(key);
    try {
      return Double.parseDouble(s);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Property must be an double value :" + key, e);
    }
  }

  public double getDouble(String key, long defaultValue) throws IllegalArgumentException {
    String s = get(key);
    if (s == null) {
      return defaultValue;
    }
    try {
      return Double.parseDouble(s);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Property must be an double value :" + key, e);
    }
  }

  public void setDouble(String key, double val) {
    put(key, Double.toString(val));
  }

  public int getInt(String key) throws IllegalArgumentException {
    String s = get(key);
    try {
      return Integer.parseInt(s);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Property must be an integer value :" + key, e);
    }
  }

  public int getInt(String key, int defaultValue) throws IllegalArgumentException {
    String s = get(key);
    if (s == null) {
      return defaultValue;
    }
    try {
      return Integer.parseInt(s);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Property must be an integer value :" + key, e);
    }
  }

  public void setInt(String key, int val) {
    put(key, Integer.toString(val));
  }

  public void setLong(String key, long val) {
    put(key, Long.toString(val));
  }
}
