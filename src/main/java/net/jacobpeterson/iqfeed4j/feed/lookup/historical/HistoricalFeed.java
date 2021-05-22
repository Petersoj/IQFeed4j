package net.jacobpeterson.iqfeed4j.feed.lookup.historical;

import com.google.common.base.Preconditions;
import net.jacobpeterson.iqfeed4j.feed.MultiMessageListener;
import net.jacobpeterson.iqfeed4j.feed.lookup.AbstractLookupFeed;
import net.jacobpeterson.iqfeed4j.model.feedenums.FeedMessageType;
import net.jacobpeterson.iqfeed4j.model.feedenums.lookup.historical.IntervalType;
import net.jacobpeterson.iqfeed4j.model.feedenums.lookup.historical.PartialDatapoint;
import net.jacobpeterson.iqfeed4j.model.feedenums.lookup.historical.TimeLabelPlacement;
import net.jacobpeterson.iqfeed4j.model.feedenums.misc.DataDirection;
import net.jacobpeterson.iqfeed4j.model.lookup.historical.DatedInterval;
import net.jacobpeterson.iqfeed4j.model.lookup.historical.Interval;
import net.jacobpeterson.iqfeed4j.model.lookup.historical.Tick;
import net.jacobpeterson.iqfeed4j.util.csv.mapper.CSVMapper.DateTimeFormatters;
import net.jacobpeterson.iqfeed4j.util.csv.mapper.IndexCSVMapper;
import net.jacobpeterson.iqfeed4j.util.string.LineEnding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;

