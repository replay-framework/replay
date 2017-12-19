package play.exceptions;

import java.util.concurrent.atomic.AtomicLong;

/**
 * The super class for all Play! exceptions
 */
public class PlayException extends RuntimeException {

    private static final AtomicLong atomicLong = new AtomicLong(System.currentTimeMillis());
    private String id;

    public PlayException() {
        setId();
    }

    public PlayException(String message) {
        super(message);
        setId();
    }

    public PlayException(Throwable cause) {
        super(cause);
        setId();
    }

    public PlayException(String message, Throwable cause) {
        super(message, cause);
        setId();
    }

    void setId() {
        long nid = atomicLong.incrementAndGet();
        id = Long.toString(nid, 26);
    }

    public String getId() {
        return id;
    }
}