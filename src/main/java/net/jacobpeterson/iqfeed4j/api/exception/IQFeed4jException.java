package net.jacobpeterson.iqfeed4j.api.exception;

/**
 * {@link IQFeed4jException} represents an IQFeed4j {@link Exception}.
 */
public class IQFeed4jException extends Exception {

    /**
     * Instantiates a new {@link IQFeed4jException}.
     */
    public IQFeed4jException() {}

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
}