import static net.jacobpeterson.iqfeed4j.util.csv.CSVUtil.valueEquals;
import static net.jacobpeterson.iqfeed4j.util.csv.CSVUtil.valuePresent;
import static net.jacobpeterson.iqfeed4j.util.csv.mapper.CSVMapper.DateTimeConverters.*;
import static net.jacobpeterson.iqfeed4j.util.csv.mapper.CSVMapper.PrimitiveConvertors.*;

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
        TICK_CSV_MAPPER.addMapping(Tick::setLastSize, INT);
        TICK_CSV_MAPPER.addMapping(Tick::setTotalVolume, LONG);
        TICK_CSV_MAPPER.addMapping(Tick::setBid, DOUBLE);
        TICK_CSV_MAPPER.addMapping(Tick::setAsk, DOUBLE);
        TICK_CSV_MAPPER.addMapping(Tick::setTickID, INT);
        TICK_CSV_MAPPER.addMapping(Tick::setBasisForLast, Tick.BasisForLast::fromValue);
        TICK_CSV_MAPPER.addMapping(Tick::setTradeMarketCenter, SHORT);
        TICK_CSV_MAPPER.addMapping(Tick::setTradeConditions, STRING);
        TICK_CSV_MAPPER.addMapping(Tick::setTradeAggressor, Tick.TradeAggressor::fromValue);
        TICK_CSV_MAPPER.addMapping(Tick::setDayCode, INT);

        INTERVAL_CSV_MAPPER = new IndexCSVMapper<>(Interval::new);
        INTERVAL_CSV_MAPPER.addMapping(Interval::setTimestamp, DASHED_DATE_SPACE_TIME);
        INTERVAL_CSV_MAPPER.addMapping(Interval::setHigh, DOUBLE);
        INTERVAL_CSV_MAPPER.addMapping(Interval::setLow, DOUBLE);
        INTERVAL_CSV_MAPPER.addMapping(Interval::setOpen, DOUBLE);
        INTERVAL_CSV_MAPPER.addMapping(Interval::setClose, DOUBLE);
        INTERVAL_CSV_MAPPER.addMapping(Interval::setTotalVolume, LONG);
        INTERVAL_CSV_MAPPER.addMapping(Interval::setPeriodVolume, INT);
        INTERVAL_CSV_MAPPER.addMapping(Interval::setNumberOfTrades, INT);

        DATED_INTERVAL_CSV_MAPPER = new IndexCSVMapper<>(DatedInterval::new);
        DATED_INTERVAL_CSV_MAPPER.addMapping(DatedInterval::setDate, DASHED_DATE);
        DATED_INTERVAL_CSV_MAPPER.addMapping(DatedInterval::setHigh, DOUBLE);
        DATED_INTERVAL_CSV_MAPPER.addMapping(DatedInterval::setLow, DOUBLE);
        DATED_INTERVAL_CSV_MAPPER.addMapping(DatedInterval::setOpen, DOUBLE);
        DATED_INTERVAL_CSV_MAPPER.addMapping(DatedInterval::setClose, DOUBLE);
        DATED_INTERVAL_CSV_MAPPER.addMapping(DatedInterval::setPeriodVolume, INT);
        DATED_INTERVAL_CSV_MAPPER.addMapping(DatedInterval::setOpenInterest, INT);
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
        super(historicalFeedName + FEED_NAME_SUFFIX, hostname, port, COMMA_DELIMITED_SPLITTER);

        messageReceivedLock = new Object();
        tickListenersOfRequestIDs = new HashMap<>();
        intervalListenersOfRequestIDs = new HashMap<>();
        datedIntervalListenersOfRequestIDs = new HashMap<>();
    }

    @Override
    protected void onMessageReceived(String[] csv) {
        if (valueEquals(csv, 0, FeedMessageType.ERROR.value())) {
            LOGGER.error("Received error message! {}", (Object) csv);
            return;
        }

        // All messages sent on this feed must have a Request ID first
        if (!valuePresent(csv, 0)) {
            LOGGER.error("Received unknown message format: {}", (Object) csv);
            return;
        }

        String requestID = csv[0];

        synchronized (messageReceivedLock) {
            if (handleMultiMessage(csv, requestID, tickListenersOfRequestIDs, TICK_CSV_MAPPER)) {
                return;
            }

            if (handleMultiMessage(csv, requestID, intervalListenersOfRequestIDs, INTERVAL_CSV_MAPPER)) {
                return;
            }

            if (handleMultiMessage(csv, requestID, datedIntervalListenersOfRequestIDs, DATED_INTERVAL_CSV_MAPPER)) {
                return;
            }
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

    //
    // START Feed commands
    //

    /**
     * Retrieves up to 'maxDataPoints' number of {@link Tick}s for the specified 'symbol'. This sends an HTX request.
     * This method is thread-safe.
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
        Preconditions.checkNotNull(symbol);
        Preconditions.checkNotNull(ticksListener);

        String requestID = getNewRequestID();
        StringBuilder requestBuilder = new StringBuilder();

        requestBuilder.append("HTX").append(",");
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
        String requestString = requestBuilder.toString();
        LOGGER.debug("Sending request: {}", requestString);
        sendMessage(requestString);
    }

    /**
     * Retrieves {@link Tick}s for the previous 'maxDays' days for the specified 'symbol'. This sends an HTD request.
     * This method is thread-safe.
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
        Preconditions.checkNotNull(symbol);
        Preconditions.checkNotNull(ticksListener);

        String requestID = getNewRequestID();
        StringBuilder requestBuilder = new StringBuilder();

        requestBuilder.append("HTD").append(",");
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
        String requestString = requestBuilder.toString();
        LOGGER.debug("Sending request: {}", requestString);
        sendMessage(requestString);
    }

    /**
     * Retrieves {@link Tick} data between 'beginDateTime' and 'endDateTime' for the specified 'symbol. This sends an
     * HTT request. This method is thread-safe.
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
        Preconditions.checkNotNull(symbol);
        Preconditions.checkArgument(beginDateTime != null || endDateTime != null);
        Preconditions.checkNotNull(ticksListener);

        String requestID = getNewRequestID();
        StringBuilder requestBuilder = new StringBuilder();

        requestBuilder.append("HTT").append(",");
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
        String requestString = requestBuilder.toString();
        LOGGER.debug("Sending request: {}", requestString);
        sendMessage(requestString);
    }

    /**
     * Retrieves up to 'maxDataPoints' number of {@link Interval}s for the specified 'symbol'. This sends an HIX
     * request. This method is thread-safe.
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
        Preconditions.checkNotNull(symbol);
        Preconditions.checkNotNull(maxDataPoints);
        Preconditions.checkNotNull(intervalsListener);

        String requestID = getNewRequestID();
        StringBuilder requestBuilder = new StringBuilder();

        requestBuilder.append("HIX").append(",");
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
        String requestString = requestBuilder.toString();
        LOGGER.debug("Sending request: {}", requestString);
        sendMessage(requestString);
    }

    /**
     * Retrieves {@link Interval}s for 'maxDays' days for the specified 'symbol'. This sends an HID request. This method
     * is thread-safe.
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
        Preconditions.checkNotNull(symbol);
        Preconditions.checkNotNull(intervalsListener);

        String requestID = getNewRequestID();
        StringBuilder requestBuilder = new StringBuilder();

        requestBuilder.append("HID").append(",");
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
        String requestString = requestBuilder.toString();
        LOGGER.debug("Sending request: {}", requestString);
        sendMessage(requestString);
    }

    /**
     * Retrieves {@link Interval} data between 'beginDateTime' and 'endDateTime' for the specified 'symbol. This sends
     * an HIT request. This method is thread-safe.
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
        Preconditions.checkNotNull(symbol);
        Preconditions.checkArgument(beginDateTime != null || endDateTime != null);
        Preconditions.checkNotNull(intervalsListener);

        String requestID = getNewRequestID();
        StringBuilder requestBuilder = new StringBuilder();

        requestBuilder.append("HIT").append(",");
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
        String requestString = requestBuilder.toString();
        LOGGER.debug("Sending request: {}", requestString);
        sendMessage(requestString);
    }

    /**
     * Retrieves up to 'maxDays' days of End-Of-Day {@link DatedInterval} for the specified 'symbol'. This sends an HDX
     * request. This method is thread-safe.
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
        Preconditions.checkNotNull(symbol);
        Preconditions.checkNotNull(datedIntervalsListener);

        String requestID = getNewRequestID();
        StringBuilder requestBuilder = new StringBuilder();

        requestBuilder.append("HDX").append(",");
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
        String requestString = requestBuilder.toString();
        LOGGER.debug("Sending request: {}", requestString);
        sendMessage(requestString);
    }

    /**
     * Retrieves Daily {@link DatedInterval}s between 'beginDate' and 'endDate' for the specified 'symbol'. This sends
     * an HDT request. This method is thread-safe.
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
        Preconditions.checkNotNull(symbol);
        Preconditions.checkArgument(beginDate != null || endDate != null);
        Preconditions.checkNotNull(datedIntervalsListener);

        String requestID = getNewRequestID();
        StringBuilder requestBuilder = new StringBuilder();

        requestBuilder.append("HDT").append(",");
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
        String requestString = requestBuilder.toString();
        LOGGER.debug("Sending request: {}", requestString);
        sendMessage(requestString);
    }

    /**
     * Retrieves up to 'maxWeeks' if composite weekly {@link DatedInterval} for the specified 'symbol'. This sends an
     * HWX request. This method is thread-safe.
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
        Preconditions.checkNotNull(symbol);
        Preconditions.checkNotNull(datedIntervalsListener);

        String requestID = getNewRequestID();
        StringBuilder requestBuilder = new StringBuilder();

        requestBuilder.append("HWX").append(",");
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
        String requestString = requestBuilder.toString();
        LOGGER.debug("Sending request: {}", requestString);
        sendMessage(requestString);
    }

    /**
     * Retrieves up to 'maxMonths' if composite monthly {@link DatedInterval} for the specified 'symbol'. This sends an
     * HMX request. This method is thread-safe.
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
        Preconditions.checkNotNull(symbol);
        Preconditions.checkNotNull(datedIntervalsListener);

        String requestID = getNewRequestID();
        StringBuilder requestBuilder = new StringBuilder();

        requestBuilder.append("HMX").append(",");
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
        String requestString = requestBuilder.toString();
        LOGGER.debug("Sending request: {}", requestString);
        sendMessage(requestString);
    }

    //
    // END Feed commands
    //
}
