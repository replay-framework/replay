package play.template2;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class GTTemplateLocationRealTest {
  @Test
  public void embedsAllFoundScripts() {
    String originalHtml =
      "<script>42</script>\n" +
      "<script>38</script>\n";

    String expectedHtml =
      "<script>#{secureInlineJavaScript}42#{/secureInlineJavaScript}</script>\n" +
      "<script>#{secureInlineJavaScript}38#{/secureInlineJavaScript}</script>\n";

    assertThat(new GTTemplateLocationReal(null, null).addInlineScriptTag(originalHtml)).isEqualTo(expectedHtml);
  }

  @Test
  public void scriptTagIsCaseInsensitive() {
    String originalHtml =
      "<SCRIPT>42</SCRIPT>\n" +
      "<sCriPt>38</ScRIpT>\n";

    String expectedHtml =
      "<script>#{secureInlineJavaScript}42#{/secureInlineJavaScript}</script>\n" +
      "<script>#{secureInlineJavaScript}38#{/secureInlineJavaScript}</script>\n";

    assertThat(new GTTemplateLocationReal(null, null).addInlineScriptTag(originalHtml)).isEqualTo(expectedHtml);
  }

  @Test
  public void scriptTagsCanHaveAttributes() {
    String originalHtml =
      "<script id=\"some-id\">42</script>\n" +
      "<script type=\"application/javascript\">38</script>\n";

    String expectedHtml =
      "<script id=\"some-id\">#{secureInlineJavaScript}42#{/secureInlineJavaScript}</script>\n" +
      "<script type=\"application/javascript\">#{secureInlineJavaScript}38#{/secureInlineJavaScript}</script>\n";

    assertThat(new GTTemplateLocationReal(null, null).addInlineScriptTag(originalHtml)).isEqualTo(expectedHtml);
  }

  @Test
  public void addsTag_secureInlineJavaScript_aroundEveryJavaScript() {
    String originalHtml = "<html>\n" +
      "<script>\n" +
      " var foo = 42;\n" +
      "</script>\n" +
      "<script id=\"some-id\" type=\"application/javascript\">\n" +
      " var bar = 38;\n" +
      "</script>\n" +
      "<script src=\"/public/javascripts/actual-documents.js\" ></script>\n" +
      "</html>\n";

    String expectedHtml = "<html>\n" +
      "<script>#{secureInlineJavaScript}\n" +
      " var foo = 42;\n" +
      "#{/secureInlineJavaScript}</script>\n" +
      "<script id=\"some-id\" type=\"application/javascript\">#{secureInlineJavaScript}\n" +
      " var bar = 38;\n" +
      "#{/secureInlineJavaScript}</script>\n" +
      "<script src=\"/public/javascripts/actual-documents.js\" >#{secureInlineJavaScript}#{/secureInlineJavaScript}</script>\n" +
      "</html>\n";

    assertThat(new GTTemplateLocationReal(null, null).addInlineScriptTag(originalHtml)).isEqualTo(expectedHtml);
  }
}