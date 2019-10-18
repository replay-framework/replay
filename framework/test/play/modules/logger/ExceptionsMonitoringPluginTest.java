package play.modules.logger;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static play.modules.logger.ExceptionsMonitoringPlugin.key;

public class ExceptionsMonitoringPluginTest {

  @Test
  public void keyTakes1stLine() {
    assertEquals("java.lang.IllegalStateException: Hello", key(new IllegalStateException("Hello\nWorld")));
  }

  @Test
  public void keyFromObjectHashcode() {
    assertEquals("java.lang.RuntimeException: while closing c3p0.impl.NewPooledConnection@*",
      key(new RuntimeException("while closing c3p0.impl.NewPooledConnection@7ead1")));

    assertEquals("java.lang.RuntimeException: while closing c3p0.impl.NewPooledConnection@* aaa",
      key(new RuntimeException("while closing c3p0.impl.NewPooledConnection@7ead1 aaa")));

    assertEquals("java.lang.RuntimeException: while closing c3p0.impl.NewPooledConnection@* aaa",
      key(new RuntimeException("while closing c3p0.impl.NewPooledConnection@1234d aaa")));
  }

  @Test
  public void keyWithSpecificNumbers() {
    assertEquals("java.lang.RuntimeException: Card '* - 0' not found or doesn't belong to 'CODEBFIMI'",
      key(new RuntimeException("Card '964301******1706 - 0' not found or doesn't belong to 'CODEBFIMI'")));

    assertEquals("java.lang.RuntimeException: обслуживаемых клиентов. dbo_id=*.",
      key(new RuntimeException("обслуживаемых клиентов. dbo_id=190398235334.")));
  }

  @Test
  public void keyWithSpecificFileName() {
    assertEquals("java.lang.RuntimeException: file * for *",
      key(new RuntimeException("file {{/etc/some} with spaces/file.pdf}} for 12345678")));

    assertEquals("java.lang.RuntimeException: file {{/etc/some with spaces/file.pdf for *",
      key(new RuntimeException("file {{/etc/some with spaces/file.pdf for 12345678")));

    assertEquals("java.lang.RuntimeException: file /etc/some with spaces/file.pdf}} for *",
      key(new RuntimeException("file /etc/some with spaces/file.pdf}} for 12345678")));

    assertEquals("java.lang.RuntimeException: file {/etc/some with spaces/file.pdf} for *",
      key(new RuntimeException("file {/etc/some with spaces/file.pdf} for 12345678")));
  }

}