package controllers

import play.mvc.PlayController
import play.rebel.View

class HelloWorld : PlayController {
  fun hello(): View {
    return View("hello.html", mapOf("who" to "world"))
  }
}