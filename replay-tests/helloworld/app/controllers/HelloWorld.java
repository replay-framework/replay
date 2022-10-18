package controllers;

import com.google.common.collect.ImmutableMap;
import play.modules.pdf.PdfResult;
import play.mvc.Controller;
import play.rebel.View;

public class HelloWorld extends Controller {
  public View hello() {
    return new View("hello.html", ImmutableMap.of("who", "world"));
  }

  public PdfResult helloPdf() {
    return new PdfResult("hello.html").with("who", "PDF World");
  }

  public PdfResult helloPdfFromPlainText() {
    return new PdfResult("HelloWorld/hello.txt")
      .with("who", "PDF from plain text");
  }

  public PdfResult helloPdfUsingRequestFormat() {
    request.format = "txt";
    return new PdfResult("@hello")
      .with("who", "Request format");
  }
}
