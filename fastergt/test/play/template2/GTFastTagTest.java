package play.template2;

import org.junit.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class GTFastTagTest {
    @Test
    public void testResolveFastTag() {
        GTFastTag ft = new TestTags();
        assertThat(ft.resolveFastTag("tag1")).isEqualTo("play.template2.TestTags.tag_tag1");
        assertThat(ft.resolveFastTag("tag2")).isNull();

        ft = new TestTags2();
        assertThat(ft.resolveFastTag("a.tag1")).isEqualTo("play.template2.TestTags2.tag_tag1");
        assertThat(ft.resolveFastTag("tag1")).isNull();
        assertThat(ft.resolveFastTag("a.b.tag1")).isNull();

        ft = new TestTags3();
        assertThat(ft.resolveFastTag("a.b.tag1")).isEqualTo("play.template2.TestTags3.tag_tag1");
        assertThat(ft.resolveFastTag("tag1")).isNull();
        assertThat(ft.resolveFastTag("a.tag1")).isNull();
    }

}

class TestTags extends GTFastTag {

    public static void tag_tag1(GTJavaBase template, Map<String, Object> args, GTContentRenderer content ) {
        template.out.append("[testFastTag before]");
        template.insertOutput( content.render());
        template.out.append("[from testFastTag after]");
    }
}

@GTFastTag.TagNamespace("a")
class TestTags2 extends GTFastTag {

    public static void tag_tag1(GTJavaBase template, Map<String, Object> args, GTContentRenderer content ) {
        template.out.append("[testFastTag before]");
        template.insertOutput( content.render());
        template.out.append("[from testFastTag after]");
    }
}

@GTFastTag.TagNamespace("a.b")
class TestTags3 extends GTFastTag {

    public static void tag_tag1(GTJavaBase template, Map<String, Object> args, GTContentRenderer content ) {
        template.out.append("[testFastTag before]");
        template.insertOutput( content.render());
        template.out.append("[from testFastTag after]");
    }
}