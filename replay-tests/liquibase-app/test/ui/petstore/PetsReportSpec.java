package ui.petstore;

import model.Kind;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.*;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.*;
import static org.assertj.core.api.Assertions.assertThat;

public class PetsReportSpec extends BaseSpec {

  @Test
  public void canRegisterNewPet() throws IOException, InvalidFormatException {
    open("/");
    $$("#pets .pet").shouldHave(size(0));
    $("#buttonRegisterPet")
      .shouldHave(text("Register new pet"))
      .click();

    PetRegistrationPage page = page();
    page.registerPet(Kind.COW, "Muuuuusie", 2);

    $$("#pets .pet").shouldHave(size(1));
    $("#totalCount").shouldHave(text("Total count: 1"));
    
    // TODO generate new pets with API call
    File report = $("#buttonReport").download();
    verifyReportFile(report);
  }

  private void verifyReportFile(File report) throws IOException, InvalidFormatException {
    assertThat(report.getName()).isEqualTo("pets-report.xls");

    try (Workbook xls = WorkbookFactory.create(report)) {
      assertThat(xls.getNumberOfSheets()).isEqualTo(1);
      var cells = cellTexts(xls.getSheetAt(0));
      assertThat(cells).contains("Pets store report", "Muuuuusie", "Total count: 1");
    }

  }

  private List<String> cellTexts(Sheet sheet) {
    List<String> cells = new ArrayList<>();
    for (Row row : sheet) {
      for (Cell cell : row) {
        cells.add(cell.toString());
      }
    }
    return cells;
  }

}