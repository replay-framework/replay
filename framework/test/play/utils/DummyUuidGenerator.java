package play.utils;

public class DummyUuidGenerator extends UuidGenerator {
  private final String uuid;
  public DummyUuidGenerator(String uuid) {
    this.uuid = uuid;
  }

  @Override public String randomUUID() {
    return uuid;
  }
}
