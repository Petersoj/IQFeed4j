package net.jacobpeterson.iqfeed4j.feed.lookup.historical;

import net.jacobpeterson.iqfeed4j.feed.lookup.AbstractLookupFeed;
import net.jacobpeterson.iqfeed4j.feed.message.MultiMessageAccumulator;
import net.jacobpeterson.iqfeed4j.feed.message.MultiMessageListener;
import net.jacobpeterson.iqfeed4j.model.feed.common.interval.IntervalType;
import net.jacobpeterson.iqfeed4j.model.feed.lookup.historical.DatedInterval;
import net.jacobpeterson.iqfeed4j.model.feed.lookup.historical.Interval;
import net.jacobpeterson.iqfeed4j.model.feed.lookup.historical.Tick;
import net.jacobpeterson.iqfeed4j.model.feed.lookup.historical.enums.DataDirection;
import net.jacobpeterson.iqfeed4j.model.feed.lookup.historical.enums.HistoricalCommand;
import net.jacobpeterson.iqfeed4j.model.feed.lookup.historical.enums.PartialDatapoint;
import net.jacobpeterson.iqfeed4j.model.feed.lookup.historical.enums.TimeLabelPlacement;
import net.jacobpeterson.iqfeed4j.util.csv.mapper.AbstractCSVMapper.DateTimeFormatters;
import net.jacobpeterson.iqfeed4j.util.csv.mapper.index.IndexCSVMapper;
import net.jacobpeterson.iqfeed4j.util.string.LineEnding;
import net.jacobpeterson.iqfeed4j.util.tradecondition.TradeConditionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static net.jacobpeterson.iqfeed4j.util.csv.mapper.AbstractCSVMapper.DateTimeConverters.DASHED_DATE;
import static net.jacobpeterson.iqfeed4j.util.csv.mapper.AbstractCSVMapper.DateTimeConverters.DASHED_DATE_SPACE_TIME;
import static net.jacobpeterson.iqfeed4j.util.csv.mapper.AbstractCSVMapper.DateTimeConverters.DASHED_DATE_SPACE_TIME_FRACTIONAL;
import static net.jacobpeterson.iqfeed4j.util.csv.mapper.AbstractCSVMapper.PrimitiveConvertors.DOUBLE;
import static net.jacobpeterson.iqfeed4j.util.csv.mapper.AbstractCSVMapper.PrimitiveConvertors.INTEGER;
import static net.jacobpeterson.iqfeed4j.util.csv.mapper.AbstractCSVMapper.PrimitiveConvertors.LONG;
import static net.jacobpeterson.iqfeed4j.util.csv.mapper.AbstractCSVMapper.PrimitiveConvertors.SHORT;

/**
 * {@link HistoricalFeed} is an {@link AbstractLookupFeed} for historical data.
 */
public class HistoricalFeed extends AbstractLookupFeed {

    /**
     * This parameter is for "performance tweaking." It specifies the number of datapoints that IQConnect will group
     * together before sending the data to a client application. Due to the way TCP socket communications work, this
     * parameter WILL NOT indicate the number of datapoints your app will read from the socket at each read. Specifying
     * higher numbers should result in faster overall data transfer but slower "response time" where response time
     * indicates the amount of time between you making the request and getting the first data returned. A value of zero
     * will queue all data locally before delivering any of it (this is not recommended).
     * <br>
     * It is set to 150 be default, but can be changed.
     */
    public static int DATAPOINTS_PER_SEND = 150;

    private static final Logger LOGGER = LoggerFactory.getLogger(HistoricalFeed.class);
    protected static final String FEED_NAME_SUFFIX = " Historical";

    protected static final IndexCSVMapper<Tick> TICK_CSV_MAPPER;
    protected static final IndexCSVMapper<Interval> INTERVAL_CSV_MAPPER;
    protected static final IndexCSVMapper<DatedInterval> DATED_INTERVAL_CSV_MAPPER;

