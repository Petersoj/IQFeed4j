package net.jacobpeterson.iqfeed4j.api;

import net.jacobpeterson.iqfeed4j.feed.lookup.AbstractLookupFeed;
import net.jacobpeterson.iqfeed4j.feed.lookup.historical.HistoricalFeed;
import net.jacobpeterson.iqfeed4j.feed.lookup.marketsummary.MarketSummaryFeed;
import net.jacobpeterson.iqfeed4j.feed.lookup.news.NewsFeed;
import net.jacobpeterson.iqfeed4j.feed.lookup.optionschains.OptionChainsFeed;
import net.jacobpeterson.iqfeed4j.feed.lookup.symbolmarketinfo.SymbolMarketInfoFeed;
import net.jacobpeterson.iqfeed4j.feed.streaming.admin.AdminFeed;
import net.jacobpeterson.iqfeed4j.feed.streaming.derivative.DerivativeFeed;
import net.jacobpeterson.iqfeed4j.feed.streaming.level1.Level1Feed;
import net.jacobpeterson.iqfeed4j.feed.streaming.marketdepth.MarketDepthFeed;
import net.jacobpeterson.iqfeed4j.properties.IQFeed4jProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * {@link IQFeed4j} contains all methods to interface with IQFeed.
 */
public class IQFeed4j {

    private final Logger LOGGER = LoggerFactory.getLogger(IQFeed4j.class);

    private final String feedName;
    private final String feedHostname;
    private final int level1FeedPort;
    private final int marketDepthFeedPort;
    private final int derivativeFeedPort;
    private final int adminFeedPort;
    private final int lookupFeedPort;

    // Ordering of fields/methods are analogous to the ordering in the IQFeed documentation
    private Level1Feed level1Feed;
    // TODO MarketDepthFeed
    private DerivativeFeed derivativeFeed;
    private AdminFeed adminFeed;
    private HistoricalFeed historicalFeed;
    private MarketSummaryFeed marketSummaryFeed;
    private NewsFeed newsFeed;
    private OptionChainsFeed optionChainsFeed;
    private SymbolMarketInfoFeed symbolMarketInfoFeed;

    /**
     * Instantiates a new {@link IQFeed4j} with properties defined in {@link IQFeed4jProperties#PROPERTIES_FILE}.
     */
    public IQFeed4j() {
        this(IQFeed4jProperties.FEED_NAME,
                IQFeed4jProperties.FEED_HOSTNAME,
                Integer.parseInt(IQFeed4jProperties.LEVEL_1_FEED_PORT),
                Integer.parseInt(IQFeed4jProperties.MARKET_DEPTH_FEED_PORT),
                Integer.parseInt(IQFeed4jProperties.DERIVATIVE_FEED_PORT),
                Integer.parseInt(IQFeed4jProperties.ADMIN_FEED_PORT),
                Integer.parseInt(IQFeed4jProperties.LOOKUP_FEED_PORT));
    }

    /**
     * Instantiates a new {@link IQFeed4j}.
     *
     * @param feedName            name of the feeds that IQFeed4j makes to IQConnect
     * @param feedHostname        the hostname of IQConnect
     * @param level1FeedPort      the {@link Level1Feed} port
     * @param marketDepthFeedPort the {@link MarketDepthFeed} port
     * @param derivativeFeedPort  the {@link DerivativeFeed} port
     * @param adminFeedPort       the {@link AdminFeed} port
     * @param lookupFeedPort      the {@link AbstractLookupFeed} port
     */
    public IQFeed4j(String feedName, String feedHostname, int level1FeedPort, int marketDepthFeedPort,
            int derivativeFeedPort, int adminFeedPort, int lookupFeedPort) {
        checkNotNull(feedName);
        checkNotNull(feedHostname);

        this.feedName = feedName;
        this.feedHostname = feedHostname;
        this.level1FeedPort = level1FeedPort;
        this.marketDepthFeedPort = marketDepthFeedPort;
        this.derivativeFeedPort = derivativeFeedPort;
        this.adminFeedPort = adminFeedPort;
        this.lookupFeedPort = lookupFeedPort;
    }

    //
    // START Level1Feed
    //

    //
    // END Level1Feed
    //

    // TODO MarketDepthFeed

    //
    // START DerivativeFeed
    //

    //
    // END DerivativeFeed
    //

    //
    // START AdminFeed
    //

    //
    // END AdminFeed
    //

    //
    // START HistoricalFeed
    //

    //
    // END HistoricalFeed
    //

    //
    // START MarketSummaryFeed
    //

    //
    // END MarketSummaryFeed
    //

    //
    // START NewsFeed
    //

    //
    // END NewsFeed
    //

    //
    // START OptionChainsFeed
    //

    //
    // END OptionChainsFeed
    //

    //
    // START SymbolMarketInfoFeed
    //

    //
    // END SymbolMarketInfoFeed
    //
}
