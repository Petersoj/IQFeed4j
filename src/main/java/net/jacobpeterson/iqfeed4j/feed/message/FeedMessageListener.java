package net.jacobpeterson.iqfeed4j.feed.message;

import net.jacobpeterson.iqfeed4j.feed.AbstractFeed;

/**
 * {@link FeedMessageListener} is used to listen to feed messages.
 * <br>
 * <strong>Calling methods in an {@link AbstractFeed} implementation that send feed messages will result in dead-lock!
 * Use a separate thread as needed.</strong>
 *
 * @param <T> the type of message
 */
@FunctionalInterface
public interface FeedMessageListener<T> {

    /**
     * Called when a message is received. <strong>This method should NEVER throw an {@link Exception}!</strong>
     * <br>
     * Note that synchronously consuming data via this method will block the underlying feed if the data is not consumed
     * fast enough.
     *
     * @param message the message
     */
    void onMessageReceived(T message);

    /**
     * Called when a message {@link Exception} has occurred.
     *
     * @param exception the {@link Exception}
     */
    default void onMessageException(Exception exception) {
        exception.printStackTrace();
    }
}
