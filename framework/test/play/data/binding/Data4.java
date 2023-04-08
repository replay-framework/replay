package play.data.binding;

import java.util.List;
import java.util.Map;
import java.util.Objects;

class Data4 {

    public String s;
    public List<Data1> data;
    public Data1[] dataArray;
    public Map<String, Data1> mapData;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Data4 data4 = (Data4) o;

        if (!Objects.equals(data, data4.data)) return false;
        //asList to ignore sequence of elements. It's not mandatory in binder
        if (data != null && !List.of(data).equals(List.of(data4.data))) return false;
        if (!Objects.equals(s, data4.s)) return false;
        return Objects.equals(mapData, data4.mapData);
    }
}
