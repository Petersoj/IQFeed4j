package net.jacobpeterson.iqfeed4j.feed.lookup.marketsummary;

import net.jacobpeterson.iqfeed4j.feed.lookup.AbstractLookupFeed;
import net.jacobpeterson.iqfeed4j.model.feedenums.FeedMessageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static net.jacobpeterson.iqfeed4j.util.csv.CSVUtil.valueEquals;
import static net.jacobpeterson.iqfeed4j.util.csv.CSVUtil.valueExists;

/**
 * {@link MarketSummaryFeed} is an {@link AbstractLookupFeed} for market summary (snapshot) data.
 */
public class MarketSummaryFeed extends AbstractLookupFeed {

    private static final Logger LOGGER = LoggerFactory.getLogger(MarketSummaryFeed.class);
    private static final String FEED_NAME_SUFFIX = " Market Summary";

    protected final Object messageReceivedLock;

    /**
     * Instantiates a new {@link MarketSummaryFeed}.
     *
     * @param marketSummaryFeedName the Market Summary feed name
     * @param hostname              the hostname
     * @param port                  the port
     */
    public MarketSummaryFeed(String marketSummaryFeedName, String hostname, int port) {
        super(marketSummaryFeedName + FEED_NAME_SUFFIX, hostname, port);

        messageReceivedLock = new Object();
    }

    @Override
    protected void onMessageReceived(String[] csv) {
        if (valueEquals(csv, 0, FeedMessageType.ERROR.value())) {
            LOGGER.error("Received error message! {}", (Object) csv);
            return;
        }

        // All messages sent on this feed must have a Request ID first
        if (!valueExists(csv, 0)) {
            LOGGER.error("Received unknown message format: {}", (Object) csv);
            return;
        }

        String requestID = csv[0];

        synchronized (messageReceivedLock) {

        }
    }

    @Override
    protected void onAsyncException(String message, Exception exception) {
        LOGGER.error(message, exception);
        LOGGER.info("Attempting to close {}...", feedName);
        try {
            stop();
        } catch (Exception stopException) {
            LOGGER.error("Could not close {}!", feedName, stopException);
        }
    }
}
