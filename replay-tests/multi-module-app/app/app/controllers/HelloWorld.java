package controllers;

import com.google.common.collect.ImmutableMap;
import play.mvc.Controller;
import play.rebel.View;

public class HelloWorld extends Controller {
  public View hello() {
    return new View("hello.html", ImmutableMap.of("who", "world"));
  }
}
