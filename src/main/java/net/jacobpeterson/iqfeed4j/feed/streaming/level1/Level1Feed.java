package net.jacobpeterson.iqfeed4j.feed.streaming.level1;

import net.jacobpeterson.iqfeed4j.feed.streaming.AbstractServerConnectionFeed;
import net.jacobpeterson.iqfeed4j.model.feed.common.enums.FeedMessageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static net.jacobpeterson.iqfeed4j.util.csv.CSVUtil.valueEquals;

/**
 * {@link Level1Feed} is an {@link AbstractServerConnectionFeed} for Level 1 market data.
 */
public class Level1Feed extends AbstractServerConnectionFeed {

    private static final Logger LOGGER = LoggerFactory.getLogger(Level1Feed.class);
    protected static final String FEED_NAME_SUFFIX = " Level 1 Feed";

    /**
     * Instantiates a new {@link Level1Feed}.
     *
     * @param level1FeedName the {@link Level1Feed} feed name
     * @param hostname       the hostname
     * @param port           the port
     */
    public Level1Feed(String level1FeedName, String hostname, int port) {
        super(level1FeedName + FEED_NAME_SUFFIX, hostname, port, COMMA_DELIMITED_SPLITTER, true, true);
    }

    @Override
    protected void onMessageReceived(String[] csv) {
        super.onMessageReceived(csv);

        if (valueEquals(csv, 0, FeedMessageType.ERROR.value())) {
            LOGGER.error("Received error message! {}", (Object) csv);
            return;
        }

        // TODO
    }
}
