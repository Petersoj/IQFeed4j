package net.jacobpeterson.iqfeed4j.feed.streaming.derivative;

import net.jacobpeterson.iqfeed4j.feed.RequestIDFeedHelper;
import net.jacobpeterson.iqfeed4j.feed.message.FeedMessageListener;
import net.jacobpeterson.iqfeed4j.feed.message.SingleMessageFuture;
import net.jacobpeterson.iqfeed4j.feed.streaming.AbstractServerConnectionFeed;
import net.jacobpeterson.iqfeed4j.model.feed.common.enums.FeedCommand;
import net.jacobpeterson.iqfeed4j.model.feed.common.enums.FeedMessageType;
import net.jacobpeterson.iqfeed4j.model.feed.common.interval.IntervalType;
import net.jacobpeterson.iqfeed4j.model.feed.streaming.derivative.Interval;
import net.jacobpeterson.iqfeed4j.model.feed.streaming.derivative.WatchedInterval;
import net.jacobpeterson.iqfeed4j.model.feed.streaming.derivative.enums.DerivativeCommand;
import net.jacobpeterson.iqfeed4j.model.feed.streaming.derivative.enums.DerivativeMessageType;
import net.jacobpeterson.iqfeed4j.model.feed.streaming.derivative.enums.DerivativeSystemCommand;
import net.jacobpeterson.iqfeed4j.model.feed.streaming.derivative.enums.DerivativeSystemMessageType;
import net.jacobpeterson.iqfeed4j.util.csv.mapper.index.IndexCSVMapper;
import net.jacobpeterson.iqfeed4j.util.csv.mapper.list.NestedListCSVMapper;
import net.jacobpeterson.iqfeed4j.util.string.LineEnding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static net.jacobpeterson.iqfeed4j.util.csv.CSVUtil.valueEquals;
import static net.jacobpeterson.iqfeed4j.util.csv.CSVUtil.valueExists;
import static net.jacobpeterson.iqfeed4j.util.csv.CSVUtil.valuePresent;
import static net.jacobpeterson.iqfeed4j.util.csv.mapper.AbstractCSVMapper.DateTimeConverters.DASHED_DATE_SPACE_TIME;
import static net.jacobpeterson.iqfeed4j.util.csv.mapper.AbstractCSVMapper.DateTimeFormatters.DATE_SPACE_TIME;
import static net.jacobpeterson.iqfeed4j.util.csv.mapper.AbstractCSVMapper.DateTimeFormatters.TIME;
import static net.jacobpeterson.iqfeed4j.util.csv.mapper.AbstractCSVMapper.PrimitiveConvertors.DOUBLE;
import static net.jacobpeterson.iqfeed4j.util.csv.mapper.AbstractCSVMapper.PrimitiveConvertors.INTEGER;
import static net.jacobpeterson.iqfeed4j.util.csv.mapper.AbstractCSVMapper.PrimitiveConvertors.STRING;

/**
 * {@link DerivativeFeed} is an {@link AbstractServerConnectionFeed} for derivative tick data (aka interval/bar data).
 */
public class DerivativeFeed extends AbstractServerConnectionFeed {

    private static final Logger LOGGER = LoggerFactory.getLogger(DerivativeFeed.class);
    protected static final String FEED_NAME_SUFFIX = " Derivative Feed";
    protected static final IndexCSVMapper<Interval> INTERVAL_CSV_MAPPER;
    protected static final NestedListCSVMapper<WatchedInterval> WATCHED_INTERVALS_CSV_MAPPER;

    static {
        // Add mappings with CSV indices analogous to line of execution

        INTERVAL_CSV_MAPPER = new IndexCSVMapper<>(Interval::new);
        INTERVAL_CSV_MAPPER.addMapping(Interval::setUpdateType, Interval.UpdateType::fromValue);
        INTERVAL_CSV_MAPPER.addMapping(Interval::setSymbol, STRING);
        INTERVAL_CSV_MAPPER.addMapping(Interval::setTimestamp, DASHED_DATE_SPACE_TIME);
        INTERVAL_CSV_MAPPER.addMapping(Interval::setOpen, DOUBLE);
        INTERVAL_CSV_MAPPER.addMapping(Interval::setHigh, DOUBLE);
        INTERVAL_CSV_MAPPER.addMapping(Interval::setLow, DOUBLE);
        INTERVAL_CSV_MAPPER.addMapping(Interval::setLast, DOUBLE);
        INTERVAL_CSV_MAPPER.addMapping(Interval::setCumulativeVolume, INTEGER);
        INTERVAL_CSV_MAPPER.addMapping(Interval::setIntervalVolume, INTEGER);
        INTERVAL_CSV_MAPPER.addMapping(Interval::setNumberOfTrades, INTEGER);

        WATCHED_INTERVALS_CSV_MAPPER = new NestedListCSVMapper<>(ArrayList::new, WatchedInterval::new, 3);
        WATCHED_INTERVALS_CSV_MAPPER.addMapping(WatchedInterval::setSymbol, STRING);
        WATCHED_INTERVALS_CSV_MAPPER.addMapping(WatchedInterval::setInterval, INTEGER);
        WATCHED_INTERVALS_CSV_MAPPER.addMapping(WatchedInterval::setRequestID, STRING);
    }

