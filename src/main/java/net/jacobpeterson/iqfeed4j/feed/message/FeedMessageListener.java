package net.jacobpeterson.iqfeed4j.feed.message;

/**
 * {@link FeedMessageListener} is used to listen to feed messages.
 *
 * @param <T> the type of message
 */
public interface FeedMessageListener<T> {

    /**
     * Called when a message is received. Note: This method should NEVER block and should NEVER throw an exception!
     *
     * @param message the message
     */
    void onMessageReceived(T message);

    /**
     * Called when a message {@link Exception} has occurred.
     *
     * @param exception the {@link Exception}
     */
    void onMessageException(Exception exception);
}
