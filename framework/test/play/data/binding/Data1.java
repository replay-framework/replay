package play.data.binding;

import java.util.Objects;

class Data1 {

    public static int myStatic;
    private final String f = "final";
    public String a;
    public int b;
    public void abc(Integer a) {
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Data1 data1 = (Data1) o;

        if (b != data1.b) return false;
        return Objects.equals(a, data1.a);
    }
}