    protected final Object messageReceivedLock;
    protected final RequestIDFeedHelper requestIDFeedHelper;
    protected final HashMap<String, IntervalListenerData> intervalListenerDataOfRequestIDs;
    protected final Queue<SingleMessageFuture<List<WatchedInterval>>> watchedIntervalsFuturesQueue;
    protected DerivativeFeedEventListener derivativeFeedEventListener;

    /**
     * Instantiates a new {@link DerivativeFeed}.
     *
     * @param derivativeFeedName the {@link DerivativeFeed} name
     * @param hostname           the hostname
     * @param port               the port
     */
    public DerivativeFeed(String derivativeFeedName, String hostname, int port) {
        super(LOGGER, derivativeFeedName + FEED_NAME_SUFFIX, hostname, port, COMMA_DELIMITED_SPLITTER, false, true);

        this.derivativeFeedEventListener = new DerivativeFeedEventListener() {
            @Override
            public void onSymbolNotWatched(String symbol) {
                LOGGER.error("{} symbol not watched!", symbol);
            }

            @Override
            public void onSymbolLimitReached(String symbol) {
                LOGGER.error("Symbol limit reached with symbol: {}!", symbol);
            }

            @Override
            public void onReplacedPreviouslyWatchedSymbol(String symbol, String requestID) {
                LOGGER.info("Symbol {} replaced for Request ID: {}", symbol, requestID);
            }
        };

        messageReceivedLock = new Object();
        requestIDFeedHelper = new RequestIDFeedHelper();
        intervalListenerDataOfRequestIDs = new HashMap<>();
        watchedIntervalsFuturesQueue = new LinkedList<>();
    }

    @Override
    protected void onMessageReceived(String[] csv) {
        if (valueEquals(csv, 0, FeedMessageType.ERROR.value())) {
            LOGGER.error("Received error message! {}", (Object) csv);
            return;
        }

        synchronized (messageReceivedLock) {
            if (valueEquals(csv, 0, FeedMessageType.SYSTEM.value())) {
                if (!valuePresent(csv, 1)) {
                    LOGGER.error("Received unknown System message: {}", (Object) csv);
                    return;
                }

                String systemMessageTypeString = csv[1];

                if (checkServerConnectionStatusMessage(systemMessageTypeString)) {
                    return;
                }

                try {
                    DerivativeSystemMessageType derivativeSystemMessage =
                            DerivativeSystemMessageType.fromValue(systemMessageTypeString);

                    if (!valueExists(csv, 2)) {
                        LOGGER.error("System message needs more arguments! Received: {}", (Object) csv);
                        return;
                    }

                    switch (derivativeSystemMessage) {
                        case SYMBOL_LIMIT_REACHED:
                            handleSymbolLimitReachedMessage(csv);
                            break;
                        case REPLACED_PREVIOUSLY_WATCHED_INTERVAL:
                            handleReplacedPreviouslyWatchedSymbolMessage(csv);
                            break;
                        case WATCHED_INTERVALS:
                            handleWatchedIntervalsMessage(csv);
                            break;
                        default:
                            LOGGER.error("Unhandled message type: {}", derivativeSystemMessage);
                    }
                } catch (IllegalArgumentException illegalArgumentException) {
                    LOGGER.error("Received unknown system message type: {}", csv[1], illegalArgumentException);
                }

                return;
            }

            // Check for 'BW' request responses
            if (valueEquals(csv, 0, DerivativeMessageType.SYMBOL_NOT_WATCHED.value())) {
                handleSymbolNotWatchedMessage(csv);
            } else if (valueExists(csv, 0)) {
                handleIntervalMessage(csv);
            } else {
                LOGGER.error("Received unknown message: {}", (Object) csv);
            }
        }
    }

    private void handleSymbolLimitReachedMessage(String[] csv) {
        if (derivativeFeedEventListener != null) {
            String symbol = csv[2];
            derivativeFeedEventListener.onSymbolLimitReached(symbol);
        }
    }

