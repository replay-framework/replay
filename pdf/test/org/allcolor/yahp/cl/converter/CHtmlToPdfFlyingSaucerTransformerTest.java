package org.allcolor.yahp.cl.converter;

import org.junit.Test;
import static org.allcolor.yahp.cl.converter.CHtmlToPdfFlyingSaucerTransformer.removeScript;
import static org.junit.Assert.*;

public class CHtmlToPdfFlyingSaucerTransformerTest {
  @Test
  public void removesScriptTagFromHtml() {
    assertEquals("", removeScript("<script src=\"/public/gen/main.js?16b1e5a0df\"></script>"));
    assertEquals("", removeScript("<script></script>"));
    assertEquals("", removeScript("<script></script><script></script>"));
    assertEquals("", removeScript("<script>foo</script><script>bar</script>"));
    assertEquals("foobar", removeScript("foo<script></script>bar"));
    assertEquals("foo", removeScript("foo<script></script>"));
    assertEquals("bar", removeScript("<script></script>bar"));
  }

  @Test
  public void removesScriptTagFromHtml_realisticExample() {
    String html = "<!DOCTYPE html>\n" +
        "<html lang=\"ru\" class=\"\" xmlns=\"http://www.w3.org/1999/xhtml\">\n" +
        "  <head>\n" +
        "<style type=\"text/css\">\n" +
        "/*<![CDATA[*/" +
        "</style>\n" +
        "<script type=\"text/javascript\" src=\"/public/javascripts/jquery.js\">\n" +
        "//<![CDATA[\n" +
        "//]]>\n" +
        "</script>\n" +
        "<script type=\"text/javascript\"\n" +
        "src=\"/public/javascripts/jquery-migrate.min.js\">\n" +
        "//<![CDATA[\n" +
        "//]]>\n" +
        "</script>\n" +
        "</head>\n" +
        "</html>\n" +
        "<script>$.migrateMute = false;</script>\n" +
        "<script src=\"/public/gen/main.js?16b1e5a0df\"></script>";
    assertEquals("<!DOCTYPE html>\n" +
        "<html lang=\"ru\" class=\"\" xmlns=\"http://www.w3.org/1999/xhtml\">\n" +
        "  <head>\n" +
        "<style type=\"text/css\">\n" +
        "/*<![CDATA[*/" +
        "</style>\n" +
        "\n" +
        "\n" +
        "</head>\n" +
        "</html>\n\n", removeScript(html));
  }
}