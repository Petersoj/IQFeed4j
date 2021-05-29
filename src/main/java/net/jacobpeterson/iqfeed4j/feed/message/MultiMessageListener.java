package net.jacobpeterson.iqfeed4j.feed.message;

/**
 * {@link MultiMessageListener} is an abstract class that represents a listener for multiple typed messages.
 *
 * @param <T> the type of the message POJO
 */
public abstract class MultiMessageListener<T> implements FeedMessageListener<T> {

    private boolean endOfMultiMessage;

    /**
     * Called when all messages have been received.
     * <br>
     * Note: be sure to call this <code>super</code> method in subclasses overriding this method.
     */
    public void onEndOfMultiMessage() {
        endOfMultiMessage = true;
    }

    /**
     * True if no more messages will be received.
     *
     * @return a boolean
     */
    public boolean isEndOfMultiMessage() {
        return endOfMultiMessage;
    }
}
