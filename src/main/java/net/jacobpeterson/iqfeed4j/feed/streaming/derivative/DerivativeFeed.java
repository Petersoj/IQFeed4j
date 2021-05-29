package net.jacobpeterson.iqfeed4j.feed.streaming.derivative;

import net.jacobpeterson.iqfeed4j.feed.RequestIDFeedHelper;
import net.jacobpeterson.iqfeed4j.feed.SingleMessageFuture;
import net.jacobpeterson.iqfeed4j.feed.streaming.AbstractServerConnectionFeed;
import net.jacobpeterson.iqfeed4j.model.feedenums.FeedMessageType;
import net.jacobpeterson.iqfeed4j.model.feedenums.interval.IntervalType;
import net.jacobpeterson.iqfeed4j.model.feedenums.streaming.derivative.DerivativeSystemMessage;
import net.jacobpeterson.iqfeed4j.model.streaming.derivative.Interval;
import net.jacobpeterson.iqfeed4j.model.streaming.derivative.WatchedInterval;
import net.jacobpeterson.iqfeed4j.util.csv.mapper.IndexCSVMapper;
import net.jacobpeterson.iqfeed4j.util.csv.mapper.NestedListCSVMapper;
import net.jacobpeterson.iqfeed4j.util.map.MapUtil;
import net.jacobpeterson.iqfeed4j.util.string.LineEnding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static net.jacobpeterson.iqfeed4j.util.csv.CSVUtil.valueEquals;
import static net.jacobpeterson.iqfeed4j.util.csv.CSVUtil.valueExists;
import static net.jacobpeterson.iqfeed4j.util.csv.CSVUtil.valuePresent;
import static net.jacobpeterson.iqfeed4j.util.csv.mapper.CSVMapper.DateTimeConverters.DASHED_DATE_SPACE_TIME;
import static net.jacobpeterson.iqfeed4j.util.csv.mapper.CSVMapper.DateTimeFormatters.DATE_SPACE_TIME;
import static net.jacobpeterson.iqfeed4j.util.csv.mapper.CSVMapper.DateTimeFormatters.TIME;
import static net.jacobpeterson.iqfeed4j.util.csv.mapper.CSVMapper.PrimitiveConvertors.DOUBLE;
import static net.jacobpeterson.iqfeed4j.util.csv.mapper.CSVMapper.PrimitiveConvertors.INT;
import static net.jacobpeterson.iqfeed4j.util.csv.mapper.CSVMapper.PrimitiveConvertors.STRING;

/**
 * {@link DerivativeFeed} is an {@link AbstractServerConnectionFeed} for derivative tick data (aka interval/bar data).
 */
public class DerivativeFeed extends AbstractServerConnectionFeed {

    private static final Logger LOGGER = LoggerFactory.getLogger(DerivativeFeed.class);
    private static final String FEED_NAME_SUFFIX = " Derivative Feed";
    private static final String SYMBOL_NOT_WATCHED_MESSAGE_PREFIX = "n";
    private static final IndexCSVMapper<Interval> INTERVAL_CSV_MAPPER;
    private static final NestedListCSVMapper<WatchedInterval> WATCHED_INTERVALS_CSV_MAPPER;

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
        INTERVAL_CSV_MAPPER.addMapping(Interval::setCumulativeVolume, INT);
        INTERVAL_CSV_MAPPER.addMapping(Interval::setIntervalVolume, INT);
        INTERVAL_CSV_MAPPER.addMapping(Interval::setNumberOfTrades, INT);

