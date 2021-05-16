package net.jacobpeterson.iqfeed4j.feed.admin;

import net.jacobpeterson.iqfeed4j.feed.AbstractFeed;
import net.jacobpeterson.iqfeed4j.model.feedenums.streaming.admin.AdminMessageType;
import net.jacobpeterson.iqfeed4j.model.streaming.admin.ClientStatistics;
import net.jacobpeterson.iqfeed4j.model.streaming.admin.FeedStatistics;
import net.jacobpeterson.iqfeed4j.util.csv.CSVMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

import static net.jacobpeterson.iqfeed4j.util.csv.CSVMapper.DateTimeConverters.DATE_SPACE_TIME;
import static net.jacobpeterson.iqfeed4j.util.csv.CSVMapper.DateTimeConverters.MONTH3_DAY_TIME_AM_PM;
import static net.jacobpeterson.iqfeed4j.util.csv.CSVMapper.PrimitiveConvertors.DOUBLE;
import static net.jacobpeterson.iqfeed4j.util.csv.CSVMapper.PrimitiveConvertors.INT;
import static net.jacobpeterson.iqfeed4j.util.csv.CSVMapper.PrimitiveConvertors.STRING;

/**
 * {@link AdminFeed} represents the Admin {@link AbstractFeed}.
 */
public class AdminFeed extends AbstractFeed {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminFeed.class);
    private static final String FEED_NAME_SUFFIX = "Admin Feed";
    private static final CSVMapper<ClientStatistics> CLIENT_STATISTICS_CSV_MAPPER;
    private static final CSVMapper<FeedStatistics> FEED_STATISTICS_CSV_MAPPER;

    static {
        // Add mappings with CSV indices analogous to line of execution

        CLIENT_STATISTICS_CSV_MAPPER = new CSVMapper<>(ClientStatistics::new);
        CLIENT_STATISTICS_CSV_MAPPER.addMapping(ClientStatistics::setType, ClientStatistics.Type::fromValue);
        CLIENT_STATISTICS_CSV_MAPPER.addMapping(ClientStatistics::setClientID, INT);
        CLIENT_STATISTICS_CSV_MAPPER.addMapping(ClientStatistics::setClientName, STRING);
        CLIENT_STATISTICS_CSV_MAPPER.addMapping(ClientStatistics::setStartTime, DATE_SPACE_TIME);
        CLIENT_STATISTICS_CSV_MAPPER.addMapping(ClientStatistics::setSymbols, INT);
        CLIENT_STATISTICS_CSV_MAPPER.addMapping(ClientStatistics::setRegionalSymbols, INT);
        CLIENT_STATISTICS_CSV_MAPPER.addMapping(ClientStatistics::setKiloBytesReceived, DOUBLE);
        CLIENT_STATISTICS_CSV_MAPPER.addMapping(ClientStatistics::setKiloBytesSent, DOUBLE);
        CLIENT_STATISTICS_CSV_MAPPER.addMapping(ClientStatistics::setKiloBytesQueued, DOUBLE);

        FEED_STATISTICS_CSV_MAPPER = new CSVMapper<>(FeedStatistics::new);
        FEED_STATISTICS_CSV_MAPPER.addMapping(FeedStatistics::setServerIP, STRING);
        FEED_STATISTICS_CSV_MAPPER.addMapping(FeedStatistics::setServerPort, INT);
        FEED_STATISTICS_CSV_MAPPER.addMapping(FeedStatistics::setMaxSymbols, INT);
        FEED_STATISTICS_CSV_MAPPER.addMapping(FeedStatistics::setNumberOfSymbols, INT);
        FEED_STATISTICS_CSV_MAPPER.addMapping(FeedStatistics::setClientsConnected, INT);
        FEED_STATISTICS_CSV_MAPPER.addMapping(FeedStatistics::setSecondsSinceLastUpdate, INT);
        FEED_STATISTICS_CSV_MAPPER.addMapping(FeedStatistics::setReconnections, INT);
        FEED_STATISTICS_CSV_MAPPER.addMapping(FeedStatistics::setAttemptedReconnections, INT);
        FEED_STATISTICS_CSV_MAPPER.addMapping(FeedStatistics::setStartTime, MONTH3_DAY_TIME_AM_PM);
        FEED_STATISTICS_CSV_MAPPER.addMapping(FeedStatistics::setMarketTime, MONTH3_DAY_TIME_AM_PM);
        FEED_STATISTICS_CSV_MAPPER.addMapping(FeedStatistics::setStatus, FeedStatistics.Status::fromValue);
        FEED_STATISTICS_CSV_MAPPER.addMapping(FeedStatistics::setIqFeedVersion, STRING);
        FEED_STATISTICS_CSV_MAPPER.addMapping(FeedStatistics::setLoginID, STRING);
        FEED_STATISTICS_CSV_MAPPER.addMapping(FeedStatistics::setTotalKiloBytesReceived, DOUBLE);
        FEED_STATISTICS_CSV_MAPPER.addMapping(FeedStatistics::setKiloBytesPerSecReceived, DOUBLE);
        FEED_STATISTICS_CSV_MAPPER.addMapping(FeedStatistics::setAvgKiloBytesPerSecRecv, DOUBLE);
        FEED_STATISTICS_CSV_MAPPER.addMapping(FeedStatistics::setTotalKiloBytesSent, DOUBLE);
        FEED_STATISTICS_CSV_MAPPER.addMapping(FeedStatistics::setKiloBytesPerSecSent, DOUBLE);
        FEED_STATISTICS_CSV_MAPPER.addMapping(FeedStatistics::setAvgKiloBytesPerSecSent, DOUBLE);

    }

    private final HashMap<AdminMessageType, ArrayList<CompletableFuture<?>>> futureListOfAdminMessageTypes;
    private final HashMap<Integer, FeedStatistics> feedStatisticsOfClientIDs;

    private FeedStatistics lastFeedStatistics;

    /**
     * Instantiates a new {@link AdminFeed}.
     *
     * @param feedName the feed name
     * @param hostname the host name
     * @param port     the port
     */
    public AdminFeed(String feedName, String hostname, int port) {
        super(feedName + FEED_NAME_SUFFIX, hostname, port);

        // Create and populate 'futureListOfAdminMessageTypes'
        futureListOfAdminMessageTypes = new HashMap<>();
        for (AdminMessageType adminMessageType : AdminMessageType.values()) {
            futureListOfAdminMessageTypes.put(adminMessageType, new ArrayList<>());
        }

        feedStatisticsOfClientIDs = new HashMap<>();
    }

    @Override
    protected void onProtocolVersionValidated() {}

    @Override
    protected void onMessageReceived(String[] csv) {
        // TODO
    }

    @Override
    public void onAsyncException(String message, Exception exception) {
        LOGGER.error(message, exception);
    }
}
