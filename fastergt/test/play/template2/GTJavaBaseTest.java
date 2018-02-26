package play.template2;

import org.junit.Test;
import play.i18n.Messages;
import play.template2.exceptions.GTTemplateRuntimeException;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class GTJavaBaseTest {
  @Test
  public void handleMessageTag_withZeroArguments() {
    GTJavaBase template = new TestTemplate("/app/views/home.html") {};

    assertThatThrownBy(() -> template.handleMessageTag(emptyList()))
      .isInstanceOf(GTTemplateRuntimeException.class)
      .hasMessage("It looks like you don't have anything in your Message tag");
  }

  @Test
  public void handleMessageTag_withOneArgument() {
    Messages.defaults.setProperty("payment.status", "Payment status");
    GTJavaBase template = new TestTemplate("/app/views/home.html") {};

    assertThat(template.handleMessageTag(asList("payment.status"))).isEqualTo("Payment status");
  }

  @Test
  public void handleMessageTag_escapesArguments() {
    Messages.defaults.setProperty("payment.status.unknown", "Unknown status: %s");
    GTJavaBase template = new TestTemplate("/app/views/home.html") {};

    assertThat(template.handleMessageTag(asList("payment.status.unknown", "<script>alert('angry hack')</script>")))
      .isEqualTo("Unknown status: &lt;script&gt;alert('angry hack')&lt;/script&gt;");
  }
}