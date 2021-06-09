package net.jacobpeterson.iqfeed4j.feed.exception;

/**
 * {@link IQFeedRuntimeException} represents IQFeed {@link RuntimeException}s resembling an unrecoverable error due to
 * faulty program logic.
 */
public class IQFeedRuntimeException extends RuntimeException {

    /**
     * Instantiates a new {@link IQFeedRuntimeException}.
     */
    public IQFeedRuntimeException() {}

    /**
     * Instantiates a new {@link IQFeedRuntimeException}.
     *
     * @param message the message
     */
    public IQFeedRuntimeException(String message) {
        super(message);
    }

    /**
     * Instantiates a new {@link IQFeedRuntimeException}.
     *
     * @param message the message
     * @param cause   the cause
     */
    public IQFeedRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Instantiates a new {@link IQFeedRuntimeException}
     *
     * @param cause the cause
     */
    public IQFeedRuntimeException(Throwable cause) {
        super(cause);
    }
}
