package org.allcolor.yahp.cl.converter;

import java.util.HashMap;
import java.util.Map;
import com.google.errorprone.annotations.CheckReturnValue;
import org.jspecify.annotations.NullMarked;
import org.xhtmlrenderer.extend.ReplacedElementFactory;
import org.xhtmlrenderer.layout.SharedContext;
import org.xhtmlrenderer.pdf.ITextOutputDevice;
import org.xhtmlrenderer.pdf.ITextRenderer;
import org.xhtmlrenderer.pdf.ITextReplacedElementFactory;
import play.modules.pdf.ReplayUserAgent;

@NullMarked
@CheckReturnValue
class ReplayITextRenderer extends ITextRenderer {
  private final Map<String, String> knownFont = new HashMap<>();

  static ReplayITextRenderer build() {
    ITextOutputDevice outputDevice = new ITextOutputDevice(DEFAULT_DOTS_PER_POINT);
    return new ReplayITextRenderer(outputDevice);
  }

  /** Initializes a new renderer with extended capabilities. */
  private ReplayITextRenderer(ITextOutputDevice outputDevice) {
    super(
        DEFAULT_DOTS_PER_POINT,
        DEFAULT_DOTS_PER_PIXEL,
        outputDevice,
        new ReplayUserAgent(outputDevice));
    ReplacedElementFactory replacedElementFactory = new ITextReplacedElementFactory(outputDevice);
    SharedContext sharedContext = getSharedContext();
    sharedContext.setReplacedElementFactory(replacedElementFactory);
  }

  void addKnown(final String path) {
    this.knownFont.put(path, path);
  }

  boolean isKnown(final String path) {
    return this.knownFont.get(path) != null;
  }
}
