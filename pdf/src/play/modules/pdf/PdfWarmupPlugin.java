package play.modules.pdf;

import static java.lang.System.currentTimeMillis;
import static java.util.Objects.requireNonNull;

import com.google.errorprone.annotations.CheckReturnValue;
import java.io.ByteArrayOutputStream;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.PlayPlugin;

/**
 * Renders a tiny PDF (text + SVG image) on application start, so that the first real PDF request
 * doesn't pay for loading Flying Saucer, iText, Batik and Java2D classes.
 *
 * <p>To enable, add this line to {@code conf/play.plugins}:
 *
 * <pre>
 *   1000:play.modules.pdf.PdfWarmupPlugin
 * </pre>
 */
@NullMarked
@CheckReturnValue
public class PdfWarmupPlugin extends PlayPlugin {
  private static final Logger logger = LoggerFactory.getLogger(PdfWarmupPlugin.class);

  private final PdfHelper helper = new PdfHelper();

  @Override
  public void onApplicationStart() {
    long start = currentTimeMillis();
    try {
      int size = warmUp();
      logger.info("Warmed up PDF generation in {} ms. ({} bytes)", currentTimeMillis() - start, size);
    } catch (RuntimeException e) {
      logger.warn("Failed to warm up PDF generation in {} ms.", currentTimeMillis() - start, e);
    }
  }

  int warmUp() {
    String svg = requireNonNull(getClass().getResource("warmup.svg")).toExternalForm();
    String html =
        """
        <html>
        <head><title>warmup</title></head>
        <body>
          <h1>Warmup</h1>
          <p>Hello, PDF!</p>
          <img src="%s" width="30px"/>
        </body>
        </html>
        """.formatted(svg);
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    helper.renderPDF(new PDFDocument(html), out, null);
    return out.size();
  }
}
