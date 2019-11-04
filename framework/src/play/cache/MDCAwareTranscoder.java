package play.cache;

import net.spy.memcached.transcoders.SerializingTranscoder;
import org.slf4j.MDC;

import javax.annotation.Nullable;

class MDCAwareTranscoder extends SerializingTranscoder {
  private final MemcachedTranscoder delegate;
  private final String mdcParameterName;
  private final String mdcParameterValue;

  MDCAwareTranscoder(MemcachedTranscoder delegate, String mdcParameterName, String mdcParameterValue) {
    this.delegate = delegate;
    this.mdcParameterName = mdcParameterName;
    this.mdcParameterValue = mdcParameterValue;
  }

  @Nullable
  @Override
  protected Object deserialize(byte[] data) {
    String originalMdcParameterValue = MDC.get(mdcParameterName);
    MDC.put(mdcParameterName, mdcParameterValue);

    try {
      return delegate.deserialize(data);
    }
    finally {
      if (originalMdcParameterValue == null)
        MDC.remove(mdcParameterName);
      else
        MDC.put(mdcParameterName, originalMdcParameterValue);
    }
  }

  @Nullable
  @Override
  protected byte[] serialize(Object object) {
    return delegate.serialize(object);
  }
}
