package net.jacobpeterson.iqfeed4j.util.exception;

/**
 * {@link AsyncExceptionListener} allows for the handling of {@link Exception}s asynchronously.
 */
public interface AsyncExceptionListener {

    /**
     * Called when an asynchronous {@link Exception} has occurred.
     *
     * @param message   the message
     * @param exception the {@link Exception}
     */
    void onAsyncException(String message, Exception exception);
}
