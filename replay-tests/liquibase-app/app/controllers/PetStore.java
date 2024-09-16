package controllers;

import com.google.common.collect.ImmutableMap;
import jakarta.inject.Inject;
import java.util.List;
import model.Pet;
import model.PetRepository;
import play.mvc.Controller;
import play.rebel.View;

public class PetStore extends Controller {
  private final PetRepository repository;

  @Inject
  public PetStore(PetRepository repository) {
    this.repository = repository;
  }

  public View index() {
    List<Pet> pets = repository.loadAllPets();
    String username = "Pet Admin";
    return new View("pet-store.html", ImmutableMap.of("who", username, "pets", pets));
  }
}
