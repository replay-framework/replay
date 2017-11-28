package play.exceptions;

public abstract class PlayExceptionWithJavaSource extends PlayException implements SourceAttachment {
    Integer line;

    protected PlayExceptionWithJavaSource(String message) {
        super(message);
    }

    protected PlayExceptionWithJavaSource(String message, Throwable cause, Integer line) {
        super(message, cause);
        this.line = line;
    }

    protected PlayExceptionWithJavaSource(String message, Throwable cause) {
        super(message, cause);

        StackTraceElement element = getInterestingStackTraceElement(cause);
        if (element != null) {
            line = element.getLineNumber();
        }
    }

    @Override
    public Integer getLineNumber() {
        return line;
    }
}