    static {
        // Add mappings with CSV indices analogous to line of execution

        TICK_CSV_MAPPER = new IndexCSVMapper<>(Tick::new);
        TICK_CSV_MAPPER.addMapping(Tick::setTimestamp, DASHED_DATE_SPACE_TIME_FRACTIONAL);
        TICK_CSV_MAPPER.addMapping(Tick::setLast, DOUBLE);
        TICK_CSV_MAPPER.addMapping(Tick::setLastSize, INTEGER);
        TICK_CSV_MAPPER.addMapping(Tick::setTotalVolume, LONG);
        TICK_CSV_MAPPER.addMapping(Tick::setBid, DOUBLE);
        TICK_CSV_MAPPER.addMapping(Tick::setAsk, DOUBLE);
        TICK_CSV_MAPPER.addMapping(Tick::setTickID, INTEGER);
        TICK_CSV_MAPPER.addMapping(Tick::setBasisForLast, Tick.BasisForLast::fromValue);
        TICK_CSV_MAPPER.addMapping(Tick::setTradeMarketCenter, SHORT);
        TICK_CSV_MAPPER.addMapping(Tick::setTradeConditions, TradeConditionUtil::listFromTradeConditionString);
        TICK_CSV_MAPPER.addMapping(Tick::setTradeAggressor, Tick.TradeAggressor::fromValue);
        TICK_CSV_MAPPER.addMapping(Tick::setDayCode, INTEGER);

        INTERVAL_CSV_MAPPER = new IndexCSVMapper<>(Interval::new);
        INTERVAL_CSV_MAPPER.addMapping(Interval::setTimestamp, DASHED_DATE_SPACE_TIME);
        INTERVAL_CSV_MAPPER.addMapping(Interval::setHigh, DOUBLE);
        INTERVAL_CSV_MAPPER.addMapping(Interval::setLow, DOUBLE);
        INTERVAL_CSV_MAPPER.addMapping(Interval::setOpen, DOUBLE);
        INTERVAL_CSV_MAPPER.addMapping(Interval::setClose, DOUBLE);
        INTERVAL_CSV_MAPPER.addMapping(Interval::setTotalVolume, LONG);
        INTERVAL_CSV_MAPPER.addMapping(Interval::setPeriodVolume, INTEGER);
        INTERVAL_CSV_MAPPER.addMapping(Interval::setNumberOfTrades, INTEGER);

        DATED_INTERVAL_CSV_MAPPER = new IndexCSVMapper<>(DatedInterval::new);
        DATED_INTERVAL_CSV_MAPPER.addMapping(DatedInterval::setDate, DASHED_DATE);
        DATED_INTERVAL_CSV_MAPPER.addMapping(DatedInterval::setHigh, DOUBLE);
        DATED_INTERVAL_CSV_MAPPER.addMapping(DatedInterval::setLow, DOUBLE);
        DATED_INTERVAL_CSV_MAPPER.addMapping(DatedInterval::setOpen, DOUBLE);
        DATED_INTERVAL_CSV_MAPPER.addMapping(DatedInterval::setClose, DOUBLE);
        DATED_INTERVAL_CSV_MAPPER.addMapping(DatedInterval::setPeriodVolume, INTEGER);
        DATED_INTERVAL_CSV_MAPPER.addMapping(DatedInterval::setOpenInterest, INTEGER);
    }

    protected final Object messageReceivedLock;
    protected final HashMap<String, MultiMessageListener<Tick>> tickListenersOfRequestIDs;
    protected final HashMap<String, MultiMessageListener<Interval>> intervalListenersOfRequestIDs;
    protected final HashMap<String, MultiMessageListener<DatedInterval>> datedIntervalListenersOfRequestIDs;

    /**
     * Instantiates a new {@link HistoricalFeed}.
     *
     * @param historicalFeedName the {@link HistoricalFeed} name
     * @param hostname           the hostname
     * @param port               the port
     */
    public HistoricalFeed(String historicalFeedName, String hostname, int port) {
        super(LOGGER, historicalFeedName + FEED_NAME_SUFFIX, hostname, port, COMMA_DELIMITED_SPLITTER);

        messageReceivedLock = new Object();
        tickListenersOfRequestIDs = new HashMap<>();
        intervalListenersOfRequestIDs = new HashMap<>();
        datedIntervalListenersOfRequestIDs = new HashMap<>();
    }

    @Override
    protected void onMessageReceived(String[] csv) {
        if (isErrorOrInvalidMessage(csv)) {
            return;
        }

        String requestID = csv[0];

        synchronized (messageReceivedLock) {
            if (handleStandardMultiMessage(csv, requestID, 2, tickListenersOfRequestIDs, TICK_CSV_MAPPER)) {
                return;
            }

            if (handleStandardMultiMessage(csv, requestID, 2, intervalListenersOfRequestIDs, INTERVAL_CSV_MAPPER)) {
                return;
            }

            if (handleStandardMultiMessage(csv, requestID, 2, datedIntervalListenersOfRequestIDs,
                    DATED_INTERVAL_CSV_MAPPER)) {
                return;
            }
        }
    }

    //
    // START Feed commands
    //

    /**
     * Retrieves up to <code>maxDataPoints</code> number of {@link Tick}s for the specified <code>symbol</code>. This
     * sends a {@link HistoricalCommand#HISTORICAL_TICKS_DATAPOINTS} request.
     *
     * @param symbol        the symbol. Max length of 30 characters.
     * @param maxDataPoints the maximum number of datapoints to be retrieved
     * @param dataDirection the data direction (defaults to {@link DataDirection#NEWEST_TO_OLDEST}) (optional)
     * @param ticksListener the {@link MultiMessageListener} for the requested {@link Tick}s
     *
     * @throws IOException thrown for {@link IOException}s
     */
    public void requestTicks(String symbol, int maxDataPoints, DataDirection dataDirection,
            MultiMessageListener<Tick> ticksListener) throws IOException {
        checkNotNull(symbol);
        checkNotNull(ticksListener);

        String requestID = requestIDFeedHelper.getNewRequestID();
        StringBuilder requestBuilder = new StringBuilder();

        requestBuilder.append(HistoricalCommand.HISTORICAL_TICKS_DATAPOINTS.value()).append(",");
        requestBuilder.append(symbol).append(",");
        requestBuilder.append(maxDataPoints).append(",");

        if (dataDirection != null) {
            requestBuilder.append(dataDirection.value());
        }
        requestBuilder.append(",");

        requestBuilder.append(requestID).append(",");
        requestBuilder.append(DATAPOINTS_PER_SEND);
        requestBuilder.append(LineEnding.CR_LF.getASCIIString());

        synchronized (messageReceivedLock) {
            tickListenersOfRequestIDs.put(requestID, ticksListener);
        }

        sendAndLogMessage(requestBuilder.toString());
    }

