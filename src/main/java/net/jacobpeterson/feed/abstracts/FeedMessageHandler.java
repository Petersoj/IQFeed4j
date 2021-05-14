package net.jacobpeterson.feed.abstracts;

/**
 * {@link FeedMessageHandler} is used to handle CSV messages.
 */
public interface FeedMessageHandler {

    /**
     * Handles a CSV message. This method should NOT block.
     *
     * @param csv the CSV message
     */
    void handleMessage(String[] csv);
}
