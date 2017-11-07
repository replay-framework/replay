package play.mvc;

import org.junit.Test;
import play.mvc.results.RenderJson;

import java.text.SimpleDateFormat;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class RendererTest {
  @Test
  public void jsonDatesAreInStandardFormatWithoutTimeZone() throws Exception {
    Date dateTime = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").parse(("11.07.2013 05:50:13"));
    try {
      new Renderer().json(dateTime);
      fail();
    }
    catch (RenderJson json) {
      assertThat(json.getJson()).isEqualTo("\"2013-07-11T05:50:13\"");
    }
  }
}