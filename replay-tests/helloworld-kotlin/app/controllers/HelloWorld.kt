package controllers

import play.modules.pdf.PdfResult
import play.mvc.PlayController
import play.rebel.View

class HelloWorld : PlayController {
  fun hello(): View {
    return View("hello.html", mapOf("who" to "world"))
  }

  fun helloPdf(): PdfResult {
    return PdfResult("hello.html").with("who", "PDF World on Kotlin")
  }
}