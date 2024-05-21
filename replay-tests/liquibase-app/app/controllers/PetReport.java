package controllers;

import model.Pet;
import model.PetRepository;
import play.Play;
import play.i18n.Lang;
import play.i18n.Messages;
import play.modules.excel.RenderExcel;
import play.mvc.Controller;
import play.mvc.results.Result;

import javax.inject.Inject;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import static play.modules.excel.RenderExcel.RA_FILENAME;

public class PetReport extends Controller {
  private final PetRepository repository;

  @Inject PetReport(PetRepository repository) {
    this.repository = repository;
  }

  public Result report() {
    List<Pet> pets = repository.loadAllPets();
    Map<String, Object> args = Map.of(
      "i18n", allMessages(),
      "today", LocalDate.now(),
      "pets", pets,
      "petsCount", pets.size()
    );
    request.format = "xls";
    renderArgs.put(RA_FILENAME, "pets-report.xls");
    return new RenderExcel(Play.file("app/views/report.xls"), args, "pets-report.xls");
  }

  private Properties allMessages() {
    String lang = Lang.get(request, response);
    return Messages.all("en".equals(lang) ? "" : lang);
  }
}
