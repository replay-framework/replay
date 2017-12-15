package play.mvc;

import org.junit.After;
import org.junit.Test;
import play.mvc.results.RenderJson;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class RendererTest {
  TimeZone originalTimeZone = TimeZone.getDefault();

  @After
  public void tearDown() {
    TimeZone.setDefault(originalTimeZone);
  }

  @Test
  public void jsonDatesAreInStandardFormatWithoutTimeZone() throws Exception {
    TimeZone.setDefault(TimeZone.getTimeZone("Europe/Tallinn"));

    Date dateTime = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").parse(("11.07.2013 05:50:13"));
    try {
      new Renderer().json(dateTime);
      fail("expected RenderJson");
    }
    catch (RenderJson json) {
      assertThat(json.getJson()).isEqualTo("\"2013-07-11T05:50:13\"");
    }
  }
}