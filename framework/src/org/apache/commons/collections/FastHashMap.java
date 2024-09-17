package org.apache.commons.collections;

import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("unused") // Used indirectly, removing it makes test fail.
public class FastHashMap<K, V> extends ConcurrentHashMap<K, V> {
  public void setFast(boolean fast) {}
}
