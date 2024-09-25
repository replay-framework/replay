package org.apache.commons.collections;

// This is copied out of `org.apache.commons.collections`, since it is rather big and barely used.
// Probably used by one of our dependencies (that now has the `transitive = false` flag set).

@SuppressWarnings("unused") // Used indirectly, removing it makes test fail.
public interface Predicate {
  boolean evaluate(Object object);
}
