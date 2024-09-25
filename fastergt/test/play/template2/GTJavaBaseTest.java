package play.template2;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Iterator;
import org.junit.jupiter.api.Test;
import play.i18n.Messages;
import play.template2.exceptions.GTTemplateRuntimeException;

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

    assertThat(
            template.handleMessageTag(
                asList("payment.status.unknown", "<script>alert('angry hack')</script>")))
        .isEqualTo("Unknown status: &lt;script&gt;alert('angry hack')&lt;/script&gt;");
  }

  @Test
  public void convertToIterator_array() {
    GTJavaBase template = new TestTemplate("/app/views/home.html") {};

    Iterator it = template.convertToIterator(new int[] {1, 2, 3});
    assertThat(it.next()).isEqualTo(1);
    assertThat(it.next()).isEqualTo(2);
    assertThat(it.next()).isEqualTo(3);
    assertThat(it.hasNext()).isFalse();
  }

  @Test
  public void convertToIterator_enum() {
    GTJavaBase template = new TestTemplate("/app/views/home.html") {};

    Iterator it = template.convertToIterator(Gender.class);
    assertThat(it.next()).isEqualTo(Gender.MALE);
    assertThat(it.next()).isEqualTo(Gender.FEMALE);
    assertThat(it.hasNext()).isFalse();
  }

  private enum Gender {
    MALE,
    FEMALE
  }
}
