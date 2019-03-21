package play.templates;

import java.util.UUID;

class UuidGenerator {
  public String randomUUID() {
    return UUID.randomUUID().toString();
  }
}
