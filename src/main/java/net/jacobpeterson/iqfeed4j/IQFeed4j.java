package net.jacobpeterson.iqfeed4j;

import net.jacobpeterson.iqfeed4j.properties.IQFeed4jProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link IQFeed4j} contains all methods to interface with IQFeed.
 */
public class IQFeed4j {

    private final Logger LOGGER = LoggerFactory.getLogger(IQFeed4j.class);

    private final String feedName;
    private final String feedHostname;
    private final int level1FeedPort;
    private final int level2FeedPort;
    private final int derivativeFeedPort;
    private final int adminFeedPort;
    private final int lookupFeedPort;

    /**
     * Instantiates a new {@link IQFeed4j} with properties defined in {@link IQFeed4jProperties#PROPERTIES_FILE}.
     */
    public IQFeed4j() {
        this(IQFeed4jProperties.FEED_NAME,
                IQFeed4jProperties.FEED_HOSTNAME,
                Integer.parseInt(IQFeed4jProperties.LEVEL1_FEED_PORT),
                Integer.parseInt(IQFeed4jProperties.LEVEL2_FEED_PORT),
                Integer.parseInt(IQFeed4jProperties.DERIVATIVE_FEED_PORT),
                Integer.parseInt(IQFeed4jProperties.ADMIN_FEED_PORT),
                Integer.parseInt(IQFeed4jProperties.LOOKUP_FEED_PORT));
    }

    /**
     * Instantiates a new {@link IQFeed4j}.
     *
     * @param feedName           name of the feeds that IQFeed4j makes to IQConnect
     * @param feedHostname       the hostname of IQConnect
     * @param level1FeedPort     the level 1 feed port
     * @param level2FeedPort     the level 2 feed port
     * @param derivativeFeedPort the derivative feed port
     * @param adminFeedPort      the admin feed port
     * @param lookupFeedPort     the lookup feed port
     */
    public IQFeed4j(String feedName, String feedHostname, int level1FeedPort, int level2FeedPort,
            int derivativeFeedPort, int adminFeedPort, int lookupFeedPort) {
        this.feedName = feedName;
        this.feedHostname = feedHostname;
        this.level1FeedPort = level1FeedPort;
        this.level2FeedPort = level2FeedPort;
        this.derivativeFeedPort = derivativeFeedPort;
        this.adminFeedPort = adminFeedPort;
        this.lookupFeedPort = lookupFeedPort;
    }
}
