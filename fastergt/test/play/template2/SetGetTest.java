package play.template2;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class SetGetTest {

    @Test
    public void testSetGet() {
        TemplateSourceRenderer r = new TemplateSourceRenderer( new GTTemplateRepoBuilder().build());

        Map<String, Object> args = new HashMap<String, Object>();

        assertThat(r.renderSrc("#{set index: 1/}#{get 'index'/}", args)).isEqualTo("1");
        assertThat(r.renderSrc("#{get 'index'}missing#{/get}", args)).isEqualTo("missing");
        assertThat(r.renderSrc("#{set index: 0/}#{get 'index'/}#{set index: index+1/}#{get 'index'/}", args)).isEqualTo("01");
        assertThat(r.renderSrc("#{set index: 'x'/}#{get 'index'/}_#{set index: index+'y'/}#{get 'index'/}", args)).isEqualTo("x_xy");

    }
}
