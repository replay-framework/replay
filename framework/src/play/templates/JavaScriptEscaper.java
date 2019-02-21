package play.templates;

import org.apache.commons.lang.UnhandledException;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

import static org.apache.commons.lang.StringUtils.isEmpty;

public class JavaScriptEscaper {
  public static String escape(String str) {
      if (isEmpty(str)) {
          return str;
      }
      try {
          StringWriter writer = new StringWriter(str.length() * 2);
          escapeJavaStyleString(writer, str);
          return writer.toString();
      } catch (IOException ioe) {
          throw new UnhandledException(ioe);
      }
  }

  private static void escapeJavaStyleString(Writer out, String str) throws IOException {
      if (out == null) {
          throw new IllegalArgumentException("The Writer must not be null");
      }
      if (str == null) {
          return;
      }
    for (int i = 0; i < str.length(); i++) {
          char ch = str.charAt(i);

          if (ch > 0x7f) {
              out.write(ch);
          } else if (ch < 32) {
              switch (ch) {
                  case '\b' :
                      out.write('\\');
                      out.write('b');
                      break;
                  case '\n' :
                      out.write('\\');
                      out.write('n');
                      break;
                  case '\t' :
                      out.write('\\');
                      out.write('t');
                      break;
                  case '\f' :
                      out.write('\\');
                      out.write('f');
                      break;
                  case '\r' :
                      out.write('\\');
                      out.write('r');
                      break;
                  default :
                      if (ch > 0xf) {
                          out.write("\\u00" + hex(ch));
                      } else {
                          out.write("\\u000" + hex(ch));
                      }
                      break;
              }
          } else {
              switch (ch) {
                  case '\'' :
                      out.write('\\');
                      out.write('\'');
                      break;
                  case '"' :
                      out.write('\\');
                      out.write('"');
                      break;
                  case '\\' :
                      out.write('\\');
                      out.write('\\');
                      break;
                  case '/' :
                      out.write('\\');
                      out.write('/');
                      break;
                  default :
                      out.write(ch);
                      break;
              }
          }
      }
  }

  private static String hex(char ch) {
      return Integer.toHexString(ch).toUpperCase();
  }
}
