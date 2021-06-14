package net.jacobpeterson.iqfeed4j.feed.message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link MultiMessageAdapter} is an adapter for {@link MultiMessageListener}
 */
public class MultiMessageAdapter<T> extends MultiMessageListener<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MultiMessageAdapter.class);

    @Override
    public void onMessageReceived(T message) {
        LOGGER.info("{}", message);
    }

    @Override
    public void onMessageException(Exception exception) {
        LOGGER.debug("Multi-message Exception!", exception);
    }

    @Override
    public void onEndOfMultiMessage() {
        LOGGER.debug("Received End Of Message.");
    }
}
