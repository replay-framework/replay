package play.cache;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import com.google.errorprone.annotations.CheckReturnValue;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import net.spy.memcached.transcoders.SerializingTranscoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NullMarked
@CheckReturnValue
class MemcachedTranscoder extends SerializingTranscoder {
  private static final Logger logger = LoggerFactory.getLogger(MemcachedTranscoder.class);

  @Nullable
  @Override
  protected Object deserialize(byte[] data) {
    try (ObjectInputStream in = new PlayObjectInputStream(data)) {
      return in.readObject();
    } catch (Exception e) {
      logger.error("Could not deserialize", e);
      return null;
    }
  }

  @Override
  protected byte @Nullable [] serialize(Object object) {
    try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
      try (ObjectOutputStream oos = new ObjectOutputStream(bos)) {
        oos.writeObject(object);
        return bos.toByteArray();
      }
    } catch (IOException e) {
      logger.error("Could not serialize", e);
    }
    return null;
  }
}
