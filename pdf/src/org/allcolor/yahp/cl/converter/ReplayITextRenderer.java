package org.allcolor.yahp.cl.converter;

import org.xhtmlrenderer.extend.ReplacedElementFactory;
import org.xhtmlrenderer.layout.SharedContext;
import org.xhtmlrenderer.pdf.ExtendedITextReplacedElementFactory;
import org.xhtmlrenderer.pdf.ITextOutputDevice;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.util.HashMap;
import java.util.Map;

class ReplayITextRenderer extends ITextRenderer {
  private final Map<String, String> knownFont = new HashMap<>();
  
  /**
   * Initializes a new renderer with extended capabilities.
   */
  ReplayITextRenderer() {
    ITextOutputDevice outputDevice = getOutputDevice();
    ReplacedElementFactory replacedElementFactory = new ExtendedITextReplacedElementFactory(outputDevice);
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
