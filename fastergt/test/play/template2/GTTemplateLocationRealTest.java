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
      "#{inlineScript}<script>42</script>#{/inlineScript}\n" +
      "#{inlineScript}<script>38</script>#{/inlineScript}\n";

    assertEquals(expectedHtml,
      new GTTemplateLocationReal(null, null).addInlineScriptTag(originalHtml));
  }

  @Test
  public void scriptsTagsCanHaveAttributes() {
    String originalHtml =
      "<script id=\"some-id\">42</script>\n" +
      "<script type=\"application/javascript\">38</script>\n";

    String expectedHtml =
      "#{inlineScript}<script id=\"some-id\">42</script>#{/inlineScript}\n" +
      "#{inlineScript}<script type=\"application/javascript\">38</script>#{/inlineScript}\n";

    assertEquals(expectedHtml,
      new GTTemplateLocationReal(null, null).addInlineScriptTag(originalHtml));
  }

  @Test
  public void addsTag_inlineScript_aroundEveryJavaScript() {
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
      "#{inlineScript}<script>\n" +
      " var foo = 42;\n" +
      "</script>#{/inlineScript}\n" +
      "#{inlineScript}<script id=\"some-id\" type=\"application/javascript\">\n" +
      " var bar = 38;\n" +
      "</script>#{/inlineScript}\n" +
      "#{inlineScript}<script src=\"/public/javascripts/actual-documents.js\" ></script>#{/inlineScript}\n" +
      "</html>\n";

    assertEquals(expectedHtml,
      new GTTemplateLocationReal(null, null).addInlineScriptTag(originalHtml));
  }
}