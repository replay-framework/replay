package play.template2.exceptions;

public class GTException extends RuntimeException{

    public GTException() {
    }

    public GTException(String s) {
        super(s);
    }

    public GTException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public GTException(Throwable throwable) {
        super(throwable);
    }
}
