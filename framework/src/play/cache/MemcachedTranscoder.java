package play.cache;

import net.spy.memcached.transcoders.SerializingTranscoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

class MemcachedTranscoder extends SerializingTranscoder {
  private static final Logger logger = LoggerFactory.getLogger(MemcachedTranscoder.class);

  @Nullable
  @Override
  protected Object deserialize(byte[] data) {
    try (ObjectInputStream in = new PlayObjectInputStream(data)) {
      return in.readObject();
    }
    catch (Exception e) {
      logger.error("Could not deserialize", e);
      return null;
    }
  }

  @Nullable
  @Override
  protected byte[] serialize(Object object) {
    try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
      try (ObjectOutputStream oos = new ObjectOutputStream(bos)) {
        oos.writeObject(object);
        return bos.toByteArray();
      }
    }
    catch (IOException e) {
      logger.error("Could not serialize", e);
    }
    return null;
  }
}
