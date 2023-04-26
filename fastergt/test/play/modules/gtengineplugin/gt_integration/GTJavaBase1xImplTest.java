package play.modules.gtengineplugin.gt_integration;

import org.junit.jupiter.api.Test;
import play.template2.GTJavaBase;
import play.template2.TestTemplate;
import play.templates.BaseTemplate.RawData;

import static org.assertj.core.api.Assertions.assertThat;

public class GTJavaBase1xImplTest {
  @Test
  public void escapes$ValuesForHtml() {
    GTJavaBase template = new TestTemplate("/app/views/home.html");

    assertThat(template.objectToString("foo&bar<a>'</a>")).isEqualTo("foo&amp;bar&lt;a&gt;'&lt;/a&gt;");
  }

  @Test
  public void escapes$ValuesForScript() {
    GTJavaBase template = new TestTemplate("/app/views/home.html");
    template.binding.setProperty("__inside_script_tag", "true");

    assertThat(template.objectToString("foo&-alert('ups')-bar")).isEqualTo("foo&-alert(\\'ups\\')-bar");
  }

  @Test
  public void doesNotEscapeCyrillicCharactersForScript() {
    GTJavaBase template = new TestTemplate("/app/views/home.html");
    template.binding.setProperty("__inside_script_tag", "true");

    assertThat(template.objectToString("русские буквы")).isEqualTo("русские буквы");
  }

  @Test
  public void escapes$ValuesForXml() {
    GTJavaBase template = new TestTemplate("/app/views/home.xml");

    assertThat(template.objectToString("foo&bar<a>'</a>")).isEqualTo("foo&amp;bar&lt;a&gt;&apos;&lt;/a&gt;");
  }

  @Test
  public void escapes$ValuesForCsv() {
    GTJavaBase template = new TestTemplate("/app/views/home.csv");

    assertThat(template.objectToString("foo&bar<a>'</a>,\t;")).isEqualTo("\"foo&bar<a>'</a>,\t;\"");
  }

  @Test
  public void doesNotEscapeRawData() {
    GTJavaBase template = new TestTemplate("/app/views/home.html");

    assertThat(template.objectToString(new RawData("this is a way <script>alert('to hell')</script>")))
      .isEqualTo("this is a way <script>alert('to hell')</script>");
  }
}