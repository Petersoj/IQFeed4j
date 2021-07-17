package net.jacobpeterson.iqfeed4j;

import net.jacobpeterson.iqfeed4j.executable.IQConnectExecutable;
import net.jacobpeterson.iqfeed4j.feed.AbstractFeed;
import net.jacobpeterson.iqfeed4j.feed.lookup.AbstractLookupFeed;
import net.jacobpeterson.iqfeed4j.feed.lookup.historical.HistoricalFeed;
import net.jacobpeterson.iqfeed4j.feed.lookup.marketsummary.MarketSummaryFeed;
import net.jacobpeterson.iqfeed4j.feed.lookup.news.NewsFeed;
import net.jacobpeterson.iqfeed4j.feed.lookup.optionchains.OptionChainsFeed;
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
 * {@link IQFeed4j} is a class that contains feed instances to interface with IQFeed along with an instance of {@link
 * IQConnectExecutable}. You will generally only need one instance of it in your application. Directly interact with the
 * various feeds that IQFeed4j provides with <code>feedName();</code> and start/stop the feeds with
 * <code>startFeedName();</code> and <code>stopFeedName();</code>. You must start the feed with
 * <code>startFeedName();</code> before using it via <code>feedName();</code>.
 */
public class IQFeed4j {

    private static final Logger LOGGER = LoggerFactory.getLogger(IQFeed4j.class);