    /**
     * Calls {@link #requestTicks(String, int, DataDirection, MultiMessageListener)} and will accumulate all requested
     * data into a {@link List} which can be consumed later.
     *
     * @return a {@link List} of {@link Tick}s
     *
     * @throws IOException          thrown for {@link IOException}s
     * @throws ExecutionException   thrown for {@link ExecutionException}s
     * @throws InterruptedException thrown for {@link InterruptedException}s
     */
    public List<Tick> requestTicks(String symbol, int maxDataPoints, DataDirection dataDirection)
            throws IOException, ExecutionException, InterruptedException {
        MultiMessageAccumulator<Tick> asyncListener = new MultiMessageAccumulator<>();
        requestTicks(symbol, maxDataPoints, dataDirection, asyncListener);
        return asyncListener.getMessages();
    }

    /**
     * Retrieves {@link Tick}s for the previous <code>maxDays</code> days for the specified <code>symbol</code>. This
     * sends a {@link HistoricalCommand#HISTORICAL_TICKS_DAYS} request.
     *
     * @param symbol          the symbol. Max length of 30 characters.
     * @param maxDays         the max days
     * @param maxDataPoints   the maximum number of datapoints to be retrieved (optional)
     * @param beginFilterTime allows you to specify the earliest time of day (Eastern) for which to receive data
     *                        (optional)
     * @param endFilterTime   allows you to specify the latest time of day (Eastern) for which to receive data
     *                        (optional)
     * @param dataDirection   the data direction (defaults to {@link DataDirection#NEWEST_TO_OLDEST}) (optional)
     * @param ticksListener   the {@link MultiMessageListener} for the requested {@link Tick}s
     *
     * @throws IOException thrown for {@link IOException}s
     */
    public void requestTicks(String symbol, int maxDays, Integer maxDataPoints, LocalTime beginFilterTime,
            LocalTime endFilterTime, DataDirection dataDirection, MultiMessageListener<Tick> ticksListener)
            throws IOException {
        checkNotNull(symbol);
        checkNotNull(ticksListener);

        String requestID = requestIDFeedHelper.getNewRequestID();
        StringBuilder requestBuilder = new StringBuilder();

        requestBuilder.append(HistoricalCommand.HISTORICAL_TICKS_DAYS.value()).append(",");
        requestBuilder.append(symbol).append(",");
        requestBuilder.append(maxDays).append(",");

        if (maxDataPoints != null) {
            requestBuilder.append(maxDataPoints);
        }
        requestBuilder.append(",");

        if (beginFilterTime != null) {
            requestBuilder.append(beginFilterTime.format(DateTimeFormatters.TIME));
        }
        requestBuilder.append(",");

        if (endFilterTime != null) {
            requestBuilder.append(endFilterTime.format(DateTimeFormatters.TIME));
        }
        requestBuilder.append(",");

        if (dataDirection != null) {
            requestBuilder.append(dataDirection.value());
        }
        requestBuilder.append(",");

        requestBuilder.append(requestID).append(",");
        requestBuilder.append(DATAPOINTS_PER_SEND);
        requestBuilder.append(LineEnding.CR_LF.getASCIIString());

        synchronized (messageReceivedLock) {
            tickListenersOfRequestIDs.put(requestID, ticksListener);
        }

        sendAndLogMessage(requestBuilder.toString());
    }

    /**
     * Calls {@link #requestTicks(String, int, Integer, LocalTime, LocalTime, DataDirection, MultiMessageListener)} and
     * will accumulate all requested data into a {@link List} which can be consumed later.
     *
     * @return a {@link List} of {@link Tick}s
     *
     * @throws IOException          thrown for {@link IOException}s
     * @throws ExecutionException   thrown for {@link ExecutionException}s
     * @throws InterruptedException thrown for {@link InterruptedException}s
     */
    public List<Tick> requestTicks(String symbol, int maxDays, Integer maxDataPoints, LocalTime beginFilterTime,
            LocalTime endFilterTime, DataDirection dataDirection)
            throws IOException, ExecutionException, InterruptedException {
        MultiMessageAccumulator<Tick> asyncListener = new MultiMessageAccumulator<>();
        requestTicks(symbol, maxDays, maxDataPoints, beginFilterTime, endFilterTime, dataDirection, asyncListener);
        return asyncListener.getMessages();
    }

    /**
     * Retrieves {@link Tick} data between <code>beginDateTime</code> and <code>endDateTime</code> for the specified
     * <code>symbol</code>. This sends a {@link HistoricalCommand#HISTORICAL_TICKS_DATETIMES} request.
     *
     * @param symbol          the symbol. Max length of 30 characters.
     * @param beginDateTime   earliest date/time (Eastern) to receive data for.
     * @param endDateTime     most recent date/time (Eastern) to receive data for
     * @param maxDataPoints   the maximum number of datapoints to be retrieved (optional)
     * @param beginFilterTime allows you to specify the earliest time of day (Eastern) for which to receive data
     *                        (optional)
     * @param endFilterTime   allows you to specify the latest time of day (Eastern) for which to receive data
     *                        (optional)
     * @param dataDirection   the data direction (defaults to {@link DataDirection#NEWEST_TO_OLDEST}) (optional)
     * @param ticksListener   the {@link MultiMessageListener} for the requested {@link Tick}s
     *
     * @throws IOException thrown for {@link IOException}s
     */
    public void requestTicks(String symbol, LocalDateTime beginDateTime, LocalDateTime endDateTime,
            Integer maxDataPoints, LocalTime beginFilterTime, LocalTime endFilterTime, DataDirection dataDirection,
            MultiMessageListener<Tick> ticksListener) throws IOException {
        checkNotNull(symbol);
        checkArgument(beginDateTime != null || endDateTime != null);
        checkNotNull(ticksListener);

        String requestID = requestIDFeedHelper.getNewRequestID();
        StringBuilder requestBuilder = new StringBuilder();

        requestBuilder.append(HistoricalCommand.HISTORICAL_TICKS_DATETIMES.value()).append(",");
        requestBuilder.append(symbol).append(",");

        if (beginDateTime != null) {
            requestBuilder.append(beginDateTime.format(DateTimeFormatters.DATE_SPACE_TIME));
        }
        requestBuilder.append(",");

        if (endDateTime != null) {
            requestBuilder.append(endDateTime.format(DateTimeFormatters.DATE_SPACE_TIME));
        }
        requestBuilder.append(",");

        if (maxDataPoints != null) {
            requestBuilder.append(maxDataPoints);
        }
        requestBuilder.append(",");

        if (beginFilterTime != null) {
            requestBuilder.append(beginFilterTime.format(DateTimeFormatters.TIME));
        }
        requestBuilder.append(",");

        if (endFilterTime != null) {
            requestBuilder.append(endFilterTime.format(DateTimeFormatters.TIME));
        }
        requestBuilder.append(",");

        if (dataDirection != null) {
            requestBuilder.append(dataDirection.value());
        }
        requestBuilder.append(",");

        requestBuilder.append(requestID).append(",");
        requestBuilder.append(DATAPOINTS_PER_SEND);
        requestBuilder.append(LineEnding.CR_LF.getASCIIString());

        synchronized (messageReceivedLock) {
            tickListenersOfRequestIDs.put(requestID, ticksListener);
        }

        sendAndLogMessage(requestBuilder.toString());
    }

