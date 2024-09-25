package org.apache.commons.collections;

import java.util.concurrent.ConcurrentHashMap;

// This class is copied out of `org.apache.commons.collections` as it is used by `commons-beanutils`
// which has the `transitive = false` flag set.
// This since commons-collections is rather big and was barely used.

@SuppressWarnings("unused") // Used indirectly, removing it makes test fail.
public class FastHashMap<K, V> extends ConcurrentHashMap<K, V> {
  public void setFast(boolean fast) {}
}
