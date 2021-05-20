package net.jacobpeterson.iqfeed4j.feed;

/**
 * {@link FeedMessageListener} is used to listen to feed CSV messages.
 */
public interface FeedMessageListener {

    /**
     * Called when a CSV message is received. Note: This method should NEVER block and should NEVER throw an exception!
     *
     * @param csv the CSV
     */
    void onMessageReceived(String[] csv);
}