    private final IQConnectExecutable iqConnectExecutable;
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
        this(IQFeed4jProperties.IQCONNECT_COMMAND,
                IQFeed4jProperties.PRODUCT_ID,
                IQFeed4jProperties.APPLICATION_VERSION,
                IQFeed4jProperties.LOGIN,
                IQFeed4jProperties.PASSWORD,
                IQFeed4jProperties.AUTOCONNECT,
                IQFeed4jProperties.SAVE_LOGIN_INFO,
                IQFeed4jProperties.FEED_NAME,
                IQFeed4jProperties.FEED_HOSTNAME,
                IQFeed4jProperties.LEVEL_1_FEED_PORT,
                IQFeed4jProperties.MARKET_DEPTH_FEED_PORT,
                IQFeed4jProperties.DERIVATIVE_FEED_PORT,
                IQFeed4jProperties.ADMIN_FEED_PORT,
                IQFeed4jProperties.LOOKUP_FEED_PORT);
    }

    /**
     * Instantiates a new {@link IQFeed4j} with any {@link IQConnectExecutable} arguments.
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
        this(null, null, null, null, null, null, null, feedName, feedHostname, level1FeedPort, marketDepthFeedPort,
                derivativeFeedPort, adminFeedPort, lookupFeedPort);
    }

    /**
     * Instantiates a new {@link IQFeed4j}.
     *
     * @param iqConnectCommand    the IQConnect.exe command (optional)
     * @param productID           the product ID (optional)
     * @param applicationVersion  the application version (optional)
     * @param login               the login (optional)
     * @param password            the password (optional)
     * @param autoconnect         the autoconnect (optional)
     * @param saveLoginInfo       the save login info (optional)
     * @param feedName            name of the feeds that IQFeed4j makes to IQConnect
     * @param feedHostname        the hostname of IQConnect
     * @param level1FeedPort      the {@link Level1Feed} port
     * @param marketDepthFeedPort the {@link MarketDepthFeed} port
     * @param derivativeFeedPort  the {@link DerivativeFeed} port
     * @param adminFeedPort       the {@link AdminFeed} port
     * @param lookupFeedPort      the {@link AbstractLookupFeed} port
     */
    public IQFeed4j(String iqConnectCommand, String productID, String applicationVersion, String login,
            String password, Boolean autoconnect, Boolean saveLoginInfo, String feedName, String feedHostname,
            int level1FeedPort, int marketDepthFeedPort, int derivativeFeedPort, int adminFeedPort,
            int lookupFeedPort) {
        if (iqConnectCommand != null) {
            iqConnectExecutable = new IQConnectExecutable(iqConnectCommand, productID, applicationVersion,
                    login, password, autoconnect, saveLoginInfo);
        } else {
            iqConnectExecutable = null;
        }

        checkNotNull(feedName);
        checkNotNull(feedHostname);

        this.feedName = feedName;
        this.feedHostname = feedHostname;
        this.level1FeedPort = level1FeedPort;
        this.marketDepthFeedPort = marketDepthFeedPort;
        this.derivativeFeedPort = derivativeFeedPort;
        this.adminFeedPort = adminFeedPort;
        this.lookupFeedPort = lookupFeedPort;

        LOGGER.debug("{}", this);
    }

    /**
     * Calls {@link IQConnectExecutable#start()}.
     *
     * @throws IOException thrown for {@link IOException}s
     */
    public void startIQConnect() throws IOException {
        iqConnectExecutable.start();
    }

    /**
     * Calls {@link IQConnectExecutable#stop()}.
     */
    public void stopIQConnect() {
        iqConnectExecutable.stop();
    }

    /**
     * Starts the {@link Level1Feed} instance, or does nothing it it's already started.
     *
     * @throws IOException          thrown for {@link IOException}s
     * @throws InterruptedException thrown for {@link InterruptedException}s
     */
    public void startLevel1Feed() throws IOException, InterruptedException {
        level1Feed = startFeed(level1Feed, () -> new Level1Feed(feedName, feedHostname, level1FeedPort));
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
        derivativeFeed = startFeed(derivativeFeed,
                () -> new DerivativeFeed(feedName, feedHostname, derivativeFeedPort));
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
        adminFeed = startFeed(adminFeed, () -> new AdminFeed(feedName, feedHostname, adminFeedPort));
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
        historicalFeed = startFeed(historicalFeed, () -> new HistoricalFeed(feedName, feedHostname, lookupFeedPort));
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
        marketSummaryFeed = startFeed(marketSummaryFeed,
                () -> new MarketSummaryFeed(feedName, feedHostname, lookupFeedPort));
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
        newsFeed = startFeed(newsFeed, () -> new NewsFeed(feedName, feedHostname, lookupFeedPort));
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
        optionChainsFeed = startFeed(optionChainsFeed,
                () -> new OptionChainsFeed(feedName, feedHostname, lookupFeedPort));
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
        symbolMarketInfoFeed = startFeed(symbolMarketInfoFeed,
                () -> new SymbolMarketInfoFeed(feedName, feedHostname, lookupFeedPort));
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

    /**
     * Starts an {@link AbstractFeed}.
     *
     * @param <F>              the {@link AbstractFeed} type parameter
     * @param feed             the {@link AbstractFeed}
     * @param feedInstantiator the {@link AbstractFeed} instantiator {@link Supplier}
     *
     * @return the started {@link AbstractFeed}
     *
     * @throws IOException          thrown for {@link IOException}s
     * @throws InterruptedException thrown for {@link InterruptedException}s
     */
    private <F extends AbstractFeed> F startFeed(F feed, Supplier<F> feedInstantiator)
            throws IOException, InterruptedException {
        if (feed == null) {
            feed = feedInstantiator.get();
            feed.start();
        } else if (!feed.isValid()) {
            feed.stop();
            feed = feedInstantiator.get();
            feed.start();
        }

        return feed;
    }

    /**
     * Stops an {@link AbstractFeed} if not <code>null</code>.
     *
     * @param <F>  the {@link AbstractFeed} type parameter
     * @param feed the {@link AbstractFeed}
     *
     * @throws IOException          thrown for {@link IOException}s
     * @throws InterruptedException thrown for {@link InterruptedException}s
     */
    private <F extends AbstractFeed> void stopFeed(F feed) throws IOException, InterruptedException {
        if (feed != null) {
            feed.stop();
        }
    }

    /**
     * Gets {@link #iqConnectExecutable}.
     *
     * @return the {@link IQConnectExecutable}
     */
    public IQConnectExecutable iqConnectExecutable() {
        return iqConnectExecutable;
    }

    /**
     * Gets {@link #level1Feed}
     *
     * @return the {@link Level1Feed}
     */
    public Level1Feed level1() {
        return level1Feed;
    }

    /**
     * Gets {@link #derivativeFeed}
     *
     * @return the {@link DerivativeFeed}
     */
    public DerivativeFeed derivative() {
        return derivativeFeed;
    }

    /**
     * Gets {@link #adminFeed}
     *
     * @return the {@link AdminFeed}
     */
    public AdminFeed admin() {
        return adminFeed;
    }

    /**
     * Gets {@link #historicalFeed}
     *
     * @return the {@link HistoricalFeed}
     */
    public HistoricalFeed historical() {
        return historicalFeed;
    }

    /**
     * Gets {@link #marketSummaryFeed}
     *
     * @return the {@link MarketSummaryFeed}
     */
    public MarketSummaryFeed marketSummary() {
        return marketSummaryFeed;
    }

    /**
     * Gets {@link #newsFeed}
     *
     * @return the {@link NewsFeed}
     */
    public NewsFeed news() {
        return newsFeed;
    }

    /**
     * Gets {@link #optionChainsFeed}
     *
     * @return the {@link OptionChainsFeed}
     */
    public OptionChainsFeed optionChains() {
        return optionChainsFeed;
    }

    /**
     * Gets {@link #symbolMarketInfoFeed}
     *
     * @return the {@link SymbolMarketInfoFeed}
     */
    public SymbolMarketInfoFeed symbolMarketInfo() {
        return symbolMarketInfoFeed;
    }

    @Override
    public String toString() {
        return "IQFeed4j{" +
                "feedName='" + feedName + '\'' +
                ", feedHostname='" + feedHostname + '\'' +
                ", level1FeedPort=" + level1FeedPort +
                ", marketDepthFeedPort=" + marketDepthFeedPort +
                ", derivativeFeedPort=" + derivativeFeedPort +
                ", adminFeedPort=" + adminFeedPort +
                ", lookupFeedPort=" + lookupFeedPort +
                '}';
    }
}
