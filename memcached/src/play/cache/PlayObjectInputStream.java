package play.cache;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;

class PlayObjectInputStream extends ObjectInputStream {
  PlayObjectInputStream(byte[] data) throws IOException {
    super(new ByteArrayInputStream(data));
  }

  @Override
  protected Class<?> resolveClass(ObjectStreamClass desc) throws ClassNotFoundException {
    return Class.forName(desc.getName(), false, Thread.currentThread().getContextClassLoader());
  }
}
