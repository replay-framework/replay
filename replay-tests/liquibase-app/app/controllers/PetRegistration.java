package controllers;

import model.Pet;
import model.PetRepository;
import play.data.validation.Valid;
import play.data.validation.Validation;
import play.mvc.Controller;
import play.mvc.results.Redirect;
import play.mvc.results.Result;
import play.rebel.View;

import javax.inject.Inject;
import java.util.Map;

public class PetRegistration extends Controller {
  private final PetRepository repository;

  @Inject
  public PetRegistration(PetRepository repository) {
    this.repository = repository;
  }

  public Result form() {
    return new View("pet-form.html", Map.of("pet", new Pet()));
  }

  public Result register(@Valid Pet pet) {
    if (Validation.hasErrors()) {
      System.out.println(Validation.errors());
      return new View("pet-form.html", Map.of("pet", pet));
    }
    repository.register(pet);
    return new Redirect("/");
  }
}
