package net.jacobpeterson.iqfeed4j.feed.message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@inheritDoc}
 * <br>
 * {@link MultiMessageAdapter} is an adapter for {@link MultiMessageListener}
 */
public class MultiMessageAdapter<T> extends MultiMessageListener<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MultiMessageAdapter.class);

    @Override
    public void onMessageReceived(T message) {}

    @Override
    public void onMessageException(Exception exception) {
        LOGGER.error("Multi-message Exception!", exception);
    }
}
