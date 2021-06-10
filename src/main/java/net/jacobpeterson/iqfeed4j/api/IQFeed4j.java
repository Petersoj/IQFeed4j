package net.jacobpeterson.iqfeed4j.api;

import net.jacobpeterson.iqfeed4j.api.exception.IQFeed4jException;
import net.jacobpeterson.iqfeed4j.feed.AbstractFeed;
import net.jacobpeterson.iqfeed4j.feed.lookup.AbstractLookupFeed;
import net.jacobpeterson.iqfeed4j.feed.lookup.historical.HistoricalFeed;
import net.jacobpeterson.iqfeed4j.feed.lookup.marketsummary.MarketSummaryFeed;
import net.jacobpeterson.iqfeed4j.feed.lookup.news.NewsFeed;
import net.jacobpeterson.iqfeed4j.feed.lookup.optionschains.OptionChainsFeed;
import net.jacobpeterson.iqfeed4j.feed.lookup.symbolmarketinfo.SymbolMarketInfoFeed;
import net.jacobpeterson.iqfeed4j.feed.message.FeedMessageListener;
import net.jacobpeterson.iqfeed4j.feed.message.MultiMessageIteratorListener;
import net.jacobpeterson.iqfeed4j.feed.message.MultiMessageListener;
import net.jacobpeterson.iqfeed4j.feed.streaming.admin.AdminFeed;
import net.jacobpeterson.iqfeed4j.feed.streaming.derivative.DerivativeFeed;
import net.jacobpeterson.iqfeed4j.feed.streaming.level1.Level1Feed;
import net.jacobpeterson.iqfeed4j.feed.streaming.marketdepth.MarketDepthFeed;
import net.jacobpeterson.iqfeed4j.model.feed.common.interval.IntervalType;
import net.jacobpeterson.iqfeed4j.model.feed.lookup.historical.DatedInterval;
import net.jacobpeterson.iqfeed4j.model.feed.lookup.historical.Interval;
import net.jacobpeterson.iqfeed4j.model.feed.lookup.historical.Tick;
import net.jacobpeterson.iqfeed4j.model.feed.lookup.historical.enums.DataDirection;
import net.jacobpeterson.iqfeed4j.model.feed.lookup.historical.enums.PartialDatapoint;
import net.jacobpeterson.iqfeed4j.properties.IQFeed4jProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Iterator;
import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * {@link IQFeed4j} contains all methods to interface with IQFeed. You will generally only need one instance of it in
 * your application. The methods in this class provide conveniences such as: automatically start feeds upon data/command
 * requests, aggregating lookup data in an {@link Iterator}, higher-level data mapping for easier processing, wrapping
 * {@link Exception}s in {@link IQFeed4jException}. Directly interact with the various feeds that IQFeed provides via:
 * <code>getFeedName();</code> or <code>startFeedName();</code> and <code>stopFeedName();</code>.
 * <br>
 * Methods allow <code>null<code/> to be passed in as a parameter if it is optional. See the Javadoc of the method for
 * more details.
 * <br>
 * Methods that interact with a Lookup Feed (e.g. feeds that extend {@link AbstractLookupFeed}) and return an {@link
 * Iterator} and do not have a method name that ends in <code>Sync</code> in this class, will accumulate all requested
 * messages/data into memory which can be consumed later. If this behavior is undesired and you want to consume data
 * synchronously with the underlying feed, either use the <code>getFeedName();</code> getters to request data with a
 * supplied {@link FeedMessageListener} instead or use the methods that have a name that ends in <code>Sync</code>. Note
 * that synchronously consuming data will block the underlying feed if data is not consumed fast enough, but prevents
 * large amounts of messages from accumulating in memory without being processed and garbage-collected, which is useful
 * for large multi-message requests.
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

    /**
     * See {@link HistoricalFeed#requestTicks(String, int, DataDirection, MultiMessageListener)} for details.
     *
     * @throws IQFeed4jException thrown for {@link IQFeed4jException}s
     */
    public Iterator<Tick> requestTicks(String symbol, int maxDataPoints, DataDirection dataDirection)
            throws IQFeed4jException {
        try {
            startHistoricalFeed();

            MultiMessageIteratorListener<Tick> asyncListener = new MultiMessageIteratorListener<>();
            historicalFeed.requestTicks(symbol, maxDataPoints, dataDirection, asyncListener);
            return asyncListener.getIterator();
        } catch (Exception exception) {
            throw new IQFeed4jException(exception);
        }
    }

    /**
     * See {@link HistoricalFeed#requestTicks(String, int, Integer, LocalTime, LocalTime, DataDirection,
     * MultiMessageListener)} for details.
     *
     * @throws IQFeed4jException thrown for {@link IQFeed4jException}s
     */
    public Iterator<Tick> requestTicks(String symbol, int maxDays, Integer maxDataPoints, LocalTime beginFilterTime,
            LocalTime endFilterTime, DataDirection dataDirection) throws IQFeed4jException {
        try {
            startHistoricalFeed();

            MultiMessageIteratorListener<Tick> asyncListener = new MultiMessageIteratorListener<>();
            historicalFeed.requestTicks(symbol, maxDays, maxDataPoints, beginFilterTime, endFilterTime, dataDirection,
                    asyncListener);
            return asyncListener.getIterator();
        } catch (Exception exception) {
            throw new IQFeed4jException(exception);
        }
    }

    /**
     * See {@link HistoricalFeed#requestTicks(String, LocalDateTime, LocalDateTime, Integer, LocalTime, LocalTime,
     * DataDirection, MultiMessageListener)} for details.
     *
     * @throws IQFeed4jException thrown for {@link IQFeed4jException}s
     */
    public Iterator<Tick> requestTicks(String symbol, LocalDateTime beginDateTime, LocalDateTime endDateTime,
            Integer maxDataPoints, LocalTime beginFilterTime, LocalTime endFilterTime, DataDirection dataDirection)
            throws IQFeed4jException {
        try {
            startHistoricalFeed();

            MultiMessageIteratorListener<Tick> asyncListener = new MultiMessageIteratorListener<>();
            historicalFeed.requestTicks(symbol, beginDateTime, endDateTime, maxDataPoints, beginFilterTime,
                    endFilterTime, dataDirection, asyncListener);
            return asyncListener.getIterator();
        } catch (Exception exception) {
            throw new IQFeed4jException(exception);
        }
    }

    /**
     * See {@link HistoricalFeed#requestIntervals(String, int, Integer, DataDirection, IntervalType,
     * MultiMessageListener)} for details.
     *
     * @throws IQFeed4jException thrown for {@link IQFeed4jException}s
     */
    public Iterator<Interval> requestIntervals(String symbol, int intervalLength, Integer maxDataPoints,
            DataDirection dataDirection, IntervalType intervalType) throws IQFeed4jException {
        try {
            startHistoricalFeed();

            MultiMessageIteratorListener<Interval> asyncListener = new MultiMessageIteratorListener<>();
            historicalFeed.requestIntervals(symbol, intervalLength, maxDataPoints, dataDirection, intervalType,
                    asyncListener);
            return asyncListener.getIterator();
        } catch (Exception exception) {
            throw new IQFeed4jException(exception);
        }
    }

    /**
     * See {@link HistoricalFeed#requestIntervals(String, int, int, Integer, LocalTime, LocalTime, DataDirection,
     * IntervalType, MultiMessageListener)} for details.
     *
     * @throws IQFeed4jException thrown for {@link IQFeed4jException}s
     */
    public Iterator<Interval> requestIntervals(String symbol, int intervalLength, int maxDays, Integer maxDataPoints,
            LocalTime beginFilterTime, LocalTime endFilterTime, DataDirection dataDirection, IntervalType intervalType)
            throws IQFeed4jException {
        try {
            startHistoricalFeed();

            MultiMessageIteratorListener<Interval> asyncListener = new MultiMessageIteratorListener<>();
            historicalFeed.requestIntervals(symbol, intervalLength, maxDays, maxDataPoints, beginFilterTime,
                    endFilterTime, dataDirection, intervalType, asyncListener);
            return asyncListener.getIterator();
        } catch (Exception exception) {
            throw new IQFeed4jException(exception);
        }
    }

    /**
     * See {@link HistoricalFeed#requestIntervals(String, int, LocalDateTime, LocalDateTime, Integer, LocalTime,
     * LocalTime, DataDirection, IntervalType, MultiMessageListener)} for details.
     *
     * @throws IQFeed4jException thrown for {@link IQFeed4jException}s
     */
    public Iterator<Interval> requestIntervals(String symbol, int intervalLength, LocalDateTime beginDateTime,
            LocalDateTime endDateTime, Integer maxDataPoints, LocalTime beginFilterTime, LocalTime endFilterTime,
            DataDirection dataDirection, IntervalType intervalType) throws IQFeed4jException {
        try {
            startHistoricalFeed();

            MultiMessageIteratorListener<Interval> asyncListener = new MultiMessageIteratorListener<>();
            historicalFeed.requestIntervals(symbol, intervalLength, beginDateTime, endDateTime, maxDataPoints,
                    beginFilterTime, endFilterTime, dataDirection, intervalType, asyncListener);
            return asyncListener.getIterator();
        } catch (Exception exception) {
            throw new IQFeed4jException(exception);
        }
    }

    /**
     * See {@link HistoricalFeed#requestDayIntervals(String, int, DataDirection, PartialDatapoint,
     * MultiMessageListener)} for details.
     *
     * @throws IQFeed4jException thrown for {@link IQFeed4jException}s
     */
    public Iterator<DatedInterval> requestDayIntervals(String symbol, int maxDays, DataDirection dataDirection,
            PartialDatapoint partialDatapoint) throws IQFeed4jException {
        try {
            startHistoricalFeed();

            MultiMessageIteratorListener<DatedInterval> asyncListener = new MultiMessageIteratorListener<>();
            historicalFeed.requestDayIntervals(symbol, maxDays, dataDirection, partialDatapoint, asyncListener);
            return asyncListener.getIterator();
        } catch (Exception exception) {
            throw new IQFeed4jException(exception);
        }
    }

    /**
     * See {@link HistoricalFeed#requestDayIntervals(String, LocalDate, LocalDate, Integer, DataDirection,
     * PartialDatapoint, MultiMessageListener)} for details.
     *
     * @throws IQFeed4jException thrown for {@link IQFeed4jException}s
     */
    public Iterator<DatedInterval> requestDayIntervals(String symbol, LocalDate beginDate, LocalDate endDate,
            Integer maxDataPoints, DataDirection dataDirection, PartialDatapoint partialDatapoint)
            throws IQFeed4jException {
        try {
            startHistoricalFeed();

            MultiMessageIteratorListener<DatedInterval> asyncListener = new MultiMessageIteratorListener<>();
            historicalFeed.requestDayIntervals(symbol, beginDate, endDate, maxDataPoints, dataDirection,
                    partialDatapoint, asyncListener);
            return asyncListener.getIterator();
        } catch (Exception exception) {
            throw new IQFeed4jException(exception);
        }
    }

    /**
     * See {@link HistoricalFeed#requestWeekIntervals(String, int, DataDirection, PartialDatapoint,
     * MultiMessageListener)} for details.
     *
     * @throws IQFeed4jException thrown for {@link IQFeed4jException}s
     */
    public Iterator<DatedInterval> requestWeekIntervals(String symbol, int maxWeeks, DataDirection dataDirection,
            PartialDatapoint partialDatapoint) throws IQFeed4jException {
        try {
            startHistoricalFeed();

            MultiMessageIteratorListener<DatedInterval> asyncListener = new MultiMessageIteratorListener<>();
            historicalFeed.requestWeekIntervals(symbol, maxWeeks, dataDirection, partialDatapoint, asyncListener);
            return asyncListener.getIterator();
        } catch (Exception exception) {
            throw new IQFeed4jException(exception);
        }
    }

    /**
     * See {@link HistoricalFeed#requestMonthIntervals(String, int, DataDirection, PartialDatapoint,
     * MultiMessageListener)} for details.
     *
     * @throws IQFeed4jException thrown for {@link IQFeed4jException}s
     */
    public Iterator<DatedInterval> requestMonthIntervals(String symbol, int maxMonths, DataDirection dataDirection,
            PartialDatapoint partialDatapoint) throws IQFeed4jException {
        try {
            startHistoricalFeed();

            MultiMessageIteratorListener<DatedInterval> asyncListener = new MultiMessageIteratorListener<>();
            historicalFeed.requestMonthIntervals(symbol, maxMonths, dataDirection, partialDatapoint, asyncListener);
            return asyncListener.getIterator();
        } catch (Exception exception) {
            throw new IQFeed4jException(exception);
        }
    }

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

    //
    // START Feed helpers
    //

    /**
     * Starts the {@link Level1Feed} instance, or does nothing it it's already started.
     *
     * @throws IQFeed4jException thrown for {@link IQFeed4jException}s
     */
    public void startLevel1Feed() throws IQFeed4jException {
        startFeed(level1Feed, () -> new Level1Feed(feedName, feedHostname, level1FeedPort));
    }

    /**
     * Stops the {@link Level1Feed} instance, or does nothing it it's already stopped.
     *
     * @throws IQFeed4jException thrown for {@link IQFeed4jException}s
     */
    public void stopLevel1Feed() throws IQFeed4jException {
        stopFeed(level1Feed);
        level1Feed = null;
    }

    /**
     * Starts the {@link DerivativeFeed} instance, or does nothing it it's already started.
     *
     * @throws IQFeed4jException thrown for {@link IQFeed4jException}s
     */
    public void startDerivativeFeed() throws IQFeed4jException {
        startFeed(derivativeFeed, () -> new DerivativeFeed(feedName, feedHostname, derivativeFeedPort));
    }

    /**
     * Stops the {@link DerivativeFeed} instance, or does nothing it it's already stopped.
     *
     * @throws IQFeed4jException thrown for {@link IQFeed4jException}s
     */
    public void stopDerivativeFeed() throws IQFeed4jException {
        stopFeed(derivativeFeed);
        derivativeFeed = null;
    }

    /**
     * Starts the {@link AdminFeed} instance, or does nothing it it's already started.
     *
     * @throws IQFeed4jException thrown for {@link IQFeed4jException}s
     */
    public void startAdminFeed() throws IQFeed4jException {
        startFeed(adminFeed, () -> new AdminFeed(feedName, feedHostname, adminFeedPort));
    }

    /**
     * Stops the {@link AdminFeed} instance, or does nothing it it's already stopped.
     *
     * @throws IQFeed4jException thrown for {@link IQFeed4jException}s
     */
    public void stopAdminFeed() throws IQFeed4jException {
        stopFeed(adminFeed);
        adminFeed = null;
    }

    /**
     * Starts the {@link HistoricalFeed} instance, or does nothing it it's already started.
     *
     * @throws IQFeed4jException thrown for {@link IQFeed4jException}s
     */
    public void startHistoricalFeed() throws IQFeed4jException {
        startFeed(historicalFeed, () -> new HistoricalFeed(feedName, feedHostname, lookupFeedPort));
    }

    /**
     * Stops the {@link HistoricalFeed} instance, or does nothing it it's already stopped.
     *
     * @throws IQFeed4jException thrown for {@link IQFeed4jException}s
     */
    public void stopHistoricalFeed() throws IQFeed4jException {
        stopFeed(historicalFeed);
        historicalFeed = null;
    }

    /**
     * Starts the {@link MarketSummaryFeed} instance, or does nothing it it's already started.
     *
     * @throws IQFeed4jException thrown for {@link IQFeed4jException}s
     */
    public void startMarketSummaryFeed() throws IQFeed4jException {
        startFeed(marketSummaryFeed, () -> new MarketSummaryFeed(feedName, feedHostname, lookupFeedPort));
    }

    /**
     * Stops the {@link MarketSummaryFeed} instance, or does nothing it it's already stopped.
     *
     * @throws IQFeed4jException thrown for {@link IQFeed4jException}s
     */
    public void stopMarketSummaryFeed() throws IQFeed4jException {
        stopFeed(marketSummaryFeed);
        marketSummaryFeed = null;
    }

    /**
     * Starts the {@link NewsFeed} instance, or does nothing it it's already started.
     *
     * @throws IQFeed4jException thrown for {@link IQFeed4jException}s
     */
    public void startNewsFeed() throws IQFeed4jException {
        startFeed(newsFeed, () -> new NewsFeed(feedName, feedHostname, lookupFeedPort));
    }

    /**
     * Stops the {@link NewsFeed} instance, or does nothing it it's already stopped.
     *
     * @throws IQFeed4jException thrown for {@link IQFeed4jException}s
     */
    public void stopNewsFeed() throws IQFeed4jException {
        stopFeed(newsFeed);
        newsFeed = null;
    }

    /**
     * Starts the {@link OptionChainsFeed} instance, or does nothing it it's already started.
     *
     * @throws IQFeed4jException thrown for {@link IQFeed4jException}s
     */
    public void startOptionChainsFeed() throws IQFeed4jException {
        startFeed(optionChainsFeed, () -> new OptionChainsFeed(feedName, feedHostname, lookupFeedPort));
    }

    /**
     * Stops the {@link OptionChainsFeed} instance, or does nothing it it's already stopped.
     *
     * @throws IQFeed4jException thrown for {@link IQFeed4jException}s
     */
    public void stopOptionChainsFeed() throws IQFeed4jException {
        stopFeed(optionChainsFeed);
        optionChainsFeed = null;
    }

    /**
     * Starts the {@link SymbolMarketInfoFeed} instance, or does nothing it it's already started.
     *
     * @throws IQFeed4jException thrown for {@link IQFeed4jException}s
     */
    public void startSymbolMarketInfoFeed() throws IQFeed4jException {
        startFeed(symbolMarketInfoFeed, () -> new SymbolMarketInfoFeed(feedName, feedHostname, lookupFeedPort));
    }

    /**
     * Stops the {@link SymbolMarketInfoFeed} instance, or does nothing it it's already stopped.
     *
     * @throws IQFeed4jException thrown for {@link IQFeed4jException}s
     */
    public void stopSymbolMarketInfoFeed() throws IQFeed4jException {
        stopFeed(symbolMarketInfoFeed);
        symbolMarketInfoFeed = null;
    }

    private <F extends AbstractFeed> void startFeed(F feed, Supplier<F> feedInstantiator)
            throws IQFeed4jException {
        try {
            if (feed == null) {
                feed = feedInstantiator.get();
                feed.start();
            } else if (!feed.isValid()) {
                feed.stop();
                feed = feedInstantiator.get();
                feed.start();
            }
        } catch (Exception exception) {
            throw new IQFeed4jException(exception);
        }
    }

    private <F extends AbstractFeed> void stopFeed(F feed) throws IQFeed4jException {
        try {
            if (feed != null) {
                feed.stop();
            }
        } catch (Exception exception) {
            throw new IQFeed4jException(exception);
        }
    }

    //
    // END Feed helpers
    //

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