    /**
     * Calls {@link #requestTicks(String, LocalDateTime, LocalDateTime, Integer, LocalTime, LocalTime, DataDirection,
     * MultiMessageListener)} and will accumulate all requested data into a {@link List} which can be consumed later.
     *
     * @return a {@link List} of {@link Tick}s
     *
     * @throws IOException          thrown for {@link IOException}s
     * @throws ExecutionException   thrown for {@link ExecutionException}s
     * @throws InterruptedException thrown for {@link InterruptedException}s
     */
    public List<Tick> requestTicks(String symbol, LocalDateTime beginDateTime, LocalDateTime endDateTime,
            Integer maxDataPoints, LocalTime beginFilterTime, LocalTime endFilterTime, DataDirection dataDirection)
            throws IOException, ExecutionException, InterruptedException {
        MultiMessageAccumulator<Tick> asyncListener = new MultiMessageAccumulator<>();
        requestTicks(symbol, beginDateTime, endDateTime, maxDataPoints, beginFilterTime, endFilterTime, dataDirection,
                asyncListener);
        return asyncListener.getMessages();
    }

    /**
     * Retrieves up to <code>maxDataPoints</code> number of {@link Interval}s for the specified <code>symbol</code>.
     * This sends a {@link HistoricalCommand#HISTORICAL_INTERVAL_DATAPOINTS} request.
     *
     * @param symbol            the symbol. Max length of 30 characters.
     * @param intervalLength    the interval length
     * @param maxDataPoints     the maximum number of datapoints to be retrieved
     * @param dataDirection     the data direction (defaults to {@link DataDirection#NEWEST_TO_OLDEST}) (optional)
     * @param intervalType      the {@link IntervalType} (defaults to {@link IntervalType#SECONDS} (optional)
     * @param intervalsListener the {@link MultiMessageListener} for the requested {@link Interval}s
     *
     * @throws IOException thrown for {@link IOException}s
     */
    public void requestIntervals(String symbol, int intervalLength, Integer maxDataPoints, DataDirection dataDirection,
            IntervalType intervalType, MultiMessageListener<Interval> intervalsListener) throws IOException {
        checkNotNull(symbol);
        checkNotNull(maxDataPoints);
        checkNotNull(intervalsListener);

        String requestID = requestIDFeedHelper.getNewRequestID();
        StringBuilder requestBuilder = new StringBuilder();

        requestBuilder.append(HistoricalCommand.HISTORICAL_INTERVAL_DATAPOINTS.value()).append(",");
        requestBuilder.append(symbol).append(",");
        requestBuilder.append(intervalLength).append(",");
        requestBuilder.append(maxDataPoints).append(",");

        if (dataDirection != null) {
            requestBuilder.append(dataDirection.value());
        }
        requestBuilder.append(",");

        requestBuilder.append(requestID).append(",");
        requestBuilder.append(DATAPOINTS_PER_SEND).append(",");

        if (intervalType != null) {
            requestBuilder.append(intervalType.value());
        }
        requestBuilder.append(",");

        requestBuilder.append(TimeLabelPlacement.BEGINNING);
        requestBuilder.append(LineEnding.CR_LF.getASCIIString());

        synchronized (messageReceivedLock) {
            intervalListenersOfRequestIDs.put(requestID, intervalsListener);
        }

        sendAndLogMessage(requestBuilder.toString());
    }

    /**
     * Calls {@link #requestIntervals(String, int, Integer, DataDirection, IntervalType, MultiMessageListener)} and will
     * accumulate all requested data into a {@link List} which can be consumed later.
     *
     * @return a {@link List} of {@link Interval}s
     *
     * @throws IOException          thrown for {@link IOException}s
     * @throws ExecutionException   thrown for {@link ExecutionException}s
     * @throws InterruptedException thrown for {@link InterruptedException}s
     */
    public List<Interval> requestIntervals(String symbol, int intervalLength, Integer maxDataPoints,
            DataDirection dataDirection, IntervalType intervalType)
            throws IOException, ExecutionException, InterruptedException {
        MultiMessageAccumulator<Interval> asyncListener = new MultiMessageAccumulator<>();
        requestIntervals(symbol, intervalLength, maxDataPoints, dataDirection, intervalType, asyncListener);
        return asyncListener.getMessages();
    }

