package play.template2;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;

public class TagFileSpacesTest {

    @Test
    public void testSpaces() {
        // first try with unix line feeds
        GTTemplateRepo tr = new GTTemplateRepoBuilder()
                .withTemplateRootFolder(new TemplateRootFolder())
                .build();

        TemplateSourceRenderer sr = new TemplateSourceRenderer(tr);

        doLineFeedTests(sr);

        // then test with windows line feeds
        tr = new GTTemplateRepoBuilder()
                .withTemplateRootFolder(new TemplateRootFolder())
                .withFakeWindowsNewLines(true)
                .build();

        sr = new TemplateSourceRenderer(tr);

        doLineFeedTests(sr);
    }

    private void doLineFeedTests(TemplateSourceRenderer sr) {
        Map<String, Object> args = new HashMap<>();
        assertThat( sr.renderSrc("beforeTag-#{simpleTag 'data'/}-afterTag", args) ).isEqualTo("beforeTag-[from tag: data]-afterTag");
        assertThat( sr.renderSrc("beforeTag-#{tagWithScriptBlock 'data'/}-afterTag", args) ).isEqualTo("beforeTag-[from tag: data]-afterTag");
        assertThat( sr.renderSrc("beforeTag-#{tagWithScriptBlockAndComment 'data'/}-afterTag", args) ).isEqualTo("beforeTag-[from tag: data]-afterTag");
    }
}
