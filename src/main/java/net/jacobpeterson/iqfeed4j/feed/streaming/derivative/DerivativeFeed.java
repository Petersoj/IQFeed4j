package net.jacobpeterson.iqfeed4j.feed.streaming.derivative;

import net.jacobpeterson.iqfeed4j.feed.streaming.AbstractServerConnectionFeed;
import net.jacobpeterson.iqfeed4j.model.streaming.derivative.Interval;
import net.jacobpeterson.iqfeed4j.util.csv.mapper.IndexCSVMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static net.jacobpeterson.iqfeed4j.util.csv.mapper.CSVMapper.DateTimeConverters.DASHED_DATE_SPACE_TIME;
import static net.jacobpeterson.iqfeed4j.util.csv.mapper.CSVMapper.PrimitiveConvertors.*;

/**
 * {@link DerivativeFeed} is an {@link AbstractServerConnectionFeed} for derivative tick data (aka interval/bar data).
 */
public class DerivativeFeed extends AbstractServerConnectionFeed {

    private static final Logger LOGGER = LoggerFactory.getLogger(DerivativeFeed.class);
    private static final String FEED_NAME_SUFFIX = " Derivative Feed";
    private static final IndexCSVMapper<Interval> INTERVAL_CSV_MAPPER;

    static {
        // Add mappings with CSV indices analogous to line of execution

        INTERVAL_CSV_MAPPER = new IndexCSVMapper<>(Interval::new);
        INTERVAL_CSV_MAPPER.addMapping(Interval::setUpdateType, Interval.UpdateType::valueOf);
        INTERVAL_CSV_MAPPER.addMapping(Interval::setSymbol, STRING);
        INTERVAL_CSV_MAPPER.addMapping(Interval::setTimestamp, DASHED_DATE_SPACE_TIME);
        INTERVAL_CSV_MAPPER.addMapping(Interval::setOpen, DOUBLE);
        INTERVAL_CSV_MAPPER.addMapping(Interval::setHigh, DOUBLE);
        INTERVAL_CSV_MAPPER.addMapping(Interval::setLow, DOUBLE);
        INTERVAL_CSV_MAPPER.addMapping(Interval::setLast, DOUBLE);
        INTERVAL_CSV_MAPPER.addMapping(Interval::setCumulativeVolume, INT);
        INTERVAL_CSV_MAPPER.addMapping(Interval::setIntervalVolume, INT);
        INTERVAL_CSV_MAPPER.addMapping(Interval::setNumberOfTrades, INT);
    }

    /**
     * Instantiates a new {@link DerivativeFeed}.
     *
     * @param derivativeFeedName the {@link DerivativeFeed} name
     * @param hostname           the hostname
     * @param port               the port
     */
    public DerivativeFeed(String derivativeFeedName, String hostname, int port) {
        super(derivativeFeedName + FEED_NAME_SUFFIX, hostname, port, COMMA_DELIMITED_SPLITTER);
    }
}
