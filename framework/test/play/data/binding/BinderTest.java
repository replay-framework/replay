package play.data.binding;

import org.junit.Before;
import org.junit.Test;
import play.PlayBuilder;
import play.mvc.Http;
import play.mvc.Scope.Session;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

import static java.math.BigDecimal.TEN;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static play.mvc.Http.Request.createRequest;


public class BinderTest {

    private final Annotation[] noAnnotations = new Annotation[]{};
    private final Http.Request request = createRequest(null, "GET", "/", "", null, null, null, null, false, 80, "localhost", false, null, null);
    private final Session session = new Session();

    // provider of generic typed collection
    private static class GenericListProvider {
        private final List<Data2> listOfData2 = new ArrayList<>();
    }

    @Before
    public void setup() {
        new PlayBuilder().build();
    }

    @Test
    public void verify_and_show_how_unbind_and_bind_work() {

        Map<String, Object> r = new HashMap<>();

        Integer myInt = 12;
        Unbinder.unBind(r, myInt, "myInt", noAnnotations);
        Map<String, String[]> r2 = fromUnbindMap2BindMap(r);
        RootParamNode root = ParamNode.convert(r2);
        assertThat(Binder.bind(request, session, root, "myInt", Integer.class, null, null)).isEqualTo(myInt);
    }

    @Test
    public void verify_unbinding_and_binding_of_simple_Bean() {

        Data1 data1 = new Data1();
        data1.a = "aAaA";
        data1.b = 13;



        Map<String, Object> r = new HashMap<>();
        Data1.myStatic = 1;

        Unbinder.unBind(r, data1, "data1", noAnnotations);
        // make sure we only have info about the properties we want..
        assertThat(r.keySet()).containsOnly("data1.a", "data1.b");

        Map<String, String[]> r2 = fromUnbindMap2BindMap( r);

        Data1.myStatic = 2;
        RootParamNode root = ParamNode.convert(r2);
        Object bindResult = Binder.bind(request, session, root, "data1", Data1.class, null, null);
        assertThat(bindResult).isEqualTo(data1);
        assertThat(Data1.myStatic).isEqualTo(2);
    }


    @Test
    public void verify_unbinding_and_binding_of_nestedBeans() {

        Data2 data2 = new Data2();
        data2.a = "aaa";
        data2.b = false;
        data2.c = 12;
        
        Data1 data1_1 = new Data1();
        data1_1.a = "aAaA";
        data1_1.b = 13;

        Data1 data1_2 = new Data1();
        data1_2.a = "bBbB";
        data1_2.b = 14;
        
        data2.data1 = data1_1;
        data2.data = new ArrayList<>(2);
        data2.data.add(data1_1);
        data2.data.add(data1_2);



        Map<String, Object> r = new HashMap<>();
        Unbinder.unBind(r, data2, "data2", noAnnotations);
        Map<String, String[]> r2 = fromUnbindMap2BindMap(r);
        RootParamNode root = ParamNode.convert(r2);
        assertThat(Binder.bind(request, session, root, "data2", Data2.class, null, null)).isEqualTo(data2);

    }


     @Test
    public void verifyBindingOfStringMaps() {
        Map<String, String[]> params = new HashMap<>();

        Map<String, String> specialCaseMap = new HashMap<>();
        params.put("specialCaseMap.a", new String[] {"AA"});
        params.put("specialCaseMap.b", new String[] {"BB"});

        Data3 data3;

        params.put("data3.a", new String[] {"aAaA"});
        params.put("data3.map[abc]", new String[] {"ABC"});
        params.put("data3.map[def]", new String[] {"DEF"});

        RootParamNode rootParamNode = ParamNode.convert(params);
        specialCaseMap = (Map<String, String>)Binder.bind(request, session, rootParamNode, "specialCaseMap", specialCaseMap.getClass(), specialCaseMap.getClass(), noAnnotations);

        assertThat(specialCaseMap.size()).isEqualTo(2);
        assertThat(specialCaseMap.get("a")).isEqualTo("AA");
        assertThat(specialCaseMap.get("b")).isEqualTo("BB");

        data3 = (Data3) Binder.bind(request, session, rootParamNode, "data3", Data3.class, Data3.class, noAnnotations);

        assertThat(data3.a).isEqualTo("aAaA");
        assertThat(data3.map.size()).isEqualTo(2);
        assertThat(data3.map.get("abc")).isEqualTo("ABC");
        assertThat(data3.map.get("def")).isEqualTo("DEF");
    }

     @Test
     public void verify_binding_of_simple_bean_collections() throws NoSuchFieldException {

         Map<String, String[]> params = new HashMap<>();

         List<Data2> lst = new ArrayList<>();
         // build the parameters
         params.put("data2[0].a", new String[]{"a0"});
         params.put("data2[1].a", new String[]{"a1"});
         params.put("data2[2].a", new String[]{"a2"});
         params.put("data2[3].a", new String[]{"a3"});
         params.put("data2[4].a", new String[]{"a4"});
         params.put("data2[5].a", new String[]{"a5"});
         params.put("data2[6].a", new String[]{"a6"});
         params.put("data2[7].a", new String[]{"a7"});
         params.put("data2[8].a", new String[]{"a8"});
         params.put("data2[9].a", new String[]{"a9"});
         params.put("data2[10].a", new String[]{"a10"});
         params.put("data2[12].a", new String[]{"a12"});

         RootParamNode rootParamNode = ParamNode.convert(params);

         lst = (List<Data2>) Binder.bind(request, session, rootParamNode, "data2", lst.getClass(),
           GenericListProvider.class.getDeclaredField("listOfData2").getGenericType(), noAnnotations);
         //check the size and the order
         assertThat(lst.size()).isEqualTo(13);
         assertThat(lst.get(0).a).isEqualTo("a0");
         assertThat(lst.get(1).a).isEqualTo("a1");
         assertThat(lst.get(9).a).isEqualTo("a9");
         assertThat(lst.get(10).a).isEqualTo("a10");
         assertThat(lst.get(10).a).isEqualTo("a10");
         assertThat(lst.get(11)).isNull(); //check for null item
         assertThat(lst.get(12).a).isEqualTo("a12");
     }

