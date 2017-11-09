/**
 * Copyright 2010, Lunatech Labs.
 * <p>
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * <p>
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * <p>
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 * <p>
 * User: nicolas
 * Date: Feb 14, 2010
 */
package play.modules.pdf;

import org.allcolor.yahp.converter.IHtmlToPdfTransformer;
import org.apache.commons.io.FilenameUtils;
import play.Play;
import play.classloading.enhancers.LocalvariablesNamesEnhancer;
import play.data.validation.Validation;
import play.exceptions.PlayException;
import play.exceptions.TemplateNotFoundException;
import play.exceptions.UnexpectedException;
import play.mvc.Http;
import play.mvc.Http.Request;
import play.mvc.Scope;
import play.vfs.VirtualFile;

import java.io.*;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class PDF {

  public static class Options {

    public String FOOTER;
    public String FOOTER_TEMPLATE;
    public String HEADER;
    public String HEADER_TEMPLATE;
    public String ALL_PAGES;
    public String EVEN_PAGES;
    public String ODD_PAGES;

    public String filename;

    public IHtmlToPdfTransformer.PageSize pageSize = IHtmlToPdfTransformer.A4P;
  }

  public static class PDFDocument {
    public String template;
    public Options options;
    public Map<String, Object> args = new HashMap<>();
    List<IHtmlToPdfTransformer.CHeaderFooter> headerFooterList = new LinkedList<>();
    String content;

    private PDFDocument(String template, Options options) {
      this.template = template;
      this.options = options;
    }

    /**
     * @deprecated Use {@link #renderTemplateAsPDF(OutputStream, MultiPDFDocuments, boolean, Map)} 
     * or {@link #generateTemplateAsPDF(java.lang.String, java.util.Map, boolean, play.modules.pdf.PDF.Options)}
     */
    @Deprecated
    public PDFDocument(String template, Options options, Object... args) {
      this(template, options);
      for (Object o : args) {
        List<String> names = LocalvariablesNamesEnhancer.LocalVariablesNamesTracer.getAllLocalVariableNames(o);
        for (String name : names) {
          this.args.put(name, o);
        }
      }
    }

    public PDFDocument(String template, Options options, Map<String, Object> args) {
      this(template, options);
      this.args.putAll(args);
    }

    public PDFDocument() {
    }
  }

  public static class MultiPDFDocuments {
    public List<PDFDocument> documents = new LinkedList<>();
    public String filename;

    public MultiPDFDocuments(String filename) {
      this.filename = filename;
    }

    public MultiPDFDocuments() {
    }

    public MultiPDFDocuments add(PDFDocument singleDoc) {
      documents.add(singleDoc);
      return this;
    }

    /**
     * @deprecated Use {@link #renderTemplateAsPDF(OutputStream, MultiPDFDocuments, boolean, Map)} 
     * or {@link #generateTemplateAsPDF(java.lang.String, java.util.Map, boolean, play.modules.pdf.PDF.Options)}
     */
    @Deprecated
    public MultiPDFDocuments add(String template, Options options, Object... args) {
      documents.add(new PDFDocument(template, options, args));
      return this;
    }

    public MultiPDFDocuments add(String template, Options options, Map<String, Object> args) {
      documents.add(new PDFDocument(template, options, args));
      return this;
    }
  }

  /**
   * Render the corresponding template
   *
   * @param args The template data
   *
   * @deprecated Use {@link #renderTemplateAsPDF(OutputStream, MultiPDFDocuments, boolean, Map)} 
   * or {@link #generateTemplateAsPDF(java.lang.String, java.util.Map, boolean, play.modules.pdf.PDF.Options)}
   */
  @Deprecated
  public static void renderPDF(Object... args) {
    render(args);
  }

  /**
   * @deprecated Use {@link #renderTemplateAsPDF(OutputStream, MultiPDFDocuments, boolean, Map)} 
   * or {@link #generateTemplateAsPDF(java.lang.String, java.util.Map, boolean, play.modules.pdf.PDF.Options)}
   */
  @Deprecated
  public static void render(Object... args) {
    writePDF(null, true, args);
  }

  /**
   * @deprecated Use {@link #renderTemplateAsPDF(OutputStream, MultiPDFDocuments, boolean, Map)} 
   * or {@link #generateTemplateAsPDF(java.lang.String, java.util.Map, boolean, play.modules.pdf.PDF.Options)}
   */
  @Deprecated
  public static void renderAsAttachment(Object... args) {
    writePDF(null, false, args);
  }

  /**
   * Render the corresponding template into a file
   *
   * @param file the file to render to, or null to render to the current Response object
   * @param args the template data
   * @deprecated Use {@link #renderTemplateAsPDF(OutputStream, MultiPDFDocuments, boolean, Map)} 
   * or {@link #generateTemplateAsPDF(java.lang.String, java.util.Map, boolean, play.modules.pdf.PDF.Options)}
   */
  @Deprecated
  public static void writePDF(File file, Object... args) {
    try (OutputStream os = new FileOutputStream(file)) {
      writePDF(os, true, args);
      os.flush();
    }
    catch (IOException e) {
      throw new UnexpectedException(e);
    }
  }

  /**
   * @deprecated Use {@link #renderTemplateAsPDF(OutputStream, MultiPDFDocuments, boolean, Map)} 
   * or {@link #generateTemplateAsPDF(java.lang.String, java.util.Map, boolean, play.modules.pdf.PDF.Options)}
   */
  @Deprecated
  public static void writePDF(OutputStream out, Object... args) {
    writePDF(out, true, args);
  }

  /**
   * Render the corresponding template into a file
   *
   * @param out  the stream to render to, or null to render to the current Response object
   * @param args the template data
   * @deprecated Use {@link #renderTemplateAsPDF(OutputStream, MultiPDFDocuments, boolean, Map)} 
   * or {@link #generateTemplateAsPDF(java.lang.String, java.util.Map, boolean, play.modules.pdf.PDF.Options)}
   */
  @Deprecated
  public static void writePDF(OutputStream out, boolean inline, Object... args) {
    final Http.Request request = Http.Request.current();
    final String format = request.format;

    PDFDocument singleDoc = new PDFDocument();
    MultiPDFDocuments docs = null;

    if (args.length > 0) {
      if (args[0] instanceof String && LocalvariablesNamesEnhancer.LocalVariablesNamesTracer.getAllLocalVariableNames(args[0]).isEmpty()) {
        singleDoc.template = args[0].toString();
      }
      else if (args[0] instanceof MultiPDFDocuments) {
        docs = (MultiPDFDocuments) args[0];
      }
      if (docs == null) {
        for (Object arg : args) {
          if (arg instanceof Options) {
            singleDoc.options = (Options) arg;
          }
        }
      }
    }
    if (docs == null) {
      if (singleDoc.template == null) {
        singleDoc.template = templateNameFromAction(format);
      }
      docs = createMultiPDFDocuments(singleDoc);
    }

    renderTemplateAsPDF(out, docs, inline, args);
  }

  private static MultiPDFDocuments createMultiPDFDocuments(PDFDocument singleDoc) {
    MultiPDFDocuments docs = new MultiPDFDocuments();
    docs.add(singleDoc);
    docs.filename = fileName(singleDoc);
    return docs;
  }

  private static String fileName(PDFDocument singleDoc) {
    return singleDoc.options != null && singleDoc.options.filename != null ?
        singleDoc.options.filename :
        FilenameUtils.getBaseName(singleDoc.template) + ".pdf";
  }

  static String templateNameFromAction(String format) {
    return Request.current().action.replace(".", "/") + "." + (format == null ? "html" : format);
  }

  static String resolveTemplateName(String templateName, Request request, String format) {
    if (templateName.startsWith("@")) {
      templateName = templateName.substring(1);
      if (!templateName.contains(".")) {
        templateName = request.controller + "." + templateName;
      }
      templateName = templateName.replace(".", "/") + "." + (format == null ? "html" : format);
    }
    VirtualFile template = Play.getVirtualFile(templateName);
    if (template == null || !template.exists()) {
      if (templateName.lastIndexOf("." + format) != -1) {
        templateName = templateName.substring(0, templateName.lastIndexOf("." + format)) + ".html";
      }
    }
    return templateName;
  }

  public static void renderTemplateAsPDF(String templateName, Map<String, Object> args, boolean inline, PDF.Options options) {
    PDF.PDFDocument singleDoc = new PDF.PDFDocument();
    singleDoc.template = templateName;
    singleDoc.options = options;
    PDF.MultiPDFDocuments docs = createMultiPDFDocuments(singleDoc);
    renderTemplateAsPDF(null, docs, inline, args);
  }

  public static byte[] generateTemplateAsPDF(String templateName, Map<String, Object> args, boolean inline, PDF.Options options) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();

    PDF.PDFDocument singleDoc = new PDF.PDFDocument();
    singleDoc.template = templateName;
    singleDoc.options = options;
    PDF.MultiPDFDocuments docs = createMultiPDFDocuments(singleDoc);
    renderTemplateAsPDF(out, docs, inline, args);
    return out.toByteArray();
  }

  /**
   * Render a specific template
   *
   * @param args The template data
   * @deprecated Use {@link #renderTemplateAsPDF(OutputStream, MultiPDFDocuments, boolean, Map)} 
   * or {@link #generateTemplateAsPDF(java.lang.String, java.util.Map, boolean, play.modules.pdf.PDF.Options)}
   */
  @Deprecated
  public static void renderTemplateAsPDF(OutputStream out, MultiPDFDocuments docs, boolean inline, Object... args) {
    Map<String, Object> arguments = new HashMap<>();
    for (Object o : args) {
      List<String> names = LocalvariablesNamesEnhancer.LocalVariablesNamesTracer.getAllLocalVariableNames(o);
      for (String name : names) {
        arguments.put(name, o);
      }
    }
    renderTemplateAsPDF(out, docs, inline, arguments);
  }

  /**
   * Render a specific template
   *
   * @param args The template data
   */
  static void renderTemplateAsPDF(OutputStream out, MultiPDFDocuments docs, boolean inline, Map<String, Object> args) {
    Map<String, Object> templateBinding = new HashMap<>();
    templateBinding.putAll(args);
    Scope.RenderArgs renderArgs = Scope.RenderArgs.current();
    if (renderArgs != null) {
      templateBinding.putAll(renderArgs.data);
    }
    
    templateBinding.put("session", Scope.Session.current());
    templateBinding.put("request", Http.Request.current());
    templateBinding.put("flash", Scope.Flash.current());
    templateBinding.put("params", Scope.Params.current());
    templateBinding.put("errors", Validation.errors());
    
    try {
      if (out == null) {
        // we're rendering to the current Response object
        throw new RenderPDFTemplate(docs, inline, templateBinding);
      }
      else {
        RenderPDFTemplate renderer = new RenderPDFTemplate(docs, inline, templateBinding);
        renderer.writePDF(out, Http.Request.current());
      }
    }
    catch (TemplateNotFoundException ex) {
      if (ex.isSourceAvailable()) {
        throw ex;
      }
      StackTraceElement element = PlayException.getInterestingStackTraceElement(ex);
      if (element != null) {
        throw new TemplateNotFoundException(ex.getPath(),
            Play.classes.getApplicationClass(element.getClassName()), element.getLineNumber());
      }
      else {
        throw ex;
      }
    }
  }

}
