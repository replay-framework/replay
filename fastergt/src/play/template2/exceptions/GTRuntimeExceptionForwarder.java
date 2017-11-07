package play.template2.exceptions;

/**
 * Exception used to forward an exception - the stacktrace of 'this' exception is not important
 */
public class GTRuntimeExceptionForwarder extends GTRuntimeException {

    public GTRuntimeExceptionForwarder(Throwable throwable) {
        super(throwable);
    }
}
