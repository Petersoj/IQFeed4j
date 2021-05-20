package net.jacobpeterson.iqfeed4j.feed;

/**
 * {@link MultiMessageListener} is an abstract class that represents a listener for multiple typed messages.
 *
 * @param <T> the type of the message POJO
 */
public abstract class MultiMessageListener<T> {

    private boolean endOfMessages;

    /**
     * Called when a message is received. Note: This method should never block!
     *
     * @param message the message
     */
    protected abstract void onMessageReceived(T message);

    /**
     * Called when a message {@link Exception} has occurred.
     *
     * @param exception the {@link Exception}
     */
    protected abstract void onMessageException(Exception exception);

    /**
     * Called when all messages have been received.
     * <br>
     * Note: be sure to call this <code>super</code> method in subclasses overriding this method.
     */
    protected void onEndOfMessages() {
        endOfMessages = true;
    }

    /**
     * True if no more messages will be received.
     *
     * @return a boolean
     */
    public boolean isEndOfMessages() {
        return endOfMessages;
    }
}
