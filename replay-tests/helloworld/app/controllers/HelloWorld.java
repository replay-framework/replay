package controllers;

import static java.util.Objects.requireNonNullElse;

import com.google.common.collect.ImmutableMap;
import play.data.validation.Min;
import play.data.validation.Valid;
import play.data.validation.Validation;
import play.modules.pdf.PdfResult;
import play.mvc.Controller;
import play.mvc.results.BadRequest;
import play.rebel.View;

public class HelloWorld extends Controller {
  public View hello(String greeting) {
    return new View("hello.html", ImmutableMap.of("who", requireNonNullElse(greeting, "world")));
  }

  public View hola(String greeting) {
    return hello(greeting);
  }

  public View images(String greeting) {
    return new View("images.html", ImmutableMap.of("who", requireNonNullElse(greeting, "world")));
  }

  public View epicFail() {
    return new View("epic-fail.html", ImmutableMap.of("who", "world"));
  }

  public View repeat(@Valid @Min(2) int times) {
    if (Validation.hasErrors()) {
      throw new BadRequest(Validation.errors().toString());
    }
    return new View("hello.html", ImmutableMap.of("who", "world #" + times));
  }

  public PdfResult helloPdf() {
    return new PdfResult("hello.html").with("who", "PDF World");
  }

  public PdfResult helloPdfFromPlainText() {
    return new PdfResult("HelloWorld/hello.txt").with("who", "PDF from plain text");
  }

  public PdfResult helloPdfUsingRequestFormat() {
    request.format = "txt";
    return new PdfResult("@hello").with("who", "Request format");
  }

  public View empty() {
    return new View();
  }
}