    private void handleReplacedPreviouslyWatchedSymbolMessage(String[] csv) {
        if (derivativeFeedEventListener != null) {
            String symbol = csv[2];
            derivativeFeedEventListener.onReplacedPreviouslyWatchedSymbol(symbol, valueExists(csv, 3) ? csv[3] : null);
        }
    }

    private void handleWatchedIntervalsMessage(String[] csv) {
        if (!watchedIntervalsFuturesQueue.isEmpty()) {
            SingleMessageFuture<List<WatchedInterval>> watchedIntervalsFuture = watchedIntervalsFuturesQueue.poll();
            try {
                watchedIntervalsFuture.complete(WATCHED_INTERVALS_CSV_MAPPER.mapToList(csv, 2));
            } catch (Exception exception) {
                watchedIntervalsFuture.completeExceptionally(exception);
            }
        } else {
            LOGGER.error("Received {} System message, but with no Future to handle it!",
                    DerivativeSystemMessageType.WATCHED_INTERVALS);
        }
    }

    private void handleSymbolNotWatchedMessage(String[] csv) {
        if (!valuePresent(csv, 1)) {
            LOGGER.error("Invalid message received: {}", (Object) csv);
            return;
        }

        if (derivativeFeedEventListener != null) {
            String symbol = csv[1];
            derivativeFeedEventListener.onSymbolNotWatched(symbol);
        }
    }

    private void handleIntervalMessage(String[] csv) {
        String requestID = csv[0]; // All interval messages on this feed should start with a Request ID

        IntervalListenerData intervalListenerData = intervalListenerDataOfRequestIDs.get(requestID);
        if (intervalListenerData == null) {
            LOGGER.trace("Received Interval message, but no listener could be found for Request ID: {}", requestID);
            return;
        }

        try {
            Interval interval = INTERVAL_CSV_MAPPER.map(csv, 1);
            intervalListenerData.getIntervalListener().onMessageReceived(interval);
        } catch (Exception exception) {
            intervalListenerData.getIntervalListener().onMessageException(exception);
        }
    }

    @Override
    protected void onFeedSocketException(Exception exception) {
        intervalListenerDataOfRequestIDs.values().stream()
                .map(IntervalListenerData::getIntervalListener)
                .forEach(listener -> listener.onMessageException(exception));
        watchedIntervalsFuturesQueue.forEach(future -> future.completeExceptionally(exception));
    }

    @Override
    protected void onFeedSocketClose() {
        onFeedSocketException(new RuntimeException("Feed socket closed normally while a request was active!"));
    }

    //
    // START Feed commands
    //

    /**
     * Request a new interval bar watch based on parameters retrieving history based on the same set of parameters. This
     * sends a {@link DerivativeCommand#BAR_WATCH} request.
     *
     * @param symbol              the symbol to watch
     * @param intervalLength      the interval in seconds/volume/trades (depending on {@link IntervalType}).
     *                            <br>
     *                            {@link IntervalType#SECONDS} must be: in the range of 1 through 300 (inclusive) OR in
     *                            the range of 300 through 3600 (inclusive) AND be divisible by 60 (1 min) OR equal to
     *                            7200 (2 hour).
     *                            <br>
     *                            {@link IntervalType#TICKS} must be 2 or greater.
     *                            <br>
     *                            {@link IntervalType#VOLUME} must be of 100 or greater.
     * @param beginDateTime       the earliest date/time to receive data for (optional)
     * @param maxDaysOfDataPoints the maximum number of trading days to be retrieved (optional)
     * @param maxDataPoints       the maximum number of datapoints to be retrieved (optional)
     * @param beginFilterTime     allows you to specify the earliest time of day (Eastern) for which to receive data.
     *                            (optional)
     * @param endFilterTime       allows you to specify the latest time of day (Eastern) for which to receive data.
     *                            (optional)
     * @param intervalType        the {@link IntervalType}
     * @param updateIntervalDelay the number of seconds before sending out an updated bar (defaults to 0) (optional)
     * @param intervalListener    the {@link FeedMessageListener} of {@link Interval}s for this request
     *
     * @throws IOException thrown for {@link IOException}s
     */
    public void requestIntervalWatch(String symbol, int intervalLength, LocalDateTime beginDateTime,
            Integer maxDaysOfDataPoints, Integer maxDataPoints, LocalTime beginFilterTime, LocalTime endFilterTime,
            IntervalType intervalType, Integer updateIntervalDelay, FeedMessageListener<Interval> intervalListener)
            throws IOException {
        checkNotNull(symbol);
        checkNotNull(intervalType);

        String requestID = requestIDFeedHelper.getNewRequestID();
        StringBuilder requestBuilder = new StringBuilder();

        requestBuilder.append(DerivativeCommand.BAR_WATCH.value()).append(",");
        requestBuilder.append(symbol).append(",");
        requestBuilder.append(intervalLength).append(",");

        if (beginDateTime != null) {
            requestBuilder.append(DATE_SPACE_TIME.format(beginDateTime));
        }
        requestBuilder.append(",");

        if (maxDaysOfDataPoints != null) {
            requestBuilder.append(maxDaysOfDataPoints);
        }
        requestBuilder.append(",");

        if (maxDataPoints != null) {
            requestBuilder.append(maxDataPoints);
        }
        requestBuilder.append(",");

        if (beginFilterTime != null) {
            requestBuilder.append(TIME.format(beginFilterTime));
        }
        requestBuilder.append(",");

        if (endFilterTime != null) {
            requestBuilder.append(TIME.format(endFilterTime));
        }
        requestBuilder.append(",");

        requestBuilder.append(requestID).append(",");
        requestBuilder.append(intervalType.value()).append(",");
        requestBuilder.append(","); // For reserved value

        if (updateIntervalDelay != null) {
            requestBuilder.append(updateIntervalDelay);
        }

        requestBuilder.append(LineEnding.CR_LF.getASCIIString());

        synchronized (messageReceivedLock) {
            intervalListenerDataOfRequestIDs.put(requestID, new IntervalListenerData(intervalListener, symbol));
        }

        sendAndLogMessage(requestBuilder.toString());
    }

