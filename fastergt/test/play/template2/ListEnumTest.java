package play.template2;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class ListEnumTest {

  private enum MyEnum {
    A,
    B,
    C
  };

  @Test
  public void testListingEnums() {
    TemplateSourceRenderer r = new TemplateSourceRenderer(new GTTemplateRepoBuilder().build());

    Map<String, Object> args = new HashMap<>();

    assertThat(r.renderSrc("#{list play.template2.ListEnumTest.MyEnum, as: 'e'}${e}#{/list}", args))
        .isEqualTo("ABC");
  }
}
