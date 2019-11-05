package play.exceptions;

public class UnexpectedException extends PlayException {
    
    public UnexpectedException(String message) {
        super(message);
    }

    public UnexpectedException(Throwable exception) {
        super(exception);
    }
    
    public UnexpectedException(String message, Throwable cause) {
        super(message, cause);
    }
}

