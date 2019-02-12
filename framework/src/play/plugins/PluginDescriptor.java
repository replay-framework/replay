package play.plugins;

import java.net.URL;
import java.util.Objects;

import static java.util.Objects.hash;

class PluginDescriptor implements Comparable<PluginDescriptor> {
  public final String name;
  public final int index;
  public final URL url;

  PluginDescriptor(String name, int index, URL url) {
    this.name = name;
    this.index = index;
    this.url = url;
  }

  @Override
  public String toString() {
    return String.format("PluginDescriptor{name='%s', index=%s, url=%s}", name, index, url);
  }

  @Override
  public int compareTo(PluginDescriptor o) {
    int res = Integer.compare(index, o.index);
    if (res != 0) {
      return res;
    }

    // Index is equal in both plugins.
    // sort on name to get consistent order
    return name.compareTo(o.name);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    PluginDescriptor that = (PluginDescriptor) o;
    return Objects.equals(index, that.index) && Objects.equals(name, that.name);
  }

  @Override
  public int hashCode() {
    return hash(name, index);
  }
}