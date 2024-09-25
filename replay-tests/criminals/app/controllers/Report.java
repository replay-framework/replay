package controllers;

import static java.util.Objects.requireNonNullElse;

import play.modules.pdf.PdfResult;
import play.mvc.Controller;

public class Report extends Controller {
  public PdfResult pdf(int days) {
    return new PdfResult("dashboard/report.html")
        .with("username", requireNonNullElse(session.get("username"), "Anonymous"))
        .with("days", days);
  }
}
