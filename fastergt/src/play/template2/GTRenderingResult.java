package play.template2;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

public class GTRenderingResult {

    protected List<StringWriter> allOuts = new ArrayList<>();

    public GTRenderingResult() {
    }

    public GTRenderingResult(List<StringWriter> allOuts) {
        this.allOuts = allOuts;
    }

    public void writeOutput(OutputStream os, String encoding) {
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
        writeOutput(out, "utf-8");
        try {
            return out.toString("utf-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

}
