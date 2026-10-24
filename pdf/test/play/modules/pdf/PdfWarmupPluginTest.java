package play.modules.pdf;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class PdfWarmupPluginTest {
  private final PdfWarmupPlugin plugin = new PdfWarmupPlugin();

  @Test
  public void rendersSamplePdfWithSvgImage() {
    assertThat(plugin.warmUp()).isGreaterThan(1000);
  }

  @Test
  public void doesNotFailApplicationStart() {
    plugin.onApplicationStart();
  }
}