    /**
     * Remove a watch request for the given <code>symbol</code>. This sends a {@link DerivativeCommand#BAR_REMOVE}
     * request.
     *
     * @param symbol the symbol to unwatch
     *
     * @throws IOException thrown for {@link IOException}s
     */
    public void requestIntervalWatchRemoval(String symbol) throws IOException {
        requestIntervalWatchRemoval(null, symbol);
    }

    /**
     * Remove a watch request given a {@link FeedMessageListener} of {@link Interval}s. This sends a
     * {@link DerivativeCommand#BAR_REMOVE} request.
     *
     * @param intervalListener the {@link FeedMessageListener} to unwatch
     *
     * @throws IOException thrown for {@link IOException}s
     */
    public void requestIntervalWatchRemoval(FeedMessageListener<Interval> intervalListener) throws IOException {
        requestIntervalWatchRemoval(intervalListener, null);
    }

    /**
     * Remove a watch request given a {@link FeedMessageListener} of {@link Interval}s or the given <code>symbol</code>.
     * This sends a {@link DerivativeCommand#BAR_REMOVE} request.
     *
     * @param intervalListener the {@link FeedMessageListener} to unwatch
     * @param symbol           the symbol to unwatch
     *
     * @throws IOException thrown for {@link IOException}s
     */
    private void requestIntervalWatchRemoval(FeedMessageListener<Interval> intervalListener, String symbol)
            throws IOException {
        checkArgument(intervalListener != null ^ symbol != null);

        synchronized (messageReceivedLock) {
            Set<String> requestIDsToRemove = new HashSet<>();

            for (Map.Entry<String, IntervalListenerData> entry : intervalListenerDataOfRequestIDs.entrySet()) {
                String requestID = entry.getKey();
                IntervalListenerData intervalListenerData = entry.getValue();

                if (intervalListener != null && !intervalListenerData.getIntervalListener().equals(intervalListener)) {
                    continue;
                } else if (symbol != null && !intervalListenerData.getSymbol().equals(symbol)) {
                    continue;
                }

                StringBuilder requestBuilder = new StringBuilder();
                requestBuilder.append(DerivativeCommand.BAR_REMOVE.value()).append(",");
                requestBuilder.append(intervalListenerData.getSymbol()).append(",");
                requestBuilder.append(requestID);
                requestBuilder.append(LineEnding.CR_LF.getASCIIString());

                sendAndLogMessage(requestBuilder.toString());

                requestIDsToRemove.add(requestID);
                requestIDFeedHelper.removeRequestID(requestID);
            }

            requestIDsToRemove.forEach(intervalListenerDataOfRequestIDs::remove);
        }
    }