    /**
     * Retrieves {@link Interval}s for <code>maxDays</code> days for the specified <code>symbol</code>. This sends a
     * {@link HistoricalCommand#HISTORICAL_INTERVAL_DAYS} request.
     *
     * @param symbol            the symbol. Max length of 30 characters.
     * @param intervalLength    the interval length
     * @param maxDays           the max days
     * @param maxDataPoints     the maximum number of datapoints to be retrieved (optional)
     * @param beginFilterTime   allows you to specify the earliest time of day (Eastern) for which to receive data
     *                          (optional)
     * @param endFilterTime     allows you to specify the latest time of day (Eastern) for which to receive data
     *                          (optional)
     * @param dataDirection     the data direction (defaults to {@link DataDirection#NEWEST_TO_OLDEST}) (optional)
     * @param intervalType      the {@link IntervalType} (defaults to {@link IntervalType#SECONDS} (optional)
     * @param intervalsListener the {@link MultiMessageListener} for the requested {@link Interval}s
     *
     * @throws IOException thrown for {@link IOException}s
     */
    public void requestIntervals(String symbol, int intervalLength, int maxDays, Integer maxDataPoints,
            LocalTime beginFilterTime, LocalTime endFilterTime, DataDirection dataDirection, IntervalType intervalType,
            MultiMessageListener<Interval> intervalsListener) throws IOException {
        checkNotNull(symbol);
        checkNotNull(intervalsListener);

        String requestID = requestIDFeedHelper.getNewRequestID();
        StringBuilder requestBuilder = new StringBuilder();

        requestBuilder.append(HistoricalCommand.HISTORICAL_INTERVAL_DAYS.value()).append(",");
        requestBuilder.append(symbol).append(",");
        requestBuilder.append(intervalLength).append(",");
        requestBuilder.append(maxDays).append(",");

        if (maxDataPoints != null) {
            requestBuilder.append(maxDataPoints);
        }
        requestBuilder.append(",");

        if (beginFilterTime != null) {
            requestBuilder.append(beginFilterTime.format(DateTimeFormatters.TIME));
        }
        requestBuilder.append(",");

        if (endFilterTime != null) {
            requestBuilder.append(endFilterTime.format(DateTimeFormatters.TIME));
        }
        requestBuilder.append(",");

        if (dataDirection != null) {
            requestBuilder.append(dataDirection.value());
        }
        requestBuilder.append(",");

        requestBuilder.append(requestID).append(",");
        requestBuilder.append(DATAPOINTS_PER_SEND).append(",");

        if (intervalType != null) {
            requestBuilder.append(intervalType.value());
        }
        requestBuilder.append(",");

        requestBuilder.append(TimeLabelPlacement.BEGINNING);
        requestBuilder.append(LineEnding.CR_LF.getASCIIString());

        synchronized (messageReceivedLock) {
            intervalListenersOfRequestIDs.put(requestID, intervalsListener);
        }

        sendAndLogMessage(requestBuilder.toString());
    }

    /**
     * Calls {@link #requestIntervals(String, int, int, Integer, LocalTime, LocalTime, DataDirection, IntervalType,
     * MultiMessageListener)} and will accumulate all requested data into a {@link List} which can be consumed later.
     *
     * @return a {@link List} of {@link Interval}s
     *
     * @throws IOException          thrown for {@link IOException}s
     * @throws ExecutionException   thrown for {@link ExecutionException}s
     * @throws InterruptedException thrown for {@link InterruptedException}s
     */
    public List<Interval> requestIntervals(String symbol, int intervalLength, int maxDays, Integer maxDataPoints,
            LocalTime beginFilterTime, LocalTime endFilterTime, DataDirection dataDirection, IntervalType intervalType)
            throws IOException, ExecutionException, InterruptedException {
        MultiMessageAccumulator<Interval> asyncListener = new MultiMessageAccumulator<>();
        requestIntervals(symbol, intervalLength, maxDays, maxDataPoints, beginFilterTime, endFilterTime, dataDirection,
                intervalType, asyncListener);
        return asyncListener.getMessages();
    }

