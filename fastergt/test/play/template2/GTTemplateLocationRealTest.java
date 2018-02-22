package play.template2;

import org.junit.Test;

import static org.junit.Assert.*;

public class GTTemplateLocationRealTest {
  @Test
  public void addsTag_inlineScript_aroundEveryJavaScript() {
    String originalHtml = "<html>\n" +
      "<script>\n" +
      " var foo = 42;\n" +
      "</script>\n" +
      "<script>\n" +
      " var bar = 38;\n" +
      "</script>\n" +
      "</html>\n";

    String expectedHtml = "<html>\n" +
      "#{inlineScript}<script>\n" +
      " var foo = 42;\n" +
      "</script>#{/inlineScript}\n" +
      "#{inlineScript}<script>\n" +
      " var bar = 38;\n" +
      "</script>#{/inlineScript}\n" +
      "</html>\n";

    assertEquals(expectedHtml,
      new GTTemplateLocationReal(null, null).addInlineScriptTag(originalHtml));
  }
}