package play.template2;

import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.NoSuchElementException;

class ArrayIterator<T> implements Iterator<T> {
  private final Object array;
  private final int endIndex;
  private int index;

  ArrayIterator(final Object array) {
    this.array = array;
    this.endIndex = Array.getLength(array);
    this.index = 0;
  }

  @Override public boolean hasNext() {
    return index < endIndex;
  }

  @Override public T next() {
    if (!hasNext()) {
      throw new NoSuchElementException();
    }
    return (T) Array.get(array, index++);
  }

  @Override public void remove() {
    throw new UnsupportedOperationException("remove() method is not supported");
  }
}
