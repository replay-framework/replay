package play.template2;

import groovy.lang.MissingPropertyException;
import groovy.lang.Script;
import play.template2.exceptions.GTException;

import javax.annotation.Nullable;
import java.io.PrintWriter;

public class GTGroovyBase extends Script {

    @Override
    public Object run() {
        throw new GTException("This method must be overridden in generated groovy script");
    }

    /**
     * All first-level property resolving is done through here
     */
    @Nullable
    @Override
    public Object getProperty(String property) {
        try {
            return super.getProperty(property);
        } catch (MissingPropertyException mpe) {
            // Just return null if not found - but check layoutData also..
            return GTJavaBase.layoutData.get().get(property); // will return null if not found
        }
    }

    protected Class<?> _resolveClass(String clazzName) {
        throw new GTException("Not implemented by default. Must be overridden by framework impl");
    }

    // Returns the correct PrintWriter right now
    private PrintWriter getPrintWriter() {
        // if someone has given us an alternative out (PrintWriter), then we must write the result to that PrintWriter.
        PrintWriter out = (PrintWriter)getProperty("out");
        if (out == null) {
            // Create PrintWriter that prints to our StringWriter
            GTJavaBase javaBase = (GTJavaBase)  getProperty("java_class");
            out = new PrintWriter(javaBase.out, true); // autoflush
        }
        return out;
    }

    @Override
    public void println() {
        getPrintWriter().println();
    }

    @Override
    public void print(Object value) {
        getPrintWriter().print(value);
    }

    @Override
    public void println(Object value) {
        getPrintWriter().println(value);
    }

    @Override
    public void printf(String format, Object value) {
        getPrintWriter().printf(format, value);
    }

    @Override
    public void printf(String format, Object[] values) {
        getPrintWriter().printf(format, values);
    }
}
