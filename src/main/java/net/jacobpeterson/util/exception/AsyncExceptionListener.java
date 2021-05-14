package net.jacobpeterson.util.exception;

public interface AsyncExceptionListener {

    /**
     * Called when an asynchronous {@link Exception} has occurred.
     *
     * @param message   the message
     * @param exception the {@link Exception}
     */
    void onAsyncException(String message, Exception exception);
}
