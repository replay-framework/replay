package play.modules.liquibase;

public enum LiquibaseAction {
  UPDATE,
  SYNC,
  LISTLOCKS,
  RELEASELOCKS,
  STATUS,
  VALIDATE,
  CLEARCHECKSUMS,
  DROPALL,
}
