package play.exceptions;

public class JavaExecutionException extends PlayExceptionWithJavaSource {

    public JavaExecutionException(Integer lineNumber, Throwable e) {
        super(e.getMessage(), e, lineNumber);
    }
    
    public JavaExecutionException(Throwable e) {
        super(e.getMessage(), e);
    }

    @Override
    public String getErrorTitle() {
        return "Execution exception";
    }

    @Override
    public String getErrorDescription() {
        return String.format("<strong>%s</strong> occurred : %s", getCause().getClass().getSimpleName(), getMessage());
    } 
}

