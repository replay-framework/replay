package play.template2;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;

public class VariableVisabilityTest {

    @Test
    public void testThatVariablesNotLeakbetweenScriptRuns() {
        TemplateSourceRenderer r = new TemplateSourceRenderer( new GTTemplateRepoBuilder().build());
        
        Map<String, Object> args = new HashMap<>();
        args.put("myData", "xxx");

        assertThat(r.renderSrc("${a}", args)).isEqualTo("");
        assertThat(r.renderSrc("%{a='x'}%${a}", args)).isEqualTo("x");
        assertThat(r.renderSrc("${a}", args)).isEqualTo("");
        
    }

    @Test
    public void testThatTagArgsToFirstTagIsNotAvailableInCalledTag() {
        GTTemplateRepo tr = new GTTemplateRepoBuilder()
                .withTemplateRootFolder(new TemplateRootFolder())
                .build();
        TemplateSourceRenderer sr = new TemplateSourceRenderer(tr);
        Map<String, Object> args = new HashMap<>();
        assertThat( sr.renderSrc("#{tagUsingSimpleTag 'data'/}", args) ).isEqualTo("data[from tag: ][from tag: argSpecified]");



    }
}
