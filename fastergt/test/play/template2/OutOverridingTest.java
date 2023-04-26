package play.template2;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class OutOverridingTest {

    @Test
    public void testLegacyOutOverriding() {
        TemplateSourceRenderer sr = new TemplateSourceRenderer( new GTTemplateRepoBuilder().build());

        Map<String, Object> args = new HashMap<>();
        args.put("myData", "xxx");

        assertThat( sr.renderSrc("a%{ print 'b' }%c", args) ).isEqualTo("abc");
        assertThat( sr.renderSrc("a%{ println('b') }%c", args) ).isEqualTo("ab\nc");
        assertThat( sr.renderSrc("a%{ printf('%d',1) }%c", args) ).isEqualTo("a1c");


    }
}