    /**
     * Retrieves {@link Interval} data between <code>beginDateTime</code> and <code>endDateTime</code> for the
     * specified
     * <code>symbol</code>. This sends a {@link HistoricalCommand#HISTORICAL_INTERVAL_DATETIMES} request.
     *
     * @param symbol            the symbol. Max length of 30 characters.
     * @param intervalLength    the interval length
     * @param beginDateTime     earliest date/time (Eastern) to receive data for.
     * @param endDateTime       most recent date/time (Eastern) to receive data for
     * @param maxDataPoints     the maximum number of datapoints to be retrieved (optional)
     * @param beginFilterTime   allows you to specify the earliest time of day (Eastern) for which to receive data
     *                          (optional)
     * @param endFilterTime     allows you to specify the latest time of day (Eastern) for which to receive data
     *                          (optional)
     * @param dataDirection     the data direction (defaults to {@link DataDirection#NEWEST_TO_OLDEST}) (optional)
     * @param intervalType      the {@link IntervalType} (defaults to {@link IntervalType#SECONDS} (optional)
     * @param intervalsListener the {@link MultiMessageListener} for the requested {@link Interval}s
     *
     * @throws IOException thrown for {@link IOException}s
     */
    public void requestIntervals(String symbol, int intervalLength, LocalDateTime beginDateTime,
            LocalDateTime endDateTime, Integer maxDataPoints, LocalTime beginFilterTime, LocalTime endFilterTime,
            DataDirection dataDirection, IntervalType intervalType, MultiMessageListener<Interval> intervalsListener)
            throws IOException {
        checkNotNull(symbol);
        checkArgument(beginDateTime != null || endDateTime != null);
        checkNotNull(intervalsListener);

        String requestID = requestIDFeedHelper.getNewRequestID();
        StringBuilder requestBuilder = new StringBuilder();

        requestBuilder.append(HistoricalCommand.HISTORICAL_INTERVAL_DATETIMES.value()).append(",");
        requestBuilder.append(symbol).append(",");
        requestBuilder.append(intervalLength).append(",");

        if (beginDateTime != null) {
            requestBuilder.append(beginDateTime.format(DateTimeFormatters.DATE_SPACE_TIME));
        }
        requestBuilder.append(",");

        if (endDateTime != null) {
            requestBuilder.append(endDateTime.format(DateTimeFormatters.DATE_SPACE_TIME));
        }
        requestBuilder.append(",");

        if (maxDataPoints != null) {
            requestBuilder.append(maxDataPoints);
        }
        requestBuilder.append(",");

        if (beginFilterTime != null) {
            requestBuilder.append(beginFilterTime.format(DateTimeFormatters.TIME));
        }
        requestBuilder.append(",");

        if (endFilterTime != null) {
            requestBuilder.append(endFilterTime.format(DateTimeFormatters.TIME));
        }
        requestBuilder.append(",");

        if (dataDirection != null) {
            requestBuilder.append(dataDirection.value());
        }
        requestBuilder.append(",");

        requestBuilder.append(requestID).append(",");
        requestBuilder.append(DATAPOINTS_PER_SEND).append(",");

        if (intervalType != null) {
            requestBuilder.append(intervalType.value());
        }
        requestBuilder.append(",");

        requestBuilder.append(TimeLabelPlacement.BEGINNING);
        requestBuilder.append(LineEnding.CR_LF.getASCIIString());

        synchronized (messageReceivedLock) {
            intervalListenersOfRequestIDs.put(requestID, intervalsListener);
        }

        sendAndLogMessage(requestBuilder.toString());
    }

    /**
     * Calls {@link #requestIntervals(String, int, LocalDateTime, LocalDateTime, Integer, LocalTime, LocalTime,
     * DataDirection, IntervalType, MultiMessageListener)} and will accumulate all requested data into a {@link List}
     * which can be consumed later.
     *
     * @return a {@link List} of {@link Interval}s
     *
     * @throws IOException          thrown for {@link IOException}s
     * @throws ExecutionException   thrown for {@link ExecutionException}s
     * @throws InterruptedException thrown for {@link InterruptedException}s
     */
    public List<Interval> requestIntervals(String symbol, int intervalLength, LocalDateTime beginDateTime,
            LocalDateTime endDateTime, Integer maxDataPoints, LocalTime beginFilterTime, LocalTime endFilterTime,
            DataDirection dataDirection, IntervalType intervalType)
            throws IOException, ExecutionException, InterruptedException {
        MultiMessageAccumulator<Interval> asyncListener = new MultiMessageAccumulator<>();
        requestIntervals(symbol, intervalLength, beginDateTime, endDateTime, maxDataPoints, beginFilterTime,
                endFilterTime, dataDirection, intervalType, asyncListener);
        return asyncListener.getMessages();
    }

    /**
     * Retrieves up to <code>maxDays</code> days of End-Of-Day {@link DatedInterval} for the specified
     * <code>symbol</code>. This sends a {@link HistoricalCommand#HISTORICAL_DAILY_DATAPOINTS} request.
     *
     * @param symbol                 the symbol. Max length of 30 characters.
     * @param maxDays                the max days
     * @param dataDirection          the data direction (defaults to {@link DataDirection#NEWEST_TO_OLDEST}) (optional)
     * @param partialDatapoint       whether to include a {@link PartialDatapoint} based on the current day's trading up
     *                               to the time the request is received by the server (defaults to {@link
     *                               PartialDatapoint#INCLUDE}) (optional)
     * @param datedIntervalsListener the {@link MultiMessageListener} for the requested {@link DatedInterval}s
     *
     * @throws IOException thrown for {@link IOException}s
     */
    public void requestDayIntervals(String symbol, int maxDays, DataDirection dataDirection,
            PartialDatapoint partialDatapoint, MultiMessageListener<DatedInterval> datedIntervalsListener)
            throws IOException {
        checkNotNull(symbol);
        checkNotNull(datedIntervalsListener);

        String requestID = requestIDFeedHelper.getNewRequestID();
        StringBuilder requestBuilder = new StringBuilder();

        requestBuilder.append(HistoricalCommand.HISTORICAL_DAILY_DATAPOINTS.value()).append(",");
        requestBuilder.append(symbol).append(",");
        requestBuilder.append(maxDays).append(",");

        if (dataDirection != null) {
            requestBuilder.append(dataDirection.value());
        }
        requestBuilder.append(",");

        requestBuilder.append(requestID).append(",");
        requestBuilder.append(DATAPOINTS_PER_SEND).append(",");

        if (partialDatapoint != null) {
            requestBuilder.append(partialDatapoint.value());
        }

        requestBuilder.append(LineEnding.CR_LF.getASCIIString());

        synchronized (messageReceivedLock) {
            datedIntervalListenersOfRequestIDs.put(requestID, datedIntervalsListener);
        }

        sendAndLogMessage(requestBuilder.toString());
    }

