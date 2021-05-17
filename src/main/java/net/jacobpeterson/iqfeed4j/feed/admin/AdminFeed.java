package net.jacobpeterson.iqfeed4j.feed.admin;

import net.jacobpeterson.iqfeed4j.feed.AbstractFeed;
import net.jacobpeterson.iqfeed4j.model.feedenums.FeedMessageType;
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
import static net.jacobpeterson.iqfeed4j.util.csv.CSVUtil.valueEquals;
import static net.jacobpeterson.iqfeed4j.util.csv.CSVUtil.valueExists;

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

    private final HashMap<AdminMessageType, ArrayList<CompletableFuture<Void>>> voidFutureListOfAdminMessageTypes;
    private final HashMap<AdminMessageType, ArrayList<CompletableFuture<String>>> stringFutureListOfAdminMessageTypes;
    private final ArrayList<CompletableFuture<FeedStatistics>> feedStatisticsFutureList;
    private final ArrayList<CompletableFuture<ClientStatistics>> clientStatisticsFutureList;
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

        voidFutureListOfAdminMessageTypes = new HashMap<>();
        // Create corresponding Void Future Lists
        voidFutureListOfAdminMessageTypes.put(AdminMessageType.REGISTER_CLIENT_APP_COMPLETED, new ArrayList<>());
        voidFutureListOfAdminMessageTypes.put(AdminMessageType.REMOVE_CLIENT_APP_COMPLETED, new ArrayList<>());
        voidFutureListOfAdminMessageTypes.put(AdminMessageType.LOGIN_INFO_SAVED, new ArrayList<>());
        voidFutureListOfAdminMessageTypes.put(AdminMessageType.LOGIN_INFO_NOT_SAVED, new ArrayList<>());
        voidFutureListOfAdminMessageTypes.put(AdminMessageType.AUTOCONNECT_ON, new ArrayList<>());
        voidFutureListOfAdminMessageTypes.put(AdminMessageType.AUTOCONNECT_OFF, new ArrayList<>());

        stringFutureListOfAdminMessageTypes = new HashMap<>();
        // Create corresponding String Future Lists
        stringFutureListOfAdminMessageTypes.put(AdminMessageType.CURRENT_LOGINID, new ArrayList<>());
        stringFutureListOfAdminMessageTypes.put(AdminMessageType.CURRENT_PASSWORD, new ArrayList<>());

        feedStatisticsFutureList = new ArrayList<>();
        clientStatisticsFutureList = new ArrayList<>();

        feedStatisticsOfClientIDs = new HashMap<>();
    }

    @Override
    protected void onProtocolVersionValidated() {}

    @Override
    protected void onMessageReceived(String[] csv) {
        // Confirm message format
        if (!valueEquals(csv, 0, FeedMessageType.SYSTEM.value()) || !valueExists(csv, 1)) {
            LOGGER.error("Received unknown message format: {}", (Object) csv);
            return;
        }

        try {
            AdminMessageType parsedAdminMessageType = AdminMessageType.fromValue(csv[1]);

            switch (parsedAdminMessageType) {
                // Complete Void Futures
                case REGISTER_CLIENT_APP_COMPLETED:
                case REMOVE_CLIENT_APP_COMPLETED:
                case LOGIN_INFO_SAVED:
                case LOGIN_INFO_NOT_SAVED:
                case AUTOCONNECT_ON:
                case AUTOCONNECT_OFF:
                    for (CompletableFuture<Void> voidFuture : voidFutureListOfAdminMessageTypes
                            .get(parsedAdminMessageType)) {
                        voidFuture.complete(null);
                    }
                    voidFutureListOfAdminMessageTypes.get(parsedAdminMessageType).clear();
                    break;
                // Complete String Futures
                case CURRENT_LOGINID:
                case CURRENT_PASSWORD:
                    if (valueExists(csv, 2)) {
                        for (CompletableFuture<String> stringFuture : stringFutureListOfAdminMessageTypes
                                .get(parsedAdminMessageType)) {
                            stringFuture.complete(csv[2]);
                        }
                    } else {
                        LOGGER.error("Received unknown message format: {}", (Object) csv);
                    }
                    break;
                case STATS:
                    FeedStatistics feedStatistics = FEED_STATISTICS_CSV_MAPPER.map(csv, 2);
                    if (feedStatistics != null) {
                        for (CompletableFuture<FeedStatistics> feedStatisticsFuture : feedStatisticsFutureList) {
                            feedStatisticsFuture.complete(feedStatistics);
                        }
                    }
                    break;
                case CLIENTSTATS:
                    ClientStatistics clientStatistics = CLIENT_STATISTICS_CSV_MAPPER.map(csv, 2);
                    if (clientStatistics != null) {
                        for (CompletableFuture<ClientStatistics> clientStatisticsFuture : clientStatisticsFutureList) {
                            clientStatisticsFuture.complete(clientStatistics);
                        }
                    }
                    break;
                default:
                    LOGGER.error("Unhandled message type: {}", parsedAdminMessageType);
            }
        } catch (Exception exception) {
            LOGGER.error("Received unknown message type: {}", csv[1], exception);
        }
    }

    @Override
    public void onAsyncException(String message, Exception exception) {
        LOGGER.error(message, exception);
    }
}
