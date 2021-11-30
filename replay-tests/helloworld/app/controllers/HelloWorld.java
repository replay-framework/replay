package controllers;

import com.google.common.collect.ImmutableMap;
import play.modules.pdf.PdfResult;
import play.mvc.PlayController;
import play.rebel.View;

public class HelloWorld implements PlayController {
  public View hello() {
    return new View("hello.html", ImmutableMap.of("who", "world"));
  }

  public PdfResult helloPdf() {
    return new PdfResult("hello.html").with("who", "PDF World");
  }
}
