package play.data.validation;

import org.junit.jupiter.api.Test;
import play.i18n.Messages;

import java.util.List;
import java.util.Properties;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

public class ErrorTest {
  @Test
  public void composesHumanReadableErrorMessage() {
    Messages.defaults = new Properties();
    Messages.defaults.setProperty("validation.error.invalid", "%s is invalid");
    Messages.defaults.setProperty("validation.error.missingName", "%s is invalid, given: '%s'");
    Messages.defaults.setProperty("validation.error.age", "%s is invalid, given: %s, should be between %s and %s");
    
    assertThat(new Error("user.password", "validation.error.invalid", emptyList()).message())
      .isEqualTo("user.password is invalid");  

    assertThat(new Error("user.name", "validation.error.missingName", List.of("John")).message())
      .isEqualTo("user.name is invalid, given: 'John'");  

    assertThat(new Error("user.age", "validation.error.age", List.of(16, 18, 40)).message())
      .isEqualTo("user.age is invalid, given: 16, should be between 18 and 40");  
  }
}