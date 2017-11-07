package play.template2.exceptions;

public class GTCompilationException extends GTException{

    public GTCompilationException() {
    }

    public GTCompilationException(String s) {
        super(s);
    }

    public GTCompilationException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public GTCompilationException(Throwable throwable) {
        super(throwable);
    }
}
