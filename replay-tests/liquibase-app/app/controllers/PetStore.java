package controllers;

import com.google.common.collect.ImmutableMap;
import play.mvc.Controller;
import play.rebel.View;

public class PetStore extends Controller {
  public View index() {
    // TODO select data from DB
    String username = "Pet Admin";
    return new View("pet-store.html", ImmutableMap.of("who", username));
  }
}
