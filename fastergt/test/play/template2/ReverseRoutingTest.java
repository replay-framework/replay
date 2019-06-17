package play.template2;

import org.junit.Test;
import play.template2.compile.GTJavaBaseTesterImpl;
import play.template2.compile.GTPreCompiler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class ReverseRoutingTest {

    protected static class ReverseRoutingGTPreCompiler extends GTPreCompiler {
        public ReverseRoutingGTPreCompiler(GTTemplateRepo templateRepo) {
            super(templateRepo, null);
        }

        @Override
        protected GTFragmentCode generateRegularActionPrinter(boolean absolute, String expression, SourceContext sc, int lineNo) {
            // just print the expression since we don't have any logic to resolve routing
            return generateExpressionPrinter(expression, sc, lineNo);
        }

        @Override
        protected String checkAndPatchActionStringsInTagArguments(String tagArguments) {
            // Just fake it by removing the starting arg:@ to make it a valid expression in this test.
            int i = tagArguments.indexOf("arg:@");
            if (i >=0) {
                tagArguments = "args:"+tagArguments.substring(i+5);
            }
            return tagArguments;
        }

        @Override
        public Class<? extends GTJavaBase> getJavaBaseClass() {
            return GTJavaBaseTesterImpl.class;
        }
    }

    protected static class ReverseRoutingGTPreCompilerFactory extends GTTemplateRepoBuilder.GTPreCompilerFactoryImpl {
        @Override
        public GTPreCompiler createCompiler(GTTemplateRepo templateRepo) {
            return new ReverseRoutingGTPreCompiler(templateRepo);
        }
    }

    @Test
    public void testRegularActionPrinter() {
        TemplateSourceRenderer r = new TemplateSourceRenderer(
                new GTTemplateRepoBuilder()
                        .withPreCompilerFactory( new ReverseRoutingGTPreCompilerFactory())
                        .build());

        Map<String, Object> args = new HashMap<>();
        final ArrayList<String> list = new ArrayList<>();
        args.put("list", list);
        args.put("e", "X");

        r.renderSrc("@{list.add(e)}", args);
        assertThat(args);
        assertThat(list).contains("X");
    }

    @Test
    public void testRegularActionPrinterAsTagArgs() {
        TemplateSourceRenderer r = new TemplateSourceRenderer(
                new GTTemplateRepoBuilder()
                        .withTemplateRootFolder(new TemplateRootFolder())
                        .withPreCompilerFactory(new ReverseRoutingGTPreCompilerFactory())
                        .build());

        Map<String, Object> args = new HashMap<>();
        ArrayList<String> list = new ArrayList<>();
        args.put("list", list);
        args.put("e", "X");

        r.renderSrc("#{simpleTag @list.add(e) /}", args);
        assertThat(args);
        assertThat(list).contains("X");
    }


}
