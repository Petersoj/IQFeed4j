package net.jacobpeterson.iqfeed4j.feed.exception;

/**
 * {@link IQFeedException} represents IQFeed {@link Exception}s resembling a recoverable error due to an issue outside
 * the control of this program.
 */
public class IQFeedException extends Exception {

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

    /**
     * Instantiates a new {@link IQFeedException}
     *
     * @param cause the cause
     */
    public IQFeedException(Throwable cause) {
        super(cause);
    }
}
