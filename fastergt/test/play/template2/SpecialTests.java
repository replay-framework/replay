package play.template2;

import org.junit.jupiter.api.Test;
import play.template2.exceptions.GTRuntimeExceptionWithSourceInfo;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class SpecialTests {
    @Test
    public void testErrorMessage_missingInclude() {

        GTTemplateRepo tr = new GTTemplateRepoBuilder()
                .withTemplateRootFolder(new TemplateRootFolder())
                .build();

        TemplateSourceRenderer sr = new TemplateSourceRenderer(tr);

        Map<String, Object> args = new HashMap<>();

        GTRuntimeExceptionWithSourceInfo e=null;
        try {
            sr.renderSrc("#{include 'missing.txt'/}", args);
        }catch(GTRuntimeExceptionWithSourceInfo _e) {
            e = _e;
        }
        
        assertThat(e).isNotNull();
        assertThat(e.getMessage()).isEqualTo("Cannot find template missing.txt");
        assertThat(e.templateLocation.relativePath).contains("_generated_key_"); // generated source without filename


        e=null;
        try {
            sr.renderSrc("#{printBody}#{include 'missing.txt'/}#{/printBody}", args);
        }catch(GTRuntimeExceptionWithSourceInfo _e) {
            e = _e;
        }

        assertThat(e).isNotNull();
        assertThat(e.getMessage()).isEqualTo("Cannot find template missing.txt");
        assertThat(e.templateLocation.relativePath).contains("_generated_key_"); // generated source without filename

    }

    @Test
    public void testStringsAndTags() {
        GTTemplateRepo tr = new GTTemplateRepoBuilder()
                .withTemplateRootFolder(new TemplateRootFolder())
                .build();

        TemplateSourceRenderer sr = new TemplateSourceRenderer(tr);

        Map<String, Object> args = new HashMap<>();

        assertThat(sr.renderSrc("a#{simpleTag '}'/}b", args)).isEqualTo("a[from tag: }]b");
        assertThat(sr.renderSrc("a#{simpleTag '\\'}'/}b", args)).isEqualTo("a[from tag: '}]b");
        assertThat(sr.renderSrc("a#{simpleTag '/}'/}b", args)).isEqualTo("a[from tag: /}]b");
        assertThat(sr.renderSrc("a#{simpleTag '#{'/}b", args)).isEqualTo("a[from tag: #{]b");

        assertThat(sr.renderSrc("a#{simpleTag \"}\"/}b", args)).isEqualTo("a[from tag: }]b");
        assertThat(sr.renderSrc("a#{simpleTag \"\\\"}\"/}b", args)).isEqualTo("a[from tag: \"}]b");
        assertThat(sr.renderSrc("a#{simpleTag \"/}\"/}b", args)).isEqualTo("a[from tag: /}]b");
        assertThat(sr.renderSrc("a#{simpleTag \"#{\"/}b", args)).isEqualTo("a[from tag: #{]b");

    }
}