    /**
     * Calls {@link #requestDayIntervals(String, int, DataDirection, PartialDatapoint, MultiMessageListener)} and will
     * accumulate all requested data into a {@link List} which can be consumed later.
     *
     * @return a {@link List} of {@link DatedInterval}s
     *
     * @throws IOException          thrown for {@link IOException}s
     * @throws ExecutionException   thrown for {@link ExecutionException}s
     * @throws InterruptedException thrown for {@link InterruptedException}s
     */
    public List<DatedInterval> requestDayIntervals(String symbol, int maxDays, DataDirection dataDirection,
            PartialDatapoint partialDatapoint) throws IOException, ExecutionException, InterruptedException {
        MultiMessageAccumulator<DatedInterval> asyncListener = new MultiMessageAccumulator<>();
        requestDayIntervals(symbol, maxDays, dataDirection, partialDatapoint, asyncListener);
        return asyncListener.getMessages();
    }

    /**
     * Retrieves Daily {@link DatedInterval}s between <code>beginDate</code> and <code>endDate</code> for the specified
     * <code>symbol</code>. This sends a {@link HistoricalCommand#HISTORICAL_DAILY_DATES} request.
     *
     * @param symbol                 the symbol. Max length of 30 characters.
     * @param beginDate              earliest date (Eastern) to receive data for.
     * @param endDate                most recent date (Eastern) to receive data for
     * @param maxDataPoints          the maximum number of datapoints to be retrieved (optional)
     * @param dataDirection          the data direction (defaults to {@link DataDirection#NEWEST_TO_OLDEST}) (optional)
     * @param partialDatapoint       whether to include a {@link PartialDatapoint} based on the current day's trading up
     *                               to the time the request is received by the server (defaults to {@link
     *                               PartialDatapoint#INCLUDE}) (optional)
     * @param datedIntervalsListener the {@link MultiMessageListener} for the requested {@link DatedInterval}s
     *
     * @throws IOException thrown for {@link IOException}s
     */
    public void requestDayIntervals(String symbol, LocalDate beginDate, LocalDate endDate, Integer maxDataPoints,
            DataDirection dataDirection, PartialDatapoint partialDatapoint,
            MultiMessageListener<DatedInterval> datedIntervalsListener) throws IOException {
        checkNotNull(symbol);
        checkArgument(beginDate != null || endDate != null);
        checkNotNull(datedIntervalsListener);

        String requestID = requestIDFeedHelper.getNewRequestID();
        StringBuilder requestBuilder = new StringBuilder();

        requestBuilder.append(HistoricalCommand.HISTORICAL_DAILY_DATES.value()).append(",");
        requestBuilder.append(symbol).append(",");

        if (beginDate != null) {
            requestBuilder.append(beginDate.format(DateTimeFormatters.DATE));
        }
        requestBuilder.append(",");

        if (endDate != null) {
            requestBuilder.append(endDate.format(DateTimeFormatters.DATE));
        }
        requestBuilder.append(",");

        if (maxDataPoints != null) {
            requestBuilder.append(maxDataPoints);
        }
        requestBuilder.append(",");

        if (dataDirection != null) {
            requestBuilder.append(dataDirection.value());
        }
        requestBuilder.append(",");

        requestBuilder.append(requestID).append(",");
        requestBuilder.append(DATAPOINTS_PER_SEND).append(",");

        if (partialDatapoint != null) {
            requestBuilder.append(partialDatapoint.value());
        }

        requestBuilder.append(LineEnding.CR_LF.getASCIIString());

        synchronized (messageReceivedLock) {
            datedIntervalListenersOfRequestIDs.put(requestID, datedIntervalsListener);
        }

        sendAndLogMessage(requestBuilder.toString());
    }

    /**
     * Calls {@link #requestDayIntervals(String, LocalDate, LocalDate, Integer, DataDirection, PartialDatapoint,
     * MultiMessageListener)} and will accumulate all requested data into a {@link List} which can be consumed later.
     *
     * @return a {@link List} of {@link DatedInterval}s
     *
     * @throws IOException          thrown for {@link IOException}s
     * @throws ExecutionException   thrown for {@link ExecutionException}s
     * @throws InterruptedException thrown for {@link InterruptedException}s
     */
    public List<DatedInterval> requestDayIntervals(String symbol, LocalDate beginDate, LocalDate endDate,
            Integer maxDataPoints, DataDirection dataDirection, PartialDatapoint partialDatapoint)
            throws IOException, ExecutionException, InterruptedException {
        MultiMessageAccumulator<DatedInterval> asyncListener = new MultiMessageAccumulator<>();
        requestDayIntervals(symbol, beginDate, endDate, maxDataPoints, dataDirection, partialDatapoint, asyncListener);
        return asyncListener.getMessages();
    }

