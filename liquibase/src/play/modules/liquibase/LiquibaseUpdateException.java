package play.modules.liquibase;

import play.exceptions.PlayException;

public class LiquibaseUpdateException extends PlayException {

  @Override public String getErrorTitle() {
    return "Liquibase Error";
  }

  @Override
  public String getErrorDescription() {
    return String.format("A DBUpdate error occurred (%s): <strong>%s</strong>",
        getMessage(), getCause() == null ? "" : getCause().getMessage());
  }

  public LiquibaseUpdateException(String message) {
    super(message, null);
  }

  public LiquibaseUpdateException(String message, Throwable cause) {
    super(message, cause);
  }

}
