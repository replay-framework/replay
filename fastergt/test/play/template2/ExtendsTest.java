package play.template2;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class ExtendsTest {

  // when tag uses extends, it should be as if the template calling the template extended it
  // the last one using extends wins
  // same behaviour if a tag calls a tag

  // include and render behaves the same way as tags

  @Test
  public void testExtends() {
    // first try with unix line feeds
    GTTemplateRepo tr =
        new GTTemplateRepoBuilder().withTemplateRootFolder(new TemplateRootFolder()).build();

    TemplateSourceRenderer sr = new TemplateSourceRenderer(tr);

    Map<String, Object> args = new HashMap<>();

    GTJavaBase t = tr.getTemplateInstance(new GTTemplateLocation("folder/subfolder/templateUsingExtendsAndTag.txt"));
    t.renderTemplate(args);
    assertThat(t.getAsString()).isEqualTo("maintemplateUsingExtends2%n[from tag: x]".formatted());

    // test nested extends
    t = tr.getTemplateInstance(new GTTemplateLocation("templateUsingExtendsExtendsAndTag.txt"));
    t.renderTemplate(args);
    assertThat(t.getAsString())
        .isEqualTo("maintemplateUsingExtendsxtemplateUsingExtends3%n[from tag: x]".formatted());

    assertThat(sr.renderSrc("#{tagUsingExtends/}template", args)).isEqualTo("maintag1template");
    assertThat(sr.renderSrc("#{tagUsingTagUsingExtends/}template", args))
        .isEqualTo("maintag1tagxtemplate");
    assertThat(sr.renderSrc("#{render 'templateUsingExtends.txt'/}template", args))
        .isEqualTo("maintemplateUsingExtendstemplate");
    assertThat(sr.renderSrc("#{include 'templateUsingExtends.txt'/}template", args))
        .isEqualTo("maintemplateUsingExtendstemplate");
  }
}
