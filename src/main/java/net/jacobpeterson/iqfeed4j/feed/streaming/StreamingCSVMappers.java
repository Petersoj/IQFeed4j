package net.jacobpeterson.iqfeed4j.feed.streaming;

import net.jacobpeterson.iqfeed4j.model.feed.streaming.common.FeedStatistics;
import net.jacobpeterson.iqfeed4j.util.csv.mapper.CSVMapper;
import net.jacobpeterson.iqfeed4j.util.csv.mapper.IndexCSVMapper;

import static net.jacobpeterson.iqfeed4j.util.csv.mapper.CSVMapper.DateTimeConverters.MONTH3_DAY_TIME_AM_PM;
import static net.jacobpeterson.iqfeed4j.util.csv.mapper.CSVMapper.PrimitiveConvertors.DOUBLE;
import static net.jacobpeterson.iqfeed4j.util.csv.mapper.CSVMapper.PrimitiveConvertors.INTEGER;
import static net.jacobpeterson.iqfeed4j.util.csv.mapper.CSVMapper.PrimitiveConvertors.STRING;

/**
 * {@link StreamingCSVMappers} contains {@link CSVMapper}s for streaming feeds.
 */
public final class StreamingCSVMappers {

    /**
     * A {@link IndexCSVMapper} for {@link FeedStatistics}.
     */
    public static final IndexCSVMapper<FeedStatistics> FEED_STATISTICS_CSV_MAPPER;

    static {
        FEED_STATISTICS_CSV_MAPPER = new IndexCSVMapper<>(FeedStatistics::new);
        FEED_STATISTICS_CSV_MAPPER.addMapping(FeedStatistics::setServerIP, STRING);
        FEED_STATISTICS_CSV_MAPPER.addMapping(FeedStatistics::setServerPort, INTEGER);
        FEED_STATISTICS_CSV_MAPPER.addMapping(FeedStatistics::setMaxSymbols, INTEGER);
        FEED_STATISTICS_CSV_MAPPER.addMapping(FeedStatistics::setNumberOfSymbols, INTEGER);
        FEED_STATISTICS_CSV_MAPPER.addMapping(FeedStatistics::setClientsConnected, INTEGER);
        FEED_STATISTICS_CSV_MAPPER.addMapping(FeedStatistics::setSecondsSinceLastUpdate, INTEGER);
        FEED_STATISTICS_CSV_MAPPER.addMapping(FeedStatistics::setReconnections, INTEGER);
        FEED_STATISTICS_CSV_MAPPER.addMapping(FeedStatistics::setAttemptedReconnections, INTEGER);
        FEED_STATISTICS_CSV_MAPPER.addMapping(FeedStatistics::setStartTime, MONTH3_DAY_TIME_AM_PM);
        FEED_STATISTICS_CSV_MAPPER.addMapping(FeedStatistics::setMarketTime, MONTH3_DAY_TIME_AM_PM);
        FEED_STATISTICS_CSV_MAPPER.addMapping(FeedStatistics::setStatus, FeedStatistics.Status::fromValue);
        FEED_STATISTICS_CSV_MAPPER.addMapping(FeedStatistics::setIQFeedVersion, STRING);
        FEED_STATISTICS_CSV_MAPPER.addMapping(FeedStatistics::setLoginID, STRING);
        FEED_STATISTICS_CSV_MAPPER.addMapping(FeedStatistics::setTotalKiloBytesReceived, DOUBLE);
        FEED_STATISTICS_CSV_MAPPER.addMapping(FeedStatistics::setKiloBytesPerSecReceived, DOUBLE);
        FEED_STATISTICS_CSV_MAPPER.addMapping(FeedStatistics::setAvgKiloBytesPerSecRecv, DOUBLE);
        FEED_STATISTICS_CSV_MAPPER.addMapping(FeedStatistics::setTotalKiloBytesSent, DOUBLE);
        FEED_STATISTICS_CSV_MAPPER.addMapping(FeedStatistics::setKiloBytesPerSecSent, DOUBLE);
        FEED_STATISTICS_CSV_MAPPER.addMapping(FeedStatistics::setAvgKiloBytesPerSecSent, DOUBLE);
    }
}
