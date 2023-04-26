package play.data.binding;

import org.junit.jupiter.api.Test;
import play.data.validation.ValidationBuilder;
import play.mvc.Http.Request;
import play.mvc.Scope.Session;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static play.mvc.Http.Request.createRequest;

public class BeanWrapperTest {
    private static class Bean {
        public String a = "a";
        public String b = "b";
        int i = 1;

        public String getA() {
            return a;
        }

        public void setA(String a) {
            this.a = a;
        }

        public String getB() {
            return b;
        }

        public void setB(String b) {
            this.b = b;
        }

        public int getI() {
            return i;
        }

        public void setI(int i) {
            this.i = i;
        }
    }

    @Test
    public void testBind() {
        Request request = createRequest(null, "GET", "/", "", null, null, null, null, false, 80, "localhost", false, null, null);
        Session session = new Session();

        ValidationBuilder.build();
        Map<String, String[]> m = new HashMap<>();
        m.put("b.a", new String[]{"a1"});
        m.put("b.b", new String[]{"b1"});
        m.put("b.i", new String[]{"2"});

        Bean b = new Bean();
        new BeanWrapper(Bean.class).bind(request, session, "b", m, "", b, null);
        assertThat(b.a).isEqualTo("a1");
        assertThat(b.b).isEqualTo("b1");
        assertThat(b.i).isEqualTo(2);

        b = new Bean();
        new BeanWrapper(Bean.class).bind(request, session, "", m, "b", b, null);
        assertThat(b.a).isEqualTo("a1");
        assertThat(b.b).isEqualTo("b1");
        assertThat(b.i).isEqualTo(2);
    }
}
