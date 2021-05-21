package net.jacobpeterson.iqfeed4j.util.exception;

/**
 * {@link IQFeedException} represents IQFeed {@link RuntimeException}s.
 */
public class IQFeedException extends RuntimeException {

    /**
     * Instantiates a new {@link IQFeedException}.
     */
    public IQFeedException() {}

    /**
     * Instantiates a new {@link IQFeedException}.
     *
     * @param message the message
     */
    public IQFeedException(String message) {
        super(message);
    }

    /**
     * Instantiates a new {@link IQFeedException}.
     *
     * @param message the message
     * @param cause   the cause
     */
    public IQFeedException(String message, Throwable cause) {
        super(message, cause);
    }
}
