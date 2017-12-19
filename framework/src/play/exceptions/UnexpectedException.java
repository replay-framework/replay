package play.exceptions;

public class UnexpectedException extends PlayException {
    
    public UnexpectedException(String message) {
        super(message);
    }

    public UnexpectedException(Throwable exception) {
        super("Unexpected Error", exception);
    }
    
    public UnexpectedException(String message, Throwable cause) {
        super(message, cause);
    }
}

