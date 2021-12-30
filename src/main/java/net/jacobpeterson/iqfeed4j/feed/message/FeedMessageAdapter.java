package net.jacobpeterson.iqfeed4j.feed.message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link FeedMessageAdapter} is an adapter for {@link FeedMessageListener}
 */
public class FeedMessageAdapter<T> implements FeedMessageListener<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeedMessageAdapter.class);

    @Override
    public void onMessageReceived(T message) {
        LOGGER.info("{}", message);
    }
}