    @Test
    public void verify_binding_collections_of_generic_types() {
        Map<String, String[]> params = new HashMap<>();
        params.put("data.genericTypeList", new String[]{"1", "2", "3"});

        RootParamNode rootParamNode = ParamNode.convert(params);
        Data3 result = (Data3) Binder.bind(request, session, rootParamNode, "data", Data3.class, Data3.class, noAnnotations);

        assertThat(result.genericTypeList).hasSize(3);

        for (int i = 1; i < 3; i++) {
            assertThat(result.genericTypeList.get(i - 1).value).isEqualTo(Long.valueOf(i));
        }
    }

    @Test
    public void test_unbinding_of_collection_of_complex_types() {
        Data1 d1 = new Data1();
        d1.a = "a";
        d1.b = 1;

        Data1 d2 = new Data1();
        d2.a = "b";
        d2.b = 2;

        Data1 d3 = new Data1();
        d3.a = "c";
        d3.b = 3;

        Data1[] dataArray = {d1, d2};
        List<Data1> data = asList(d2, d1, d3);

        Map<String, Data1> mapData = new HashMap<>();
        mapData.put(d1.a, d1);
        mapData.put(d2.a, d2);
        mapData.put(d3.a, d3);

        Data4 original = new Data4();
        original.s = "some";
        original.data = data;
        original.dataArray = dataArray;
        original.mapData = mapData;

        Map<String, Object> result = new HashMap<>();
        Unbinder.unBind(result, original, "data", noAnnotations);

        Map<String, String[]> r2 = fromUnbindMap2BindMap(result);
        RootParamNode root = ParamNode.convert(r2);

        Object binded = Binder.bind(request, session, root, "data", Data4.class, Data4.class, noAnnotations);
        assertThat(binded).isEqualTo(original);
    }

    @Test
    public void test_enum_set_binding() {
        Data5 data = new Data5();
        data.s = "test";
        data.testEnumSet = EnumSet.of(Data5.TestEnum.A, Data5.TestEnum.B, Data5.TestEnum.C);

        Map<String, String[]> params = new HashMap<>();
        params.put("data.testEnumSet", new String[]{"A", "B", "C"});

        RootParamNode rootParamNode = ParamNode.convert(params);

        Data5 binded = (Data5) Binder.bind(request, session, rootParamNode, "data", Data5.class, Data5.class, noAnnotations);
        assertThat(binded.testEnumSet).isEqualTo(data.testEnumSet);
    }

    @Test
    public void test_binding_class_with_private_constructor() {
        Map<String, String[]> params = new HashMap<>();
        params.put("user.name", new String[]{"john"});

        RootParamNode rootParamNode = ParamNode.convert(params);

        Data6 binded = (Data6) Binder.bind(request, session, rootParamNode, "user", Data6.class, Data6.class, noAnnotations);
        assertThat(binded.name).isEqualTo("john");
    }

    /**
     * Transforms map from Unbinder to Binder
     * @param r map filled by Unbinder
     * @return map used as input to Binder
     */
    private Map<String, String[]> fromUnbindMap2BindMap(Map<String, Object> r) {
        Map<String, String[]> r2 = new HashMap<>();
        for (Map.Entry<String, Object> e : r.entrySet()) {
            String key = e.getKey();
            Object v = e.getValue();
            System.out.println(key + " " + v + " " ) ;
            if (v instanceof String) {
                r2.put(key, new String[]{(String)v});
            } else if (v instanceof String[]) {
                r2.put(key, (String[])v);
            } else if (v instanceof Collection) {
                Object[] array = ((Collection) v).toArray();
                r2.put(key, Arrays.copyOf(array, array.length, String[].class));
            } else {
                throw new RuntimeException("error");
            }
        }
        return r2;
    }

    @Test
    public void applicationCanRegisterAndUnregisterCustomBinders() {
        Binder.register(BigDecimal.class, new MyBigDecimalBinder());
        assertNotNull(Binder.supportedTypes.get(BigDecimal.class));

        Binder.unregister(BigDecimal.class);
        assertNull(Binder.supportedTypes.get(BigDecimal.class));
    }

    private static class MyBigDecimalBinder implements TypeBinder<BigDecimal> {
        @Override
        public Object bind(Http.Request request, Session session, String name, Annotation[] annotations, String value, Class actualClass, Type genericType) {
            return new BigDecimal(value).add(TEN);
        }
    }

    @Test
    public void verify_binding_of_BigInteger() {
        Map<String, Object> r = new HashMap<>();

        BigInteger myBigInt = new BigInteger("12");
        Integer myBigIntAsInteger = 12;
        Unbinder.unBind(r, myBigIntAsInteger, "myBigInt", noAnnotations);
        Map<String, String[]> r2 = fromUnbindMap2BindMap(r);
        RootParamNode root = ParamNode.convert(r2);
        assertThat(Binder.bind(request, session, root, "myBigInt", BigInteger.class, null, null)).isEqualTo(myBigInt);
    }
}


