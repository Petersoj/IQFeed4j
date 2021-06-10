package net.jacobpeterson.iqfeed4j.api.exception;

/**
 * {@link IQFeed4jException} represents IQFeed4j {@link Exception}s. Use {@link #getCause()} to get the {@link
 * Exception} cause, if one exists.
 */
public class IQFeed4jException extends Exception {

    /**
     * Instantiates a new {@link IQFeed4jException}.
     *
     * @param message the message
     */
    public IQFeed4jException(String message) {
        super(message);
    }

    /**
     * Instantiates a new {@link IQFeed4jException}.
     *
     * @param message the message
     * @param cause   the cause
     */
    public IQFeed4jException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Instantiates a new {@link IQFeed4jException}
     *
     * @param cause the cause
     */
    public IQFeed4jException(Throwable cause) {
        super(cause);
    }
}
