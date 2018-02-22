package play.template2;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class GTTemplateLocationRealTest {
  @Test
  public void embedsAllFoundScripts() {
    String originalHtml =
      "<script>42</script>\n" +
      "<script>38</script>\n";

    String expectedHtml =
      "#{secureInlineJavaScript}<script>42</script>#{/secureInlineJavaScript}\n" +
      "#{secureInlineJavaScript}<script>38</script>#{/secureInlineJavaScript}\n";

    assertEquals(expectedHtml,
      new GTTemplateLocationReal(null, null).addInlineScriptTag(originalHtml));
  }

  @Test
  public void scriptsTagsCanHaveAttributes() {
    String originalHtml =
      "<script id=\"some-id\">42</script>\n" +
      "<script type=\"application/javascript\">38</script>\n";

    String expectedHtml =
      "#{secureInlineJavaScript}<script id=\"some-id\">42</script>#{/secureInlineJavaScript}\n" +
      "#{secureInlineJavaScript}<script type=\"application/javascript\">38</script>#{/secureInlineJavaScript}\n";

    assertEquals(expectedHtml,
      new GTTemplateLocationReal(null, null).addInlineScriptTag(originalHtml));
  }

  @Test
  public void ignoresScriptWithEmptyBody() {
    String originalHtml =
      "<script id=\"some-id\">42</script>\n" +
      "<script src=\"public/javascript/some.js\"></script>\n";

    String expectedHtml =
      "#{secureInlineJavaScript}<script id=\"some-id\">42</script>#{/secureInlineJavaScript}\n" +
      "<script src=\"public/javascript/some.js\"></script>\n";

    assertEquals(expectedHtml,
      new GTTemplateLocationReal(null, null).addInlineScriptTag(originalHtml));
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
      "#{secureInlineJavaScript}<script>\n" +
      " var foo = 42;\n" +
      "</script>#{/secureInlineJavaScript}\n" +
      "#{secureInlineJavaScript}<script id=\"some-id\" type=\"application/javascript\">\n" +
      " var bar = 38;\n" +
      "</script>#{/secureInlineJavaScript}\n" +
      "<script src=\"/public/javascripts/actual-documents.js\" ></script>\n" +
      "</html>\n";

    assertEquals(expectedHtml,
      new GTTemplateLocationReal(null, null).addInlineScriptTag(originalHtml));
  }
}