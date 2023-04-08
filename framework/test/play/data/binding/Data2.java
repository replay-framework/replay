package play.data.binding;

import java.util.List;
import java.util.Objects;

class Data2 {
    public String a;
    public Boolean b;
    public int c;

    /**
     * Tried first with arrays and lists but the Unbinder fails in such situations.
     */

    public Data1 data1;
    public List<Data1> data;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Data2 data2 = (Data2) o;

        if (c != data2.c) return false;
        if (!Objects.equals(a, data2.a)) return false;
        if (!Objects.equals(b, data2.b)) return false;
        if (!Objects.equals(data1, data2.data1)) return false;
        return Objects.equals(data, data2.data);
    }
}
