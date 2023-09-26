package org.allcolor.yahp.cl.converter;

import org.junit.jupiter.api.Test;
import static org.allcolor.yahp.cl.converter.CHtmlToPdfFlyingSaucerTransformer.removeScript;
import static org.assertj.core.api.Assertions.assertThat;

public class CHtmlToPdfFlyingSaucerTransformerTest {
  CHtmlToPdfFlyingSaucerTransformer transformer = new CHtmlToPdfFlyingSaucerTransformer();

  @Test
  public void removesScriptTagFromHtml() {
    assertThat(removeScript("<script src=\"/public/gen/main.js?16b1e5a0df\"></script>")).isEqualTo("");
    assertThat(removeScript("<script></script>")).isEqualTo("");
    assertThat(removeScript("<script></script><script></script>")).isEqualTo("");
    assertThat(removeScript("<script>foo</script><script>bar</script>")).isEqualTo("");
    assertThat(removeScript("foo<script></script>bar")).isEqualTo("foobar");
    assertThat(removeScript("foo<script></script>")).isEqualTo("foo");
    assertThat(removeScript("<script></script>bar")).isEqualTo("bar");
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
    assertThat(removeScript(html)).isEqualTo("<!DOCTYPE html>\n" +
                                             "<html lang=\"ru\" class=\"\" xmlns=\"http://www.w3.org/1999/xhtml\">\n" +
                                             "  <head>\n" +
                                             "<style type=\"text/css\">\n" +
                                             "/*<![CDATA[*/" +
                                             "</style>\n" +
                                             "\n" +
                                             "\n" +
                                             "</head>\n" +
                                             "</html>\n\n");
  }

  @Test
  void extractsWidthAndHeight() {
    assertThat(transformer.transformStyle(""))
      .isEqualTo("display: inline-block;border: 1px solid black;width: 50px;");

    assertThat(transformer.transformStyle("width: 200px;"))
      .isEqualTo("display: inline-block;border: 1px solid black;width: : 200px;");

    assertThat(transformer.transformStyle("height: 100px;"))
      .isEqualTo("display: inline-block;border: 1px solid black;width: 50px;height: : 100px;");

    assertThat(transformer.transformStyle("width: 200px; height: 100px;"))
      .isEqualTo("display: inline-block;border: 1px solid black;width: : 200px;height: : 100px;");

    assertThat(transformer.transformStyle("border: 1px; width: 200px; height: 100px; position: absolute;"))
      .isEqualTo("display: inline-block;border: 1px solid black;width: : 200px;height: : 100px;");
  }

}