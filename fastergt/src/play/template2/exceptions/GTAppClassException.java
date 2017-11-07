package play.template2.exceptions;

// Thrown when we detect that the exception happened inside a play app class
public class GTAppClassException extends GTException {
    
    public final String className;
    public final int lineNo;

    public GTAppClassException(String msg, Throwable throwable, String className, int lineNo) {
        super(msg, throwable);
        this.className = className;
        this.lineNo = lineNo;
    }
}
