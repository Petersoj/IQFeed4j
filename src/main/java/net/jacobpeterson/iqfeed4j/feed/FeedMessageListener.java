package net.jacobpeterson.iqfeed4j.feed;

/**
 * {@link FeedMessageListener} is used to listen to feed CSV messages.
 */
public interface FeedMessageListener {

    /**
     * Called when a CSV message is received.
     *
     * @param csv the CSV
     */
    void onMessageReceived(String[] csv);
}
