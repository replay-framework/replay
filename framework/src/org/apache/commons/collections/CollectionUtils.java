package org.apache.commons.collections;

import java.util.Collection;

// This is copied out of `org.apache.commons.collections`, since it is rather big and barely used.
// Probably used by one of our dependencies (that now has the `transitive = false` flag set).

@SuppressWarnings("unused") // Used indirectly, removing it makes test fail.
public class CollectionUtils {
  public static <T> boolean isEmpty(Collection<T> collection) {
    return (collection == null || collection.isEmpty());
  }

  public static <T> boolean isNotEmpty(Collection<T> collection) {
    return !isEmpty(collection);
  }
}
