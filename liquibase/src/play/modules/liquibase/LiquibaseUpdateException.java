package play.modules.liquibase;

import play.exceptions.PlayException;

public class LiquibaseUpdateException extends PlayException {
  public LiquibaseUpdateException(String message) {
    super(message);
  }

  public LiquibaseUpdateException(String message, Throwable cause) {
    super(message, cause);
  }

}