        WATCHED_INTERVALS_CSV_MAPPER = new NestedListCSVMapper<>(ArrayList::new, WatchedInterval::new, 3);
        WATCHED_INTERVALS_CSV_MAPPER.addMapping(WatchedInterval::setSymbol, STRING);
        WATCHED_INTERVALS_CSV_MAPPER.addMapping(WatchedInterval::setInterval, INT);
        WATCHED_INTERVALS_CSV_MAPPER.addMapping(WatchedInterval::setRequestID, STRING);
    }

    protected final Object messageReceivedLock;
    protected final RequestIDFeedHelper requestIDFeedHelper;
    protected final HashMap<String, IntervalListener> intervalListenersOfRequestIDs;
    protected final HashMap<String, List<IntervalListener>> intervalListenersOfWatchedSymbols;
    protected SingleMessageFuture<List<WatchedInterval>> watchedIntervalsFuture;

    /**
     * Instantiates a new {@link DerivativeFeed}.
     *
     * @param derivativeFeedName the {@link DerivativeFeed} name
     * @param hostname           the hostname
     * @param port               the port
     */
    public DerivativeFeed(String derivativeFeedName, String hostname, int port) {
        super(derivativeFeedName + FEED_NAME_SUFFIX, hostname, port, COMMA_DELIMITED_SPLITTER);

        messageReceivedLock = new Object();
        requestIDFeedHelper = new RequestIDFeedHelper();
        intervalListenersOfRequestIDs = new HashMap<>();
        intervalListenersOfWatchedSymbols = new HashMap<>();
    }

    @Override
    protected void onMessageReceived(String[] csv) {
        super.onMessageReceived(csv);

        if (valueEquals(csv, 0, FeedMessageType.ERROR.value())) {
            LOGGER.error("Received error message! {}", (Object) csv);
            return;
        }

        synchronized (messageReceivedLock) {
            // Handle derivative 'SYSTEM' messages
            if (valueEquals(csv, 0, FeedMessageType.SYSTEM.value())) {
                try {
                    DerivativeSystemMessage derivativeSystemMessage = DerivativeSystemMessage.fromValue(csv[1]);

                    if (!valueExists(csv, 2)) {
                        LOGGER.error("System message needs more arguments!");
                        return;
                    }

                    switch (derivativeSystemMessage) {
                        case SYMBOL_LIMIT_REACHED:
                        case REPLACED_PREVIOUSLY_WATCHED_INTERVAL:
                            String symbol = csv[2];

                            List<IntervalListener> intervalListeners = intervalListenersOfWatchedSymbols.get(symbol);
                            if (intervalListeners == null) {
                                LOGGER.warn("Received {} System message, but no listener could be found for symbol: {}",
                                        derivativeSystemMessage, symbol);
                                return;
                            }

                            switch (derivativeSystemMessage) {
                                case SYMBOL_LIMIT_REACHED:
                                    intervalListeners.forEach(listener -> listener.onSymbolLimitReached(symbol));
                                    break;
                                case REPLACED_PREVIOUSLY_WATCHED_INTERVAL:
                                    if (valueExists(csv, 3)) {
                                        String requestID = csv[3];
                                        IntervalListener intervalListener =
                                                intervalListenersOfRequestIDs.get(requestID);
                                        if (intervalListener == null) {
                                            LOGGER.warn("Received {} System message, but no listener could " +
                                                    "be found for Request ID: {}", derivativeSystemMessage, requestID);
                                            return;
                                        }
                                        intervalListener.onReplacedPreviouslyWatchedSymbol(symbol, requestID);
                                    } else {
                                        intervalListeners.forEach(listener ->
                                                listener.onReplacedPreviouslyWatchedSymbol(symbol, null));
                                    }
                                    break;
                            }
                            break;
                        case WATCHED_INTERVALS:
                            if (watchedIntervalsFuture != null) {
                                try {
                                    watchedIntervalsFuture.complete(WATCHED_INTERVALS_CSV_MAPPER.mapToList(csv, 2));
                                } catch (Exception exception) {
                                    watchedIntervalsFuture.completeExceptionally(exception);
                                }

                                watchedIntervalsFuture = null;
                            } else {
                                LOGGER.error("Received {} System message, but with no Future to handle it!",
                                        derivativeSystemMessage);
                            }
                            break;
                        default:
                            throw new UnsupportedOperationException();
                    }
                } catch (Exception ignored) {} // Only handle 'DerivativeSystemMessage's here

                return;
            }

            // Check for 'BW' request responses
            if (valueEquals(csv, 0, SYMBOL_NOT_WATCHED_MESSAGE_PREFIX)) {
                if (!valuePresent(csv, 1)) {
                    LOGGER.error("Invalid message received: {}", (Object) csv);
                    return;
                }

                String symbol = csv[1];
                List<IntervalListener> intervalListeners = intervalListenersOfWatchedSymbols.get(symbol);
                if (intervalListeners == null) {
                    LOGGER.warn("Received '{}' message, but no listeners could be found for symbol: {}",
                            SYMBOL_NOT_WATCHED_MESSAGE_PREFIX, symbol);
                    return;
                }
                intervalListeners.forEach(listener -> listener.onSymbolNotWatched(symbol));
            } else if (valueExists(csv, 0)) {
                String requestID = csv[0]; // All interval messages on this feed should start with a Request ID

                IntervalListener intervalListener = intervalListenersOfRequestIDs.get(requestID);
                if (intervalListener == null) {
                    LOGGER.warn("Received Interval message, but no listener could be found for Request ID: {}",
                            requestID);
                    return;
                }

                try {
                    Interval interval = INTERVAL_CSV_MAPPER.map(csv, 1);
                    intervalListener.onMessageReceived(interval);
                } catch (Exception exception) {
                    intervalListener.onMessageException(exception);
                }
            } else {
                LOGGER.error("Received unknown message: {}", (Object) csv);
            }
        }
    }

    //
    // START Feed commands
    //

    /**
     * Request a new interval bar watch based on parameters retrieving history based on the same set of parameters. This
     * sends an BW request. This method is thread-safe.
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
     * @param intervalListener    the {@link IntervalListener} for this request
     *
     * @throws IOException thrown for {@link IOException}s
     */
    public void requestIntervalWatch(String symbol, int intervalLength, LocalDateTime beginDateTime,
            Integer maxDaysOfDataPoints, Integer maxDataPoints, LocalTime beginFilterTime, LocalTime endFilterTime,
            IntervalType intervalType, Integer updateIntervalDelay, IntervalListener intervalListener)
            throws IOException {
        checkNotNull(symbol);
        checkNotNull(intervalType);

        String requestID = requestIDFeedHelper.getNewRequestID();
        StringBuilder requestBuilder = new StringBuilder();

        requestBuilder.append("BW").append(",");
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
            intervalListenersOfRequestIDs.put(requestID, intervalListener);

            List<IntervalListener> intervalListeners =
                    intervalListenersOfWatchedSymbols.computeIfAbsent(symbol, k -> new ArrayList<>());
            intervalListeners.add(intervalListener);
        }
        String requestString = requestBuilder.toString();
        LOGGER.debug("Sending request: {}", requestString);
        sendMessage(requestString);
    }

    /**
     * Remove a watch request given a {@link IntervalListener}. This sends an BR request. This method is thread-safe.
     *
     * @param intervalListener the {@link IntervalListener} to remove
     *
     * @throws Exception thrown for {@link Exception}s
     */
    public void requestIntervalWatchRemoval(IntervalListener intervalListener) throws Exception {
        checkNotNull(intervalListener);

        String symbol = getWatchedSymbol(intervalListener);
        String requestID = getRequestID(intervalListener);
        checkNotNull(symbol);
        checkNotNull(requestID);

        StringBuilder requestBuilder = new StringBuilder();
        requestBuilder.append("BR").append(",");
        requestBuilder.append(symbol).append(",");
        requestBuilder.append(requestID);
        requestBuilder.append(LineEnding.CR_LF.getASCIIString());

        synchronized (messageReceivedLock) {
            intervalListenersOfRequestIDs.remove(requestID);

            List<IntervalListener> intervalListeners = intervalListenersOfWatchedSymbols.get(symbol);
            if (intervalListeners != null) {
                intervalListeners.remove(intervalListener);
            }
        }
        String requestString = requestBuilder.toString();
        LOGGER.debug("Sending request: {}", requestString);
        sendMessage(requestString);
    }

    /**
     * Request a list of all the current watch requests. This sends an 'S,REQUEST WATCHES' request. This method is
     * thread-safe.
     *
     * @return a {@link SingleMessageFuture} of {@link WatchedInterval}s
     *
     * @throws IOException thrown for {@link IOException}s
     */
    public SingleMessageFuture<List<WatchedInterval>> requestWatchedIntervals() throws IOException {
        synchronized (messageReceivedLock) {
            if (watchedIntervalsFuture != null) {
                return watchedIntervalsFuture;
            }
        }

        StringBuilder requestBuilder = new StringBuilder();
        requestBuilder.append(FeedMessageType.SYSTEM.value()).append(",");
        requestBuilder.append("REQUEST WATCHES");
        requestBuilder.append(LineEnding.CR_LF.getASCIIString());

        synchronized (messageReceivedLock) {
            watchedIntervalsFuture = new SingleMessageFuture<>();
        }
        String requestString = requestBuilder.toString();
        LOGGER.debug("Sending request: {}", requestString);
        sendMessage(requestString);

        return watchedIntervalsFuture;
    }

    /**
     * Requests removal of all currently watched intervals. This sends an 'S,UNWATCH ALL' request. This method is
     * thread-safe.
     *
     * @throws IOException thrown for {@link IOException}s
     */
    public void requestUnwatchAll() throws IOException {
        StringBuilder requestBuilder = new StringBuilder();
        requestBuilder.append(FeedMessageType.SYSTEM.value()).append(",");
        requestBuilder.append("UNWATCH ALL");
        requestBuilder.append(LineEnding.CR_LF.getASCIIString());

        synchronized (messageReceivedLock) {
            intervalListenersOfRequestIDs.clear();
            intervalListenersOfWatchedSymbols.clear();
        }
        String requestString = requestBuilder.toString();
        LOGGER.debug("Sending request: {}", requestString);
        sendMessage(requestString);
    }

    //
    // END Feed commands
    //

    /**
     * Gets an {@link IntervalListener} for the given 'requestID' if one exists.
     *
     * @param requestID the Request ID
     *
     * @return the {@link IntervalListener} or null
     */
    public IntervalListener getIntervalListener(String requestID) {
        synchronized (messageReceivedLock) {
            return intervalListenersOfRequestIDs.get(requestID);
        }
    }

    /**
     * Gets a Request ID for the given {@link IntervalListener} if one exists.
     *
     * @param intervalListener the {@link IntervalListener}
     *
     * @return the Request ID or null
     */
    public String getRequestID(IntervalListener intervalListener) {
        synchronized (messageReceivedLock) {
            return MapUtil.getKeyByValue(intervalListenersOfRequestIDs, intervalListener);
        }
    }

    /**
     * Gets the watched symbol for the given {@link IntervalListener}.
     *
     * @param intervalListener the {@link IntervalListener}
     *
     * @return the watched symbol
     */
    public String getWatchedSymbol(IntervalListener intervalListener) {
        synchronized (messageReceivedLock) {
            return intervalListenersOfWatchedSymbols.entrySet().stream()
                    .filter(entry -> entry.getValue().contains(intervalListener))
                    .map(Map.Entry::getKey)
                    .findFirst()
                    .orElse(null);
        }
    }
}
