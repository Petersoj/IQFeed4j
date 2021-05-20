package net.jacobpeterson.iqfeed4j.feed.lookup.historical;

import net.jacobpeterson.iqfeed4j.feed.MultiMessageListener;
import net.jacobpeterson.iqfeed4j.feed.lookup.AbstractLookupFeed;
import net.jacobpeterson.iqfeed4j.model.lookup.historical.DatedInterval;
import net.jacobpeterson.iqfeed4j.model.lookup.historical.Interval;
import net.jacobpeterson.iqfeed4j.model.lookup.historical.Tick;
import net.jacobpeterson.iqfeed4j.util.csv.CSVMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

import static net.jacobpeterson.iqfeed4j.util.csv.CSVMapper.DateTimeConverters.DASHED_DATE;
import static net.jacobpeterson.iqfeed4j.util.csv.CSVMapper.DateTimeConverters.DASHED_DATE_SPACE_TIME;
import static net.jacobpeterson.iqfeed4j.util.csv.CSVMapper.DateTimeConverters.DASHED_DATE_SPACE_TIME_FRACTIONAL;
import static net.jacobpeterson.iqfeed4j.util.csv.CSVMapper.PrimitiveConvertors.DOUBLE;
import static net.jacobpeterson.iqfeed4j.util.csv.CSVMapper.PrimitiveConvertors.INT;
import static net.jacobpeterson.iqfeed4j.util.csv.CSVMapper.PrimitiveConvertors.LONG;
import static net.jacobpeterson.iqfeed4j.util.csv.CSVMapper.PrimitiveConvertors.SHORT;
import static net.jacobpeterson.iqfeed4j.util.csv.CSVMapper.PrimitiveConvertors.STRING;

/**
 * {@link HistoricalFeed} is an {@link AbstractLookupFeed} for historical data.
 */
public class HistoricalFeed extends AbstractLookupFeed {

    private static final Logger LOGGER = LoggerFactory.getLogger(HistoricalFeed.class);
    private static final String FEED_NAME_SUFFIX = " Historical";

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

    private final HashMap<String, MultiMessageListener<Tick>> tickListenersOfRequestIDs;
    private final HashMap<String, MultiMessageListener<Interval>> intervalListenersOfRequestIDs;
    private final HashMap<String, MultiMessageListener<DatedInterval>> datedIntervalListenersOfRequestIDs;

    /**
     * Instantiates a new {@link HistoricalFeed}.
     *
     * @param historicalFeedName the historical feed name
     * @param hostname           the hostname
     * @param port               the port
     */
    public HistoricalFeed(String historicalFeedName, String hostname, int port) {
        super(historicalFeedName + FEED_NAME_SUFFIX, hostname, port);

        tickListenersOfRequestIDs = new HashMap<>();
        intervalListenersOfRequestIDs = new HashMap<>();
        datedIntervalListenersOfRequestIDs = new HashMap<>();
    }

    @Override
    protected void onMessageReceived(String[] csv) {

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
}