    /**
     * Request a list of all the current watch requests. This sends a {@link DerivativeSystemCommand#REQUEST_WATCHES}
     * request.
     *
     * @return a {@link SingleMessageFuture} of {@link WatchedInterval}s
     *
     * @throws IOException thrown for {@link IOException}s
     */
    public SingleMessageFuture<List<WatchedInterval>> requestWatchedIntervals() throws IOException {
        StringBuilder requestBuilder = new StringBuilder();
        requestBuilder.append(FeedCommand.SYSTEM.value()).append(",");
        requestBuilder.append(DerivativeSystemCommand.REQUEST_WATCHES.value());
        requestBuilder.append(LineEnding.CR_LF.getASCIIString());

        SingleMessageFuture<List<WatchedInterval>> watchedIntervalsFuture = new SingleMessageFuture<>();
        synchronized (messageReceivedLock) {
            watchedIntervalsFuturesQueue.add(watchedIntervalsFuture);
        }

        sendAndLogMessage(requestBuilder.toString());

        return watchedIntervalsFuture;
    }

    /**
     * Requests removal of all currently watched intervals. This sends a {@link DerivativeSystemCommand#UNWATCH_ALL}
     * request.
     *
     * @throws IOException thrown for {@link IOException}s
     */
    public void requestUnwatchAll() throws IOException {
        StringBuilder requestBuilder = new StringBuilder();
        requestBuilder.append(FeedCommand.SYSTEM.value()).append(",");
        requestBuilder.append(DerivativeSystemCommand.UNWATCH_ALL.value());
        requestBuilder.append(LineEnding.CR_LF.getASCIIString());

        synchronized (messageReceivedLock) {
            intervalListenerDataOfRequestIDs.clear();
        }

        sendAndLogMessage(requestBuilder.toString());
    }

    //
    // END Feed commands
    //

    /**
     * Gets the watched symbols for the given {@link FeedMessageListener} of {@link Interval}s.
     *
     * @param intervalListener the {@link FeedMessageListener} of {@link Interval}s
     *
     * @return a {@link Set} of {@link String}s
     */
    public Set<String> getWatchedSymbols(FeedMessageListener<Interval> intervalListener) {
        synchronized (messageReceivedLock) {
            return intervalListenerDataOfRequestIDs.values().stream()
                    .filter(intervalListenerData -> intervalListenerData.getIntervalListener().equals(intervalListener))
                    .map(IntervalListenerData::getSymbol)
                    .collect(Collectors.toSet());
        }
    }

    /**
     * Gets {@link FeedMessageListener} of {@link Interval}s for the given <code>symbol</code>.
     *
     * @param symbol the symbol
     *
     * @return a {@link Set} of {@link FeedMessageListener}s of {@link Interval}
     */
    public Set<FeedMessageListener<Interval>> getIntervalListeners(String symbol) {
        synchronized (messageReceivedLock) {
            return intervalListenerDataOfRequestIDs.values().stream()
                    .filter(intervalListenerData -> intervalListenerData.getSymbol().equals(symbol))
                    .map(IntervalListenerData::getIntervalListener)
                    .collect(Collectors.toSet());
        }
    }

    /**
     * Gets {@link #derivativeFeedEventListener}.
     *
     * @return the {@link DerivativeFeedEventListener}
     */
    public DerivativeFeedEventListener getDerivativeFeedEventListener() {
        return derivativeFeedEventListener;
    }

    /**
     * Sets {@link #derivativeFeedEventListener}.
     *
     * @param derivativeFeedEventListener the {@link DerivativeFeedEventListener}
     */
    public void setDerivativeFeedEventListener(DerivativeFeedEventListener derivativeFeedEventListener) {
        synchronized (messageReceivedLock) {
            this.derivativeFeedEventListener = derivativeFeedEventListener;
        }
    }

    /**
     * {@link IntervalListenerData} contains data for a {@link FeedMessageListener} of {@link Interval}s.
     */
    private static class IntervalListenerData {

        private final FeedMessageListener<Interval> intervalListener;
        private final String symbol;

        /**
         * Instantiates a new {@link IntervalListenerData}.
         *
         * @param intervalListener the {@link FeedMessageListener} of {@link Interval}s
         * @param symbol           the symbol of the {@link FeedMessageListener} of {@link Interval}s
         */
        public IntervalListenerData(FeedMessageListener<Interval> intervalListener, String symbol) {
            this.intervalListener = intervalListener;
            this.symbol = symbol;
        }

        /**
         * Gets {@link #intervalListener}.
         *
         * @return a {@link FeedMessageListener} of {@link Interval}s
         */
        public FeedMessageListener<Interval> getIntervalListener() {
            return intervalListener;
        }

        /**
         * Gets {@link #symbol}.
         *
         * @return a {@link String}
         */
        public String getSymbol() {
            return symbol;
        }
    }
}
