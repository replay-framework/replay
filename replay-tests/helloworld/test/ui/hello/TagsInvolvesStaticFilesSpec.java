package ui.hello;

import com.codeborne.selenide.Selenide;
import org.junit.jupiter.api.Test;

import static com.codeborne.selenide.Selenide.open;
import static org.assertj.core.api.Assertions.assertThat;

public class TagsInvolvesStaticFilesSpec extends BaseSpec {

  @Test
  public void openEmptyPage() {
    open("/empty");
    assertThat(Selenide.title()).isEqualTo("Empty page");
  }
}
