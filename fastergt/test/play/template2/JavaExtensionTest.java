package play.template2;

import groovy.lang.GroovyObjectSupport;
import org.junit.Test;
import play.template2.compile.GTJavaExtensionMethodResolver;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class JavaExtensionTest {

    private TemplateSourceRenderer createSourceRenderer() {
        TemplateSourceRenderer sr = new TemplateSourceRenderer( new GTTemplateRepoBuilder()
                .withTemplateRootFolder(new TemplateRootFolder())
                .withGTJavaExtensionMethodResolver(methodName -> {
                    for (Method m : SimpleJavaExtensions.class.getDeclaredMethods()) {
                        if (m.getName().equals(methodName)) {
                            return SimpleJavaExtensions.class;
                        }
                    }
                    return null;
                })
                .build());
        return sr;
    }

    @Test
    public void testJavaExtensions() {
        TemplateSourceRenderer sr = createSourceRenderer();

        Map<String, Object> args = new HashMap<String, Object>();
        args.put("a", "abc");
        
        Map<String, String> myMap = new HashMap<String, String>();
        myMap.put("a", "b");
        args.put("myMap", myMap);

        assertThat(sr.renderSrc("${a.addToString('x')}", args)).isEqualTo("abcx");
        // test that special toString for map in groovy is invoked even if we have
        // a toString (with different type in uur JavaExtensions-class).
        // It is a problem if the default java-toString method gets invoked.
        // Groovy prints: '[a:b]' and java prints '{a=b}'
        assertThat( sr.renderSrc("${myMap.toString()}", args) ).isEqualTo("[a:b]");

        // Make sure JavaExtension method is called when object does not have the method itself
        Foo1 foo = new Foo1();
        foo.inner = "X";
        args.put("foo", foo);
        assertThat(sr.renderSrc("${foo.returnInner()}", args)).isEqualTo("X");

        // Make sure objects own method is used if it exists
        foo = new Foo2();
        foo.inner = "X";
        args.put("foo", foo);
        assertThat(sr.renderSrc("${foo.returnInner()}", args)).isEqualTo("Foo2Inner: X");
        
        foo = new Foo1();
        foo.inner = "a";
        args.put("foo", foo);
        Foo1 foo2 = new Foo1();
        foo2.inner = "b";
        args.put("foo2", foo2);
        assertThat(sr.renderSrc("${foo.fooConcat(foo2)}", args)).isEqualTo("ab");

        // test "inteligent" args-type resolving when invoking real-none-JavaExtension-method
        foo = new Foo2();
        foo.inner = "x";
        args.put("foo", foo);
        foo2 = new Foo2();
        foo2.inner = "y";
        args.put("foo2", foo2);
        assertThat(sr.renderSrc("${foo.fooConcat(foo2)}", args)).isEqualTo("Foo2: xy");

    }

    @Test
    public void testStaticNameClashProblem() {
        TemplateSourceRenderer sr = createSourceRenderer();

        Map<String, Object> args = new HashMap<String, Object>();
        args.put("a", "ab&c");
        assertThat(sr.renderSrc("${org.apache.commons.lang.StringEscapeUtils.escapeHtml(a)}", args)).isEqualTo("ab&amp;c");

    }

    public static class Foo1 {
        public String inner;
    }

    public static class Foo2 extends Foo1 {

        public String returnInner() {
            return "Foo2Inner: " + inner;
        }

        public String fooConcat(Foo1 other) {
            return "Foo2: " + inner + other.inner;
        }
    }

    @Test
    public void testStrangeFirstTimeGroovyObjectUsageParamsIssue() {
        TemplateSourceRenderer sr = createSourceRenderer();
        Map<String, Object> args = new HashMap<String, Object>();
        args.put("id",1);
        args.put("o", new MyGroovyObject());
        // Do it one time
        assertThat(sr.renderSrc("${o.methodNameThatCanCollideWithGroovyObjectSupport(id)}", args)).isEqualTo("method:methodNameThatCanCollideWithGroovyObjectSupport-arg:1");
        // Do it again
        assertThat(sr.renderSrc("${o.methodNameThatCanCollideWithGroovyObjectSupport(id)}", args)).isEqualTo("method:methodNameThatCanCollideWithGroovyObjectSupport-arg:1");
        // Do it twice in the same template
        assertThat(sr.renderSrc("${o.methodNameThatCanCollideWithGroovyObjectSupport(id)} - ${o.methodNameThatCanCollideWithGroovyObjectSupport(id)}", args))
                .isEqualTo("method:methodNameThatCanCollideWithGroovyObjectSupport-arg:1 - method:methodNameThatCanCollideWithGroovyObjectSupport-arg:1");

    }
}


class SimpleJavaExtensions {
    
    public static String addToString(String s, String stuffToAdd) {
        return s + stuffToAdd;
    }
    
    public static String returnInner(JavaExtensionTest.Foo1 foo) {
        return foo.inner;
    }
    
    public static String toString(BigDecimal n) {
        throw new RuntimeException("Should not get invoked");
    }
    
    public static String fooConcat(JavaExtensionTest.Foo1 foo, JavaExtensionTest.Foo1 other) {
        return foo.inner + other.inner;
    }
    
    public static String escapeHtml(String s) {
        return "JE.escapeHtml:"+s;
    }

    public static String methodNameThatCanCollideWithGroovyObjectSupport() {
        return "";
    }
}

class MyGroovyObject extends GroovyObjectSupport {

    @Override
    public Object invokeMethod(String name, Object args) {
        Object[] a = (Object[])args;
        return "method:"+name+"-arg:"+a[0];
    }

    @Override
    public Object getProperty(String property) {
        return new MyGroovyObject();
    }
}