package play.template2;

import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class GTFastTagTest {
    @Test
    public void testResolveFastTag() {
        GTFastTag ft = new TestTags();
        assertEquals( "play.template2.TestTags.tag_tag1", ft.resolveFastTag("tag1"));
        assertNull( ft.resolveFastTag("tag2"));

        ft = new TestTags2();
        assertEquals( "play.template2.TestTags2.tag_tag1", ft.resolveFastTag("a.tag1"));
        assertNull( ft.resolveFastTag("tag1"));
        assertNull( ft.resolveFastTag("a.b.tag1"));

        ft = new TestTags3();
        assertEquals( "play.template2.TestTags3.tag_tag1", ft.resolveFastTag("a.b.tag1"));
        assertNull( ft.resolveFastTag("tag1"));
        assertNull( ft.resolveFastTag("a.tag1"));
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