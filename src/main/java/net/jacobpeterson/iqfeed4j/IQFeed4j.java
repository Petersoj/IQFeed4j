package net.jacobpeterson.iqfeed4j;

import net.jacobpeterson.iqfeed4j.feed.AbstractFeed;
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

import java.io.IOException;
import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * {@link IQFeed4j} contains feeds to interface with IQFeed. You will generally only need one instance of it in your
 * application. Directly interact with the various feeds that IQFeed4j provides via: <code>getFeedName();</code> or
 * <code>startFeedName();</code> and <code>stopFeedName();</code>.
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

    /**
     * Starts the {@link Level1Feed} instance, or does nothing it it's already started.
     *
     * @throws IOException          thrown for {@link IOException}s
     * @throws InterruptedException thrown for {@link InterruptedException}s
     */
    public void startLevel1Feed() throws IOException, InterruptedException {
        startFeed(level1Feed, () -> new Level1Feed(feedName, feedHostname, level1FeedPort));
    }

    /**
     * Stops the {@link Level1Feed} instance, or does nothing it it's already stopped.
     *
     * @throws IOException          thrown for {@link IOException}s
     * @throws InterruptedException thrown for {@link InterruptedException}s
     */
    public void stopLevel1Feed() throws IOException, InterruptedException {
        stopFeed(level1Feed);
        level1Feed = null;
    }

    /**
     * Starts the {@link DerivativeFeed} instance, or does nothing it it's already started.
     *
     * @throws IOException          thrown for {@link IOException}s
     * @throws InterruptedException thrown for {@link InterruptedException}s
     */
    public void startDerivativeFeed() throws IOException, InterruptedException {
        startFeed(derivativeFeed, () -> new DerivativeFeed(feedName, feedHostname, derivativeFeedPort));
    }

    /**
     * Stops the {@link DerivativeFeed} instance, or does nothing it it's already stopped.
     *
     * @throws IOException          thrown for {@link IOException}s
     * @throws InterruptedException thrown for {@link InterruptedException}s
     */
    public void stopDerivativeFeed() throws IOException, InterruptedException {
        stopFeed(derivativeFeed);
        derivativeFeed = null;
    }

    /**
     * Starts the {@link AdminFeed} instance, or does nothing it it's already started.
     *
     * @throws IOException          thrown for {@link IOException}s
     * @throws InterruptedException thrown for {@link InterruptedException}s
     */
    public void startAdminFeed() throws IOException, InterruptedException {
        startFeed(adminFeed, () -> new AdminFeed(feedName, feedHostname, adminFeedPort));
    }

    /**
     * Stops the {@link AdminFeed} instance, or does nothing it it's already stopped.
     *
     * @throws IOException          thrown for {@link IOException}s
     * @throws InterruptedException thrown for {@link InterruptedException}s
     */
    public void stopAdminFeed() throws IOException, InterruptedException {
        stopFeed(adminFeed);
        adminFeed = null;
    }

    /**
     * Starts the {@link HistoricalFeed} instance, or does nothing it it's already started.
     *
     * @throws IOException          thrown for {@link IOException}s
     * @throws InterruptedException thrown for {@link InterruptedException}s
     */
    public void startHistoricalFeed() throws IOException, InterruptedException {
        startFeed(historicalFeed, () -> new HistoricalFeed(feedName, feedHostname, lookupFeedPort));
    }

    /**
     * Stops the {@link HistoricalFeed} instance, or does nothing it it's already stopped.
     *
     * @throws IOException          thrown for {@link IOException}s
     * @throws InterruptedException thrown for {@link InterruptedException}s
     */
    public void stopHistoricalFeed() throws IOException, InterruptedException {
        stopFeed(historicalFeed);
        historicalFeed = null;
    }

    /**
     * Starts the {@link MarketSummaryFeed} instance, or does nothing it it's already started.
     *
     * @throws IOException          thrown for {@link IOException}s
     * @throws InterruptedException thrown for {@link InterruptedException}s
     */
    public void startMarketSummaryFeed() throws IOException, InterruptedException {
        startFeed(marketSummaryFeed, () -> new MarketSummaryFeed(feedName, feedHostname, lookupFeedPort));
    }

    /**
     * Stops the {@link MarketSummaryFeed} instance, or does nothing it it's already stopped.
     *
     * @throws IOException          thrown for {@link IOException}s
     * @throws InterruptedException thrown for {@link InterruptedException}s
     */
    public void stopMarketSummaryFeed() throws IOException, InterruptedException {
        stopFeed(marketSummaryFeed);
        marketSummaryFeed = null;
    }

    /**
     * Starts the {@link NewsFeed} instance, or does nothing it it's already started.
     *
     * @throws IOException          thrown for {@link IOException}s
     * @throws InterruptedException thrown for {@link InterruptedException}s
     */
    public void startNewsFeed() throws IOException, InterruptedException {
        startFeed(newsFeed, () -> new NewsFeed(feedName, feedHostname, lookupFeedPort));
    }

    /**
     * Stops the {@link NewsFeed} instance, or does nothing it it's already stopped.
     *
     * @throws IOException          thrown for {@link IOException}s
     * @throws InterruptedException thrown for {@link InterruptedException}s
     */
    public void stopNewsFeed() throws IOException, InterruptedException {
        stopFeed(newsFeed);
        newsFeed = null;
    }

    /**
     * Starts the {@link OptionChainsFeed} instance, or does nothing it it's already started.
     *
     * @throws IOException          thrown for {@link IOException}s
     * @throws InterruptedException thrown for {@link InterruptedException}s
     */
    public void startOptionChainsFeed() throws IOException, InterruptedException {
        startFeed(optionChainsFeed, () -> new OptionChainsFeed(feedName, feedHostname, lookupFeedPort));
    }

    /**
     * Stops the {@link OptionChainsFeed} instance, or does nothing it it's already stopped.
     *
     * @throws IOException          thrown for {@link IOException}s
     * @throws InterruptedException thrown for {@link InterruptedException}s
     */
    public void stopOptionChainsFeed() throws IOException, InterruptedException {
        stopFeed(optionChainsFeed);
        optionChainsFeed = null;
    }

    /**
     * Starts the {@link SymbolMarketInfoFeed} instance, or does nothing it it's already started.
     *
     * @throws IOException          thrown for {@link IOException}s
     * @throws InterruptedException thrown for {@link InterruptedException}s
     */
    public void startSymbolMarketInfoFeed() throws IOException, InterruptedException {
        startFeed(symbolMarketInfoFeed, () -> new SymbolMarketInfoFeed(feedName, feedHostname, lookupFeedPort));
    }

    /**
     * Stops the {@link SymbolMarketInfoFeed} instance, or does nothing it it's already stopped.
     *
     * @throws IOException          thrown for {@link IOException}s
     * @throws InterruptedException thrown for {@link InterruptedException}s
     */
    public void stopSymbolMarketInfoFeed() throws IOException, InterruptedException {
        stopFeed(symbolMarketInfoFeed);
        symbolMarketInfoFeed = null;
    }

    private <F extends AbstractFeed> void startFeed(F feed, Supplier<F> feedInstantiator)
            throws IOException, InterruptedException {
        if (feed == null) {
            feed = feedInstantiator.get();
            feed.start();
        } else if (!feed.isValid()) {
            feed.stop();
            feed = feedInstantiator.get();
            feed.start();
        }
    }

    private <F extends AbstractFeed> void stopFeed(F feed) throws IOException, InterruptedException {
        if (feed != null) {
            feed.stop();
        }
    }

    /**
     * Gets {@link #level1Feed}
     *
     * @return the {@link Level1Feed}
     */
    public Level1Feed getLevel1Feed() {
        return level1Feed;
    }

    /**
     * Gets {@link #derivativeFeed}
     *
     * @return the {@link DerivativeFeed}
     */
    public DerivativeFeed getDerivativeFeed() {
        return derivativeFeed;
    }

    /**
     * Gets {@link #adminFeed}
     *
     * @return the {@link AdminFeed}
     */
    public AdminFeed getAdminFeed() {
        return adminFeed;
    }

    /**
     * Gets {@link #historicalFeed}
     *
     * @return the {@link HistoricalFeed}
     */
    public HistoricalFeed getHistoricalFeed() {
        return historicalFeed;
    }

    /**
     * Gets {@link #marketSummaryFeed}
     *
     * @return the {@link MarketSummaryFeed}
     */
    public MarketSummaryFeed getMarketSummaryFeed() {
        return marketSummaryFeed;
    }

    /**
     * Gets {@link #newsFeed}
     *
     * @return the {@link NewsFeed}
     */
    public NewsFeed getNewsFeed() {
        return newsFeed;
    }

    /**
     * Gets {@link #optionChainsFeed}
     *
     * @return the {@link OptionChainsFeed}
     */
    public OptionChainsFeed getOptionChainsFeed() {
        return optionChainsFeed;
    }

    /**
     * Gets {@link #symbolMarketInfoFeed}
     *
     * @return the {@link SymbolMarketInfoFeed}
     */
    public SymbolMarketInfoFeed getSymbolMarketInfoFeed() {
        return symbolMarketInfoFeed;
    }
}
