package net.jacobpeterson.iqfeed4j.feed.lookup;

import net.jacobpeterson.iqfeed4j.feed.AbstractFeed;
import net.jacobpeterson.iqfeed4j.model.feedenums.FeedMessageType;
import net.jacobpeterson.iqfeed4j.model.lookup.historical.DatedInterval;
import net.jacobpeterson.iqfeed4j.model.lookup.historical.Interval;
import net.jacobpeterson.iqfeed4j.model.lookup.historical.Tick;
import net.jacobpeterson.iqfeed4j.util.csv.CSVMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;

import static net.jacobpeterson.iqfeed4j.util.csv.CSVMapper.DateTimeConverters.DASHED_DATE;
import static net.jacobpeterson.iqfeed4j.util.csv.CSVMapper.DateTimeConverters.DASHED_DATE_SPACE_TIME;
import static net.jacobpeterson.iqfeed4j.util.csv.CSVMapper.DateTimeConverters.DASHED_DATE_SPACE_TIME_FRACTIONAL;
import static net.jacobpeterson.iqfeed4j.util.csv.CSVMapper.PrimitiveConvertors.DOUBLE;
import static net.jacobpeterson.iqfeed4j.util.csv.CSVMapper.PrimitiveConvertors.INT;
import static net.jacobpeterson.iqfeed4j.util.csv.CSVMapper.PrimitiveConvertors.LONG;
import static net.jacobpeterson.iqfeed4j.util.csv.CSVMapper.PrimitiveConvertors.SHORT;
import static net.jacobpeterson.iqfeed4j.util.csv.CSVMapper.PrimitiveConvertors.STRING;
import static net.jacobpeterson.iqfeed4j.util.csv.CSVUtil.valueEquals;

/**
 * {@link LookupFeed} represents the Lookup {@link AbstractFeed}. Methods in this class are not synchronized.
 */
public class LookupFeed extends AbstractFeed {

    private static final Logger LOGGER = LoggerFactory.getLogger(LookupFeed.class);
    private static final String FEED_NAME_SUFFIX = " Lookup Feed";
    private static final CSVMapper<Tick> TICK_CSV_MAPPER;
    private static final CSVMapper<Interval> INTERVAL_CSV_MAPPER;
    private static final CSVMapper<DatedInterval> DATED_INTERVAL_CSV_MAPPER;

    static {
        // Add mappings with CSV indices analogous to line of execution

        TICK_CSV_MAPPER = new CSVMapper<>(Tick::new);
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

        INTERVAL_CSV_MAPPER = new CSVMapper<>(Interval::new);
        INTERVAL_CSV_MAPPER.addMapping(Interval::setTimestamp, DASHED_DATE_SPACE_TIME);
        INTERVAL_CSV_MAPPER.addMapping(Interval::setHigh, DOUBLE);
        INTERVAL_CSV_MAPPER.addMapping(Interval::setLow, DOUBLE);
        INTERVAL_CSV_MAPPER.addMapping(Interval::setOpen, DOUBLE);
        INTERVAL_CSV_MAPPER.addMapping(Interval::setClose, DOUBLE);
        INTERVAL_CSV_MAPPER.addMapping(Interval::setTotalVolume, LONG);
        INTERVAL_CSV_MAPPER.addMapping(Interval::setPeriodVolume, INT);
        INTERVAL_CSV_MAPPER.addMapping(Interval::setNumberOfTrades, INT);

        DATED_INTERVAL_CSV_MAPPER = new CSVMapper<>(DatedInterval::new);
        DATED_INTERVAL_CSV_MAPPER.addMapping(DatedInterval::setDate, DASHED_DATE);
        DATED_INTERVAL_CSV_MAPPER.addMapping(DatedInterval::setHigh, DOUBLE);
        DATED_INTERVAL_CSV_MAPPER.addMapping(DatedInterval::setLow, DOUBLE);
        DATED_INTERVAL_CSV_MAPPER.addMapping(DatedInterval::setOpen, DOUBLE);
        DATED_INTERVAL_CSV_MAPPER.addMapping(DatedInterval::setClose, DOUBLE);
        DATED_INTERVAL_CSV_MAPPER.addMapping(DatedInterval::setPeriodVolume, INT);
        DATED_INTERVAL_CSV_MAPPER.addMapping(DatedInterval::setOpenInterest, INT);
    }

    protected final Object messageReceivedLock;
    private final HashSet<Integer> requestIDs;

    /**
     * Instantiates a new {@link LookupFeed}.
     *
     * @param lookupFeedName the lookup feed name
     * @param hostname       the host name
     * @param port           the port
     */
    public LookupFeed(String lookupFeedName, String hostname, int port) {
        super(lookupFeedName + FEED_NAME_SUFFIX, hostname, port);

        messageReceivedLock = new Object();
        requestIDs = new HashSet<>();
    }

    @Override
    protected void onProtocolVersionValidated() {}

    @Override
    protected void onMessageReceived(String[] csv) {
        if (isErrorMessage(csv)) {
            LOGGER.error("Received error message! {}", (Object) csv);
            return;
        }
    }

    @Override
    protected void onAsyncException(String message, Exception exception) {
        LOGGER.error(message, exception);
    }

    /**
     * Checks for error message format.
     * <br>
     * e.g. <code>[Request ID], E, [Error Text]</code> or <code>E, [Error Text]</code>
     * <br>
     * If the 'Request ID' is the char literal 'E', then this will always return true unfortunately (this is a flaw with
     * the IQFeed API)
     *
     * @param csv the CSV
     *
     * @return true if the 'csv' represents an error message
     */
    protected boolean isErrorMessage(String[] csv) {
        return valueEquals(csv, 0, FeedMessageType.ERROR.value()) ||
                valueEquals(csv, 1, FeedMessageType.ERROR.value());
    }
}