    /**
     * Retrieves up to <code>maxWeeks</code> if composite weekly {@link DatedInterval} for the specified
     * <code>symbol</code>. This sends a {@link HistoricalCommand#HISTORICAL_WEEKLY_DATAPOINTS} request.
     *
     * @param symbol                 the symbol. Max length of 30 characters.
     * @param maxWeeks               the max weeks
     * @param dataDirection          the data direction (defaults to {@link DataDirection#NEWEST_TO_OLDEST}) (optional)
     * @param partialDatapoint       whether to include a {@link PartialDatapoint} based on the current day's trading up
     *                               to the time the request is received by the server (defaults to {@link
     *                               PartialDatapoint#INCLUDE}) (optional)
     * @param datedIntervalsListener the {@link MultiMessageListener} for the requested {@link DatedInterval}s
     *
     * @throws IOException thrown for {@link IOException}s
     */
    public void requestWeekIntervals(String symbol, int maxWeeks, DataDirection dataDirection,
            PartialDatapoint partialDatapoint, MultiMessageListener<DatedInterval> datedIntervalsListener)
            throws IOException {
        checkNotNull(symbol);
        checkNotNull(datedIntervalsListener);

        String requestID = requestIDFeedHelper.getNewRequestID();
        StringBuilder requestBuilder = new StringBuilder();

        requestBuilder.append(HistoricalCommand.HISTORICAL_WEEKLY_DATAPOINTS.value()).append(",");
        requestBuilder.append(symbol).append(",");
        requestBuilder.append(maxWeeks).append(",");

        if (dataDirection != null) {
            requestBuilder.append(dataDirection.value());
        }
        requestBuilder.append(",");

        requestBuilder.append(requestID).append(",");
        requestBuilder.append(DATAPOINTS_PER_SEND).append(",");

        if (partialDatapoint != null) {
            requestBuilder.append(partialDatapoint.value());
        }

        requestBuilder.append(LineEnding.CR_LF.getASCIIString());

        synchronized (messageReceivedLock) {
            datedIntervalListenersOfRequestIDs.put(requestID, datedIntervalsListener);
        }

        sendAndLogMessage(requestBuilder.toString());
    }

    /**
     * Calls {@link #requestWeekIntervals(String, int, DataDirection, PartialDatapoint, MultiMessageListener)} and will
     * accumulate all requested data into a {@link List} which can be consumed later.
     *
     * @return a {@link List} of {@link DatedInterval}s
     *
     * @throws IOException          thrown for {@link IOException}s
     * @throws ExecutionException   thrown for {@link ExecutionException}s
     * @throws InterruptedException thrown for {@link InterruptedException}s
     */
    public List<DatedInterval> requestWeekIntervals(String symbol, int maxWeeks, DataDirection dataDirection,
            PartialDatapoint partialDatapoint) throws IOException, ExecutionException, InterruptedException {
        MultiMessageAccumulator<DatedInterval> asyncListener = new MultiMessageAccumulator<>();
        requestWeekIntervals(symbol, maxWeeks, dataDirection, partialDatapoint, asyncListener);
        return asyncListener.getMessages();
    }

    /**
     * Retrieves up to <code>maxMonths</code> if composite monthly {@link DatedInterval} for the specified
     * <code>symbol</code>. This sends a {@link HistoricalCommand#HISTORICAL_MONTHLY_DATAPOINTS} request.
     *
     * @param symbol                 the symbol. Max length of 30 characters.
     * @param maxMonths              the max months
     * @param dataDirection          the data direction (defaults to {@link DataDirection#NEWEST_TO_OLDEST}) (optional)
     * @param partialDatapoint       whether to include a {@link PartialDatapoint} based on the current day's trading up
     *                               to the time the request is received by the server (defaults to {@link
     *                               PartialDatapoint#INCLUDE}) (optional)
     * @param datedIntervalsListener the {@link MultiMessageListener} for the requested {@link DatedInterval}s
     *
     * @throws IOException thrown for {@link IOException}s
     */
    public void requestMonthIntervals(String symbol, int maxMonths, DataDirection dataDirection,
            PartialDatapoint partialDatapoint, MultiMessageListener<DatedInterval> datedIntervalsListener)
            throws IOException {
        checkNotNull(symbol);
        checkNotNull(datedIntervalsListener);

        String requestID = requestIDFeedHelper.getNewRequestID();
        StringBuilder requestBuilder = new StringBuilder();

        requestBuilder.append(HistoricalCommand.HISTORICAL_MONTHLY_DATAPOINTS.value()).append(",");
        requestBuilder.append(symbol).append(",");
        requestBuilder.append(maxMonths).append(",");

        if (dataDirection != null) {
            requestBuilder.append(dataDirection.value());
        }
        requestBuilder.append(",");

        requestBuilder.append(requestID).append(",");
        requestBuilder.append(DATAPOINTS_PER_SEND).append(",");

        if (partialDatapoint != null) {
            requestBuilder.append(partialDatapoint.value());
        }

        requestBuilder.append(LineEnding.CR_LF.getASCIIString());

        synchronized (messageReceivedLock) {
            datedIntervalListenersOfRequestIDs.put(requestID, datedIntervalsListener);
        }

        sendAndLogMessage(requestBuilder.toString());
    }

    /**
     * Calls {@link #requestMonthIntervals(String, int, DataDirection, PartialDatapoint, MultiMessageListener)} and will
     * accumulate all requested data into a {@link List} which can be consumed later.
     *
     * @return a {@link List} of {@link DatedInterval}s
     *
     * @throws IOException          thrown for {@link IOException}s
     * @throws ExecutionException   thrown for {@link ExecutionException}s
     * @throws InterruptedException thrown for {@link InterruptedException}s
     */
    public List<DatedInterval> requestMonthIntervals(String symbol, int maxMonths, DataDirection dataDirection,
            PartialDatapoint partialDatapoint) throws IOException, ExecutionException, InterruptedException {
        MultiMessageAccumulator<DatedInterval> asyncListener = new MultiMessageAccumulator<>();
        requestMonthIntervals(symbol, maxMonths, dataDirection, partialDatapoint, asyncListener);
        return asyncListener.getMessages();
    }

    //
    // END Feed commands
    //
}
