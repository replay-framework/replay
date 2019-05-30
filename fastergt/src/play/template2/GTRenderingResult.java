package play.template2;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;

public class GTRenderingResult {

    protected List<StringWriter> allOuts = new ArrayList<>();

    public GTRenderingResult() {
    }

    public GTRenderingResult(List<StringWriter> allOuts) {
        this.allOuts = allOuts;
    }

    public void writeOutput(OutputStream os, Charset encoding) {
        for ( StringWriter s : allOuts) {
            try {
                os.write(s.getBuffer().toString().getBytes(encoding));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * returns the rendering-output as string, but remember:
     * when dumping output to an output stream, it is better to use writeOutput()
     *
     * @return the rendering-output as string
     */
    public String getAsString() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeOutput(out, UTF_8);
        return out.toString(UTF_8);
    }

}
