package play.template2;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class DefInScriptsRemoverTest {
    
    @Test
    public void testIt() {
        TemplateSourceRenderer r = new TemplateSourceRenderer( new GTTemplateRepoBuilder().build());
        
        Map<String, Object> args = new HashMap<>();
        args.put("myData", "xxx");

        assertThat(r.renderSrc("%{ a = 'x1'; b = 'y1' }%${a}:${b}", args)).isEqualTo("x1:y1");
        assertThat(r.renderSrc("%{ def a = 'x2'; b = 'y2' }%${a}:${b}", args)).isEqualTo("x2:y2");
        assertThat(r.renderSrc("%{ if(true){def a = 'x3'}; b = 'y3' }%${a}:${b}", args)).isEqualTo("x3:y3");

        assertThat(r.renderSrc("%{ String a = 'x4'; b = 'y4' }%${a}:${b}", args)).isEqualTo("x4:y4");
        assertThat(r.renderSrc("%{ if(true){String a = 'x5'}; b = 'y5' }%${a}:${b}", args)).isEqualTo("x5:y5");

        assertThat(r.renderSrc("%{ Integer a = 1; b = 'y6' }%${a}:${b}", args)).isEqualTo("1:y6");
        assertThat(r.renderSrc("%{ if(true){Long a = 2}; b = 'y7' }%${a}:${b}", args)).isEqualTo("2:y7");


    }
}
