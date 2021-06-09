package net.jacobpeterson.iqfeed4j.feed.message;

import net.jacobpeterson.iqfeed4j.feed.AbstractFeed;

/**
 * {@link MultiMessageListener} is an abstract class that represents a listener for multiple typed messages.
 *
 * @param <T> the type of the message POJO
 */
public abstract class MultiMessageListener<T> implements FeedMessageListener<T> {

    private boolean endOfMultiMessage;

    /**
     * Called when all messages have been received.
     */
    public abstract void onEndOfMultiMessage();

    /**
     * This is called internally by a {@link AbstractFeed} for when all messages have been received.
     */
    public final void handleEndOfMultiMessage() {
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
