package controllers;

import play.mvc.Controller;
import play.mvc.results.Result;
import play.rebel.View;

public class Dashboard extends Controller {

  public Result index() {
    return new View("dashboard/index.html").with("username", session.get("username"));
  }
}
