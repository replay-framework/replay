package org.allcolor.yahp.cl.converter;

import com.lowagie.text.pdf.BaseFont;
import org.allcolor.xml.parser.CShaniDomParser;
import org.allcolor.xml.parser.dom.ADocument;
import org.allcolor.yahp.cl.converter.CDocumentCut.DocumentAndSize;
import org.allcolor.yahp.converter.IHtmlToPdfTransformer;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.*;
import org.xhtmlrenderer.extend.ReplacedElementFactory;
import org.xhtmlrenderer.layout.SharedContext;
import org.xhtmlrenderer.pdf.ExtendedITextReplacedElementFactory;
import org.xhtmlrenderer.pdf.ITextOutputDevice;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.*;
import java.text.NumberFormat;
import java.util.*;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * This class transform an html document in a PDF.
 *
 * @author Quentin Anciaux
 * @version 0.02
 */
public final class CHtmlToPdfFlyingSaucerTransformer implements IHtmlToPdfTransformer {
  private static class _ITextRenderer extends ITextRenderer {
    private final Map<String, String> knownFont = new HashMap<>();


    /**
     * Initializes a new renderer with extended capabilities.
     */
    private _ITextRenderer() {
      ITextOutputDevice outputDevice = getOutputDevice();

      ReplacedElementFactory replacedElementFactory = new ExtendedITextReplacedElementFactory(outputDevice);

      SharedContext sharedContext = getSharedContext();

      sharedContext.setReplacedElementFactory(replacedElementFactory);
    }

    private void addKnown(final String path) {
      this.knownFont.put(path, path);
    }

    private boolean isKnown(final String path) {
      return this.knownFont.get(path) != null;
    }
  }

  private static final Logger log = LoggerFactory.getLogger(CHtmlToPdfFlyingSaucerTransformer.class);

  private static boolean accept(final File dir, final String name) {
    return name.toLowerCase().endsWith(".ttf");
  }

