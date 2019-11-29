package play.mvc;

import play.libs.Codec;

class ReadonlySession extends Scope.Session {
  @Override void change() {
    throw new IllegalStateException("This is read-only session");
  }

  ReadonlySession() {
    super(Codec.UUID());
  }
}
