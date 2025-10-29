package controllers

import model.Identifier
import model.User
import play.mvc.PlayController
import play.mvc.results.BadRequest
import play.mvc.results.Result
import play.rebel.View

@Suppress("unused")
class TypedInputController : PlayController {

  fun printForced(userId: Long?): Result {
    if (userId == null) return BadRequest("forced userId missing")
    return View(
      "index.html",
      mapOf(
        "userId" to userId,
        "innerType" to userId::class.simpleName,
        "outerType" to "N/A"
      )
    )
  }

  fun printGeneric(userId: Identifier<User>?): Result {
    if (userId == null) return BadRequest("generic userId missing")
    return View(
      "index.html",
      mapOf(
        "userId" to userId,
        "innerType" to userId.value::class.simpleName,
        "outerType" to userId.getTypeString()
      )
    )
  }
}