  private static void registerTTF(final File f, final _ITextRenderer renderer) {
    if (f.isDirectory()) {
      final File[] list = f.listFiles();
      if (list != null) {
        for (File aList : list) {
          registerTTF(aList, renderer);
        }
      }
    }
    else if (accept(f.getParentFile(), f.getName())) {
      if (!renderer.isKnown(f.getAbsolutePath())) {
        try {
          renderer.getFontResolver().addFont(f.getAbsolutePath(),
              BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
          renderer.addKnown(f.getAbsolutePath());
        }
        catch (final Throwable ignore) {
        }
      }
    }
  }

  static String removeScript(String html) {
    int nextScriptStart = html.indexOf("<script");
    if (nextScriptStart == -1) return html;

    StringBuilder sb = new StringBuilder(html.length() - 16);
    int i = 0;
    while (nextScriptStart > -1) {
      sb.append(html, i, nextScriptStart);
      i = html.indexOf("</script>", nextScriptStart) + 9;
      nextScriptStart = html.indexOf("<script", i);
    }
    sb.append(html, i, html.length());
    return sb.toString();
  }

  /**
   * Creates a new CHtmlToPdfFlyingSaucerTransformer object.
   */
  public CHtmlToPdfFlyingSaucerTransformer() {
  }

  private void convertComboboxToVisibleHTML(final Document doc) {
    final NodeList nl = doc.getElementsByTagName("select");
    while (nl.getLength() > 0) {
      final Element select = (Element) nl.item(0);
      final String ssize = select.getAttribute("size");
      int size = 1;
      if (!"".equals(ssize.trim())) {
        try {
          size = Integer.parseInt(ssize);
        }
        catch (final Exception ignore) {
        }
      }
      final Node node = select.getParentNode();
      final NodeList options = select.getElementsByTagName("option");
      final String style = select.getAttribute("style");
      String width = null;
      String height = null;
      if (style.contains("width")) {
        width = style.substring(style.indexOf("width") + 5);
        if (width.indexOf(':') != -1) {
          width = width.substring(width.indexOf(':'));
        }
        if (width.indexOf(';') != -1) {
          width = width.substring(0, width.indexOf(';'));
        }
      }
      if (width == null) {
        width = "50px";
      }
      if (style.contains("height")) {
        height = style.substring(style.indexOf("height") + 6);
        if (height.indexOf(':') != -1) {
          height = height.substring(height.indexOf(':'));
        }
        if (height.indexOf(';') != -1) {
          height = height.substring(0, height.indexOf(';'));
        }
      }
      if (size > 1) {
        final Element span = doc.createElement("span");
        span
            .setAttribute("style",
                "display: inline-block;border: 1px solid black;" + ("width: " + width + ";") + (height != null ? "height: " + height + ";" : ""));
        for (int i = 0; (i < options.getLength()) && (i < size); i++) {
          final Element option = (Element) options.item(i);
          final Element content = doc.createElement("span");
          content.setTextContent(option.getTextContent());
          span.appendChild(content);
          if (i < options.getLength() - 1) {
            final Element br = doc.createElement("br");
            span.appendChild(br);
          }
        }
        node.insertBefore(span, select);
      }
      else {
        for (int i = 0; i < options.getLength(); i++) {
          final Element option = (Element) options.item(i);
          if ("selected".equalsIgnoreCase(option
              .getAttribute("selected"))) {
            final Element span = doc.createElement("span");
            span.setTextContent(option.getTextContent().trim());
            span.setAttribute("style",
                "display: inline-block;border: 1px solid black;" + ("width: " + width + ";") + (height != null ? "height: " + height + ";" : ""));
            node.insertBefore(span, select);
            break;
          }
        }
      }
      node.removeChild(select);
    }
  }

  private void convertInputToVisibleHTML(final Document doc) {
    final NodeList nl = doc.getElementsByTagName("input");
    while (nl.getLength() > 0) {
      final Element input = (Element) nl.item(0);
      final String type = input.getAttribute("type");
      final Node node = input.getParentNode();
      if ("image".equalsIgnoreCase(type)) {
        final Element img = doc.createElement("img");
        img.setAttribute("src", input.getAttribute("src"));
        img.setAttribute("alt", input.getAttribute("alt"));
        node.insertBefore(img, input);
      }
      else if ("text".equalsIgnoreCase(type)
          || "password".equalsIgnoreCase(type)
          || "button".equalsIgnoreCase(type)) {
        final String style = input.getAttribute("style");
        String width = null;
        String height = null;
        if (style.contains("width")) {
          width = style.substring(style.indexOf("width") + 5);
          if (width.indexOf(':') != -1) {
            width = width.substring(width.indexOf(':'));
          }
          if (width.indexOf(';') != -1) {
            width = width.substring(0, width.indexOf(';'));
          }
        }
        if (width == null) {
          width = "100px";
        }
        if (style.contains("height")) {
          height = style.substring(style.indexOf("height") + 6);
          if (height.indexOf(':') != -1) {
            height = height.substring(height.indexOf(':'));
          }
          if (height.indexOf(';') != -1) {
            height = height.substring(0, height.indexOf(';'));
          }
        }
        if (height == null) {
          height = "16px";
        }
        final Element span = doc.createElement("span");
        span.setTextContent(input.getAttribute("value"));
        span
            .setAttribute(
                "style",
                "display: inline-block;border: 1px solid black;" +
                    ("width: " + width + ";") +
                    ("height: " + height + ";") +
                    ("button".equalsIgnoreCase(type) ? "background-color: #DDDDDD;" : ""));
        node.insertBefore(span, input);
      }
      else if ("radio".equalsIgnoreCase(type)) {
        final Element span = doc.createElement("span");
        span.setTextContent("\u00A0");
        if ("checked".equalsIgnoreCase(input.getAttribute("checked"))) {
          span
              .setAttribute(
                  "style",
                  "display: inline-block;width: 10px;background-color: black;height: 10px;border: 1px solid black;");
        }
        else {
          span
              .setAttribute("style",
                  "display: inline-block;width: 10px;height: 10px;border: 1px solid black;");
        }
        node.insertBefore(span, input);
      }
      else if ("checkbox".equalsIgnoreCase(type)) {
        final Element span = doc.createElement("span");
        span.setTextContent("\u00A0");
        if ("checked".equalsIgnoreCase(input.getAttribute("checked"))) {
          span
              .setAttribute(
                  "style",
                  "display: inline-block;width: 10px;background-color: black;height: 10px;border: 1px solid black;");
        }
        else {
          span
              .setAttribute("style",
                  "display: inline-block;width: 10px;height: 10px;border: 1px solid black;");
        }
        node.insertBefore(span, input);
      }
      node.removeChild(input);
    }
  }

  private void convertTextAreaToVisibleHTML(final Document doc) {
    final NodeList nl = doc.getElementsByTagName("textarea");
    while (nl.getLength() > 0) {
      final Element textarea = (Element) nl.item(0);
      final Node node = textarea.getParentNode();
      final String style = textarea.getAttribute("style");
      String width = null;
      String height = null;
      if (style.contains("display:none")
          || style.contains("display: none")
          || style.contains("visibility:hidden")
          || style.contains("visibility: hidden")) {
        node.removeChild(textarea);
        continue;
      }
      if (style.contains("width")) {
        width = style.substring(style.indexOf("width") + 5);
        if (width.indexOf(':') != -1) {
          width = width.substring(width.indexOf(':'));
        }
        if (width.indexOf(';') != -1) {
          width = width.substring(0, width.indexOf(';'));
        }
      }
      if (width == null) {
        width = "100px";
      }
      if (style.contains("height")) {
        height = style.substring(style.indexOf("height") + 6);
        if (height.indexOf(':') != -1) {
          height = height.substring(height.indexOf(':'));
        }
        if (height.indexOf(';') != -1) {
          height = height.substring(0, height.indexOf(';'));
        }
      }
      if (height == null) {
        height = "50px";
      }
      final Element span = doc.createElement("span");
      span
          .setAttribute("style",
              "display: inline-block;border: 1px solid black;" + ("width: " + width + ";") + ("height: " + height + ";"));
      node.insertBefore(span, textarea);
      final String content = textarea.getTextContent();
      node.removeChild(textarea);
      try (final BufferedReader reader = new BufferedReader(new StringReader(content))) {
        String line;
        while ((line = reader.readLine()) != null) {
          final Element econtent = doc.createElement("span");
          econtent.setTextContent(line);
          final Element br = doc.createElement("br");
          span.appendChild(econtent);
          span.appendChild(br);
        }
      }
      catch (IOException ignore) {
      }
    }

  }

  private CShaniDomParser createCShaniDomParser() {
    CShaniDomParser ret = new CShaniDomParser(true, false);
    ret.setAutodoctype(false);
    ret.setIgnoreDTD(true);
    return ret;
  }

  /**
   * Transform the html document in the inputstream to a pdf in the
   * outputstream
   *
   * @param in         html document stream
   * @param urlForBase base url of the document
   * @param size       pdf document page size
   * @param hf         header-footer list
   * @param properties transform properties
   * @param out        out stream to the pdf file
   */
  @Override public void transform(final InputStream in, String urlForBase,
                                  final PageSize size, final List hf, final Map properties,
                                  final OutputStream out) throws CConvertException {
    List<File> files = new ArrayList<>();
    try {
      final CShaniDomParser parser = createCShaniDomParser();
      final _ITextRenderer renderer = new _ITextRenderer();

      String html = IOUtils.toString(in, UTF_8);

      Document theDoc = parser.parse(new StringReader(removeScript(html)));
      this.convertInputToVisibleHTML(theDoc);
      this.convertComboboxToVisibleHTML(theDoc);
      this.convertTextAreaToVisibleHTML(theDoc);
      final NodeList styles = theDoc.getElementsByTagName("style");
      for (int i = 0; i < styles.getLength(); i++) {
        final Node n = styles.item(i);
        final StringBuilder style = new StringBuilder();
        while (n.getChildNodes().getLength() > 0) {
          final Node child = n.getChildNodes().item(0);
          if (child.getNodeType() == Node.COMMENT_NODE) {
            final Comment c = (Comment) child;
            style.append(c.getData());
          }
          else if (child.getNodeType() == Node.TEXT_NODE) {
            final Text c = (Text) child;
            style.append(c.getData());
          }
          else if (child.getNodeType() == Node.CDATA_SECTION_NODE) {
            final CDATASection c = (CDATASection) child;
            style.append(c.getData());
          }
          n.removeChild(child);
        }

        final String content = style.toString().trim();
        final Text start = theDoc.createTextNode("/*");
        final CDATASection cd = theDoc.createCDATASection("*/\n"
            + content + "\n/*");
        final Text end = theDoc.createTextNode("*/\n");
        n.appendChild(start);
        n.appendChild(cd);
        n.appendChild(end);
      }

      final Element documentElement = theDoc.getDocumentElement();
      if (documentElement == null) {
        throw new IllegalArgumentException("Html hasn't document node");
      }

      final List<Node> toRemove = new ArrayList<>();
      final NodeList tnl = theDoc.getChildNodes();
      for (int i = 0; i < tnl.getLength(); i++) {
        final Node n = tnl.item(i);
        if (n != documentElement) {
          toRemove.add(n);
        }
      }
      final Node title = documentElement
          .getElementsByTagName("title").item(0);
      if ((title != null)
          && (properties.get(IHtmlToPdfTransformer.PDF_TITLE) == null)) {
        properties.put(IHtmlToPdfTransformer.PDF_TITLE, title
            .getTextContent());
      }

      Node body = documentElement.getElementsByTagName("body").item(0);
      Node head = documentElement.getElementsByTagName("head").item(0);
      for (Node n : toRemove) {
        n.getParentNode().removeChild(n);
        if (n.getNodeType() == Node.TEXT_NODE) {
          final Text t = (Text) n;
          if (t.getData().trim().isEmpty()) {
            continue;
          }
        }
        if ("link".equals(n.getNodeName())
            || "style".equals(n.getNodeName())) {
          head.appendChild(n);
        }
        else {
          body.appendChild(n);
        }
      }

      final DocumentAndSize[] docs = CDocumentCut.cut(theDoc, size);
      for (DocumentAndSize doc1 : docs) {
        Document mydoc = doc1.doc;
        body = mydoc.getDocumentElement().getElementsByTagName("body").item(0);
        head = mydoc.getDocumentElement().getElementsByTagName("head").item(0);
        try {
          String surlForBase = ((Element) mydoc.getElementsByTagName("base").item(0)).getAttribute("href");
          if (surlForBase != null && !surlForBase.isEmpty()) {
            urlForBase = surlForBase;
          }
        }
        catch (final Exception ignore) {
        }
        if (urlForBase != null) {
          mydoc.setDocumentURI(urlForBase);
        }
        final NodeList nl = mydoc.getElementsByTagName("base");

        if (nl.getLength() == 0) {
          final ADocument doc = (ADocument) mydoc;
          final Element base = doc.createElement("base");
          base.setAttribute("href", urlForBase);

          if (head.getFirstChild() != null) {
            head.insertBefore(base, head.getFirstChild());
          }
          else {
            head.appendChild(base);
          }
        }
        else {
          final Element base = (Element) nl.item(0);
          base.setAttribute("href", urlForBase);
        }

        final NumberFormat nf = NumberFormat.getInstance(Locale.US);
        nf.setMaximumFractionDigits(2);
        nf.setMinimumFractionDigits(0);
        final Element style = mydoc.createElement("style");
        style.setAttribute("type", "text/css");
        final double[] dsize = doc1.size.getCMSize();
        final double[] dmargin = doc1.size.getCMMargin();
        style.setTextContent("\n@page {\n" + "size: "
            + nf.format(dsize[0] / 2.54) + "in "
            + nf.format(dsize[1] / 2.54) + "in;\n"
            + "margin-left: " + nf.format(dmargin[0] / 2.54)
            + "in;\n" + "margin-right: "
            + nf.format(dmargin[1] / 2.54) + "in;\n"
            + "margin-bottom: " + nf.format(dmargin[2] / 2.54)
            + "in;\n" + "margin-top: "
            + nf.format(dmargin[3] / 2.54) + "in;\npadding: 0in;\n"
            + "}\n"

        );
        head.appendChild(style);
        if (properties.get(IHtmlToPdfTransformer.FOP_TTF_FONT_PATH) != null) {
          final File dir = new File((String) properties.get(IHtmlToPdfTransformer.FOP_TTF_FONT_PATH));
          if (dir.isDirectory()) {
            registerTTF(dir, renderer);
          }
        }
        ((ADocument) mydoc).setInputEncoding("utf-8");
        ((ADocument) mydoc).setXmlEncoding("utf-8");
        renderer.getSharedContext().setBaseURL(urlForBase);
        mydoc = parser.parse(new StringReader(mydoc.toString()));
        mydoc.getDomConfig().setParameter("entities", Boolean.FALSE);
        renderer.setDocument(mydoc, urlForBase);
        renderer.layout();

        final File f = File.createTempFile("pdf", "yahp");
        files.add(f);
        try (OutputStream fout = new BufferedOutputStream(new FileOutputStream(f))) {
          renderer.createPDF(fout, true);
          fout.flush();
        }
      }
      final PageSize[] sizes = new PageSize[docs.length];
      for (int i = 0; i < docs.length; i++) {
        sizes[i] = docs[i].size;

      }
      CDocumentReconstructor
          .reconstruct(
              files,
              properties,
              out,
              urlForBase,
              "Flying Saucer Renderer (https://xhtmlrenderer.dev.java.net/)",
              sizes, hf);
    }
    catch (final Throwable e) {
      log.error("Failed to transform html to pdf", e);
      throw new CConvertException("Failed to transform html to pdf: " + e, e);
    }
    finally {
      try {
        out.flush();
      }
      catch (final Exception ignore) {
      }
      for (final File f : files) {
        try {
          f.delete();
        }
        catch (final Exception ignore) {
        }
      }
    }
  }

}
