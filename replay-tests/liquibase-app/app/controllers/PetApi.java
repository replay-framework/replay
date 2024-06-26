package controllers;

import model.PetRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.Play;
import play.mvc.Before;
import play.mvc.Controller;
import play.mvc.results.Forbidden;
import play.mvc.results.RenderJson;
import play.mvc.results.Result;

import jakarta.inject.Inject;
import java.util.Map;

public class PetApi extends Controller {
  private static final Logger log = LoggerFactory.getLogger(PetApi.class);
  private final PetRepository repository;

  @Inject
  PetApi(PetRepository repository) {
    this.repository = repository;
  }

  @Before
  public void checkEnv() {
    if (!Play.id.equals("test")) {
      throw new Forbidden("env");
    }
  }
  
  public Result resetAllPets() {
    int count = repository.deleteAllPets();
    log.info("Deleted {} pets", count);
    return new RenderJson(Map.of("count", count));
  }
}
