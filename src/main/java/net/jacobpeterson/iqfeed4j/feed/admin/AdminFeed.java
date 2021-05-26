package net.jacobpeterson.iqfeed4j.feed.admin;

import com.google.common.base.Preconditions;
import net.jacobpeterson.iqfeed4j.feed.AbstractFeed;
import net.jacobpeterson.iqfeed4j.feed.SingleMessageFuture;
import net.jacobpeterson.iqfeed4j.model.feedenums.FeedCommand;
import net.jacobpeterson.iqfeed4j.model.feedenums.FeedMessageType;
import net.jacobpeterson.iqfeed4j.model.feedenums.streaming.admin.AdminCommand;
import net.jacobpeterson.iqfeed4j.model.feedenums.streaming.admin.AdminMessageType;
import net.jacobpeterson.iqfeed4j.model.feedenums.util.OnOffOption;
import net.jacobpeterson.iqfeed4j.model.streaming.admin.ClientStatistics;
import net.jacobpeterson.iqfeed4j.model.streaming.admin.ClientStatistics.Type;
import net.jacobpeterson.iqfeed4j.model.streaming.admin.FeedStatistics;
import net.jacobpeterson.iqfeed4j.model.streaming.admin.FeedStatistics.Status;
import net.jacobpeterson.iqfeed4j.util.csv.mapper.IndexCSVMapper;
import net.jacobpeterson.iqfeed4j.util.string.LineEnding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

import static net.jacobpeterson.iqfeed4j.util.csv.CSVUtil.*;
import static net.jacobpeterson.iqfeed4j.util.csv.mapper.CSVMapper.DateTimeConverters.DATE_SPACE_TIME;
import static net.jacobpeterson.iqfeed4j.util.csv.mapper.CSVMapper.DateTimeConverters.MONTH3_DAY_TIME_AM_PM;
import static net.jacobpeterson.iqfeed4j.util.csv.mapper.CSVMapper.PrimitiveConvertors.*;

/**
 * {@link AdminFeed} represents the Admin {@link AbstractFeed}. Methods in this class are not synchronized.
 */
public class AdminFeed extends AbstractFeed {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminFeed.class);
    protected static final String FEED_NAME_SUFFIX = " Admin Feed";
    protected static final IndexCSVMapper<ClientStatistics> CLIENT_STATISTICS_CSV_MAPPER;
    protected static final IndexCSVMapper<FeedStatistics> FEED_STATISTICS_CSV_MAPPER;

    static {
        // Add mappings with CSV indices analogous to line of execution

        CLIENT_STATISTICS_CSV_MAPPER = new IndexCSVMapper<>(ClientStatistics::new);
        CLIENT_STATISTICS_CSV_MAPPER.addMapping(ClientStatistics::setType, Type::fromValue);
        CLIENT_STATISTICS_CSV_MAPPER.addMapping(ClientStatistics::setClientID, INT);
        CLIENT_STATISTICS_CSV_MAPPER.addMapping(ClientStatistics::setClientName, STRING);
        CLIENT_STATISTICS_CSV_MAPPER.addMapping(ClientStatistics::setStartTime, DATE_SPACE_TIME);
        CLIENT_STATISTICS_CSV_MAPPER.addMapping(ClientStatistics::setSymbols, INT);
        CLIENT_STATISTICS_CSV_MAPPER.addMapping(ClientStatistics::setRegionalSymbols, INT);
        CLIENT_STATISTICS_CSV_MAPPER.addMapping(ClientStatistics::setKiloBytesReceived, DOUBLE);
        CLIENT_STATISTICS_CSV_MAPPER.addMapping(ClientStatistics::setKiloBytesSent, DOUBLE);
        CLIENT_STATISTICS_CSV_MAPPER.addMapping(ClientStatistics::setKiloBytesQueued, DOUBLE);

        FEED_STATISTICS_CSV_MAPPER = new IndexCSVMapper<>(FeedStatistics::new);
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
        FEED_STATISTICS_CSV_MAPPER.addMapping(FeedStatistics::setStatus, Status::fromValue);
        FEED_STATISTICS_CSV_MAPPER.addMapping(FeedStatistics::setIqFeedVersion, STRING);
        FEED_STATISTICS_CSV_MAPPER.addMapping(FeedStatistics::setLoginID, STRING);
        FEED_STATISTICS_CSV_MAPPER.addMapping(FeedStatistics::setTotalKiloBytesReceived, DOUBLE);
        FEED_STATISTICS_CSV_MAPPER.addMapping(FeedStatistics::setKiloBytesPerSecReceived, DOUBLE);
        FEED_STATISTICS_CSV_MAPPER.addMapping(FeedStatistics::setAvgKiloBytesPerSecRecv, DOUBLE);
        FEED_STATISTICS_CSV_MAPPER.addMapping(FeedStatistics::setTotalKiloBytesSent, DOUBLE);
        FEED_STATISTICS_CSV_MAPPER.addMapping(FeedStatistics::setKiloBytesPerSecSent, DOUBLE);
        FEED_STATISTICS_CSV_MAPPER.addMapping(FeedStatistics::setAvgKiloBytesPerSecSent, DOUBLE);
    }

    protected final Object messageReceivedLock;
    protected final HashMap<Integer, ClientStatistics> clientStatisticsOfClientIDs;
    protected final HashMap<AdminMessageType, SingleMessageFuture<Void>> voidFutureOfAdminMessageTypes;
    protected final HashMap<AdminMessageType, SingleMessageFuture<String>> stringFutureOfAdminMessageTypes;
    protected SingleMessageFuture<FeedStatistics> feedStatisticsFuture;
    protected SingleMessageFuture<ClientStatistics> clientStatisticsFuture;
    protected FeedStatistics lastFeedStatistics;

    /**
     * Instantiates a new {@link AdminFeed}.
     *
     * @param adminFeedName the {@link AdminFeed} name
     * @param hostname      the host name
     * @param port          the port
     */
    public AdminFeed(String adminFeedName, String hostname, int port) {
        super(adminFeedName + FEED_NAME_SUFFIX, hostname, port, COMMA_DELIMITED_SPLITTER);

        messageReceivedLock = new Object();
        clientStatisticsOfClientIDs = new HashMap<>();
        voidFutureOfAdminMessageTypes = new HashMap<>();
        stringFutureOfAdminMessageTypes = new HashMap<>();
    }

    @Override
    protected void onMessageReceived(String[] csv) {
        if (valueEquals(csv, 0, FeedMessageType.ERROR.value())) {
            LOGGER.error("Received error message! {}", (Object) csv);
            return;
        }

        // Confirm message format
        if (!valueEquals(csv, 0, FeedMessageType.SYSTEM.value()) || !valueNotWhitespace(csv, 1)) {
            LOGGER.error("Received unknown message format: {}", (Object) csv);
            return;
        }

        synchronized (messageReceivedLock) {
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
                        SingleMessageFuture<Void> voidFuture =
                                voidFutureOfAdminMessageTypes.get(parsedAdminMessageType);
                        if (voidFuture != null) {
                            voidFuture.complete(null);
                            voidFutureOfAdminMessageTypes.put(parsedAdminMessageType, null);
                        } else {
                            LOGGER.error("Could not complete {} future!", parsedAdminMessageType);
                        }
                        break;
                    // Complete String Futures
                    case CURRENT_LOGINID:
                    case CURRENT_PASSWORD:
                        SingleMessageFuture<String> stringFuture =
                                stringFutureOfAdminMessageTypes.get(parsedAdminMessageType);
                        if (stringFuture != null) {
                            if (valueExists(csv, 2)) {
                                stringFuture.complete(csv[2]);
                            } else {
                                stringFuture.completeExceptionally(new RuntimeException("CSV response value missing!"));
                            }
                            stringFutureOfAdminMessageTypes.put(parsedAdminMessageType, null);
                        } else {
                            LOGGER.error("Could not complete {} future!", parsedAdminMessageType);
                        }
                        break;
                    case STATS:
                        try {
                            FeedStatistics feedStatistics = FEED_STATISTICS_CSV_MAPPER.map(csv, 2);
                            lastFeedStatistics = feedStatistics;

                            if (feedStatisticsFuture != null) {
                                feedStatisticsFuture.complete(feedStatistics);
                                feedStatisticsFuture = null;
                            }
                        } catch (Exception exception) {
                            LOGGER.error("Could not map {}!", parsedAdminMessageType, exception);
                            if (feedStatisticsFuture != null) {
                                feedStatisticsFuture.completeExceptionally(exception);
                                feedStatisticsFuture = null;
                            }
                        }
                        break;
                    case CLIENTSTATS:
                        try {
                            ClientStatistics clientStatistics = CLIENT_STATISTICS_CSV_MAPPER.map(csv, 2);
                            clientStatisticsOfClientIDs.put(clientStatistics.getClientID(), clientStatistics);

                            if (clientStatisticsFuture != null) {
                                clientStatisticsFuture.complete(clientStatistics);
                                clientStatisticsFuture = null;
                            }
                        } catch (Exception exception) {
                            LOGGER.error("Could not map {}!", parsedAdminMessageType, exception);
                            if (clientStatisticsFuture != null) {
                                clientStatisticsFuture.completeExceptionally(exception);
                                clientStatisticsFuture = null;
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

    /**
     * Sends a {@link FeedCommand#SYSTEM} {@link AdminCommand}.
     *
     * @param adminCommand the {@link AdminCommand}
     * @param arguments    the arguments. Null for no arguments.
     *
     * @throws IOException thrown for {@link IOException}s
     */
    private void sendAdminCommand(AdminCommand adminCommand, String... arguments) throws IOException {
        StringJoiner joiner = new StringJoiner(",", "", LineEnding.CR_LF.getASCIIString());
        joiner.add(FeedCommand.SYSTEM.value());
        joiner.add(adminCommand.value());
        if (arguments != null && arguments.length != 0) {
            for (String argument : arguments) {
                joiner.add(argument);
            }
        }

        String command = joiner.toString();
        LOGGER.debug("Sending Admin command message: {}", command);
        sendMessage(command);
    }

    /**
     * Gets the {@link AdminCommand} {@link SingleMessageFuture} or calls {@link #sendAdminCommand(AdminCommand,
     * String...)} and creates/puts a new {@link SingleMessageFuture}. This method is synchronized with {@link
     * #messageReceivedLock}.
     *
     * @param <T>                       the {@link SingleMessageFuture} type
     * @param futureOfAdminMessageTypes the {@link SingleMessageFuture}s of {@link AdminMessageType}s
     * @param adminMessageType          the {@link AdminMessageType}
     * @param adminCommand              the {@link AdminCommand} for the {@link AdminMessageType}
     * @param arguments                 the arguments
     *
     * @return a {@link SingleMessageFuture}
     *
     * @throws IOException thrown for {@link IOException}s
     */
    private <T> SingleMessageFuture<T> getOrSendAdminCommandFuture(
            Map<AdminMessageType, SingleMessageFuture<T>> futureOfAdminMessageTypes, AdminMessageType adminMessageType,
            AdminCommand adminCommand, String... arguments) throws IOException {
        synchronized (messageReceivedLock) {
            SingleMessageFuture<T> messageFuture = futureOfAdminMessageTypes.get(adminMessageType);
            if (messageFuture != null) {
                return messageFuture;
            }

            sendAdminCommand(adminCommand, arguments);
            messageFuture = new SingleMessageFuture<>();
            futureOfAdminMessageTypes.put(adminMessageType, messageFuture);
            return messageFuture;
        }
    }

    //
    // START Feed commands
    //

    /**
     * Registers your application with the feed. Users will not be able to login to the feed until an application is
     * registered.
     *
     * @param productID      the Registered Product ID that you were assigned when you created your developer API
     *                       account.
     * @param productVersion the version of YOUR application.
     *
     * @return a {@link SingleMessageFuture} completed upon command response
     *
     * @throws Exception thrown for {@link Exception}s
     */
    public SingleMessageFuture<Void> registerClientApp(String productID, String productVersion) throws Exception {
        Preconditions.checkNotNull(productID);
        Preconditions.checkNotNull(productVersion);

        return getOrSendAdminCommandFuture(voidFutureOfAdminMessageTypes,
                AdminMessageType.REGISTER_CLIENT_APP_COMPLETED,
                AdminCommand.REGISTER_CLIENT_APP, productID, productVersion);
    }

    /**
     * Removes the registration of your application with the feed.
     *
     * @param productID      the Registered Product ID that you were assigned when you created your developer API
     *                       account.
     * @param productVersion the version of YOUR application.
     *
     * @return a {@link SingleMessageFuture} completed upon command response
     *
     * @throws Exception thrown for {@link Exception}s
     */
    public SingleMessageFuture<Void> removeClientApp(String productID, String productVersion) throws Exception {
        Preconditions.checkNotNull(productID);
        Preconditions.checkNotNull(productVersion);

        return getOrSendAdminCommandFuture(voidFutureOfAdminMessageTypes,
                AdminMessageType.REMOVE_CLIENT_APP_COMPLETED,
                AdminCommand.REMOVE_CLIENT_APP, productID, productVersion);
    }

    /**
     * Sets the user loginID for IQFeed.
     *
     * @param loginID the loginID that was assigned to the user when they created their datafeed account.
     *
     * @return a {@link SingleMessageFuture} completed upon command response
     *
     * @throws Exception thrown for {@link Exception}s
     */
    public SingleMessageFuture<String> setLoginID(String loginID) throws Exception {
        Preconditions.checkNotNull(loginID);

        return getOrSendAdminCommandFuture(stringFutureOfAdminMessageTypes,
                AdminMessageType.CURRENT_LOGINID,
                AdminCommand.SET_LOGINID, loginID);
    }

    /**
     * Sets the user password for IQFeed.
     *
     * @param password the password that was assigned to the user when they created their datafeed account.
     *
     * @return a {@link SingleMessageFuture} completed upon command response
     *
     * @throws Exception thrown for {@link Exception}s
     */
    public SingleMessageFuture<String> setPassword(String password) throws Exception {
        Preconditions.checkNotNull(password);

        return getOrSendAdminCommandFuture(stringFutureOfAdminMessageTypes,
                AdminMessageType.CURRENT_PASSWORD,
                AdminCommand.SET_PASSWORD, password);
    }

    /**
     * Sets the save login info (loginID/password) flag for IQFeed. This will be ignored at the time of connection if
     * either the loginID, password are not set.
     *
     * @param onOffOption {@link OnOffOption#ON} if you want IQConnect to save the user's loginID and password or {@link
     *                    OnOffOption#OFF} if you do not want IQConnect to save the user's loginID and password.
     *
     * @return a {@link SingleMessageFuture} completed upon command response
     *
     * @throws Exception thrown for {@link Exception}s
     */
    public SingleMessageFuture<Void> setSaveLoginInfo(OnOffOption onOffOption) throws Exception {
        Preconditions.checkNotNull(onOffOption);

        switch (onOffOption) {
            case ON:
                return getOrSendAdminCommandFuture(voidFutureOfAdminMessageTypes,
                        AdminMessageType.LOGIN_INFO_SAVED,
                        AdminCommand.SET_SAVE_LOGIN_INFO, onOffOption.value());
            case OFF:
                return getOrSendAdminCommandFuture(voidFutureOfAdminMessageTypes,
                        AdminMessageType.LOGIN_INFO_NOT_SAVED,
                        AdminCommand.SET_SAVE_LOGIN_INFO, onOffOption.value());
            default:
                throw new UnsupportedOperationException();
        }
    }

    /**
     * Sets the auto connect flag for IQFeed. This will be ignored at the time of connection if either the loginID,
     * password are not set.
     *
     * @param onOffOption {@link OnOffOption#ON} if you want IQConnect to automatically connect to the servers or {@link
     *                    OnOffOption#OFF} if you do not want IQConnect to automatically connect.
     *
     * @return a {@link SingleMessageFuture} completed upon command response
     *
     * @throws Exception thrown for {@link Exception}s
     */
    public SingleMessageFuture<Void> setAutoconnect(OnOffOption onOffOption) throws Exception {
        Preconditions.checkNotNull(onOffOption);

        switch (onOffOption) {
            case ON:
                return getOrSendAdminCommandFuture(voidFutureOfAdminMessageTypes,
                        AdminMessageType.AUTOCONNECT_ON,
                        AdminCommand.SET_AUTOCONNECT, onOffOption.value());
            case OFF:
                return getOrSendAdminCommandFuture(voidFutureOfAdminMessageTypes,
                        AdminMessageType.AUTOCONNECT_OFF,
                        AdminCommand.SET_AUTOCONNECT, onOffOption.value());
            default:
                throw new UnsupportedOperationException();
        }
    }

    /**
     * Tells IQConnect.exe to initiate a connection to the servers. This happens automatically upon launching the feed
     * unless the ProductID and/or Product version have not been set. This message is ignored if the feed is already
     * connected.
     * <br>
     * There is no set message associated with this command but you should notice the connection status in the {@link
     * #getLastFeedStatistics()} message change from {@link Status#NOT_CONNECTED} to {@link Status#CONNECTED} if the
     * connection was successful.
     *
     * @throws Exception thrown for {@link Exception}s
     */
    public void connect() throws Exception {
        sendAdminCommand(AdminCommand.CONNECT);
    }

    /**
     * Tells IQConnect.exe to disconnect from the IQFeed servers. This happens automatically as soon as the last client
     * connection to IQConnect is terminated and the ClientsConnected value in the S,STATS message returns to zero
     * (after having incremented above zero). This message is ignored if the feed is already disconnected.
     * <br>
     * There is no set message associated with this command but you should notice the connection status in the {@link
     * #getLastFeedStatistics()} message change from {@link Status#CONNECTED} to {@link Status#NOT_CONNECTED} if the
     * disconnection was successful.
     *
     * @throws Exception thrown for {@link Exception}s
     */
    public void disconnect() throws Exception {
        sendAdminCommand(AdminCommand.DISCONNECT);
    }

    /**
     * Tells IQConnect.exe to start streaming client stats to your connection.
     * <br>
     * There is no set message associated with this command but you should start receiving {@link ClientStatistics}
     * messages (detailed on the Admin System Messages page). You will receive 1 stats message per second per client
     * currently connected to IQFeed.
     *
     * @throws Exception thrown for {@link Exception}s
     * @see #getClientStatisticsOfClientIDs()
     */
    public void setClientStatsOn() throws Exception {
        sendAdminCommand(AdminCommand.CLIENTSTATS_ON);
    }

    /**
     * Tells IQConnect.exe to stop streaming client stats to your connection.
     * <br>
     * There is no set message associated with this command but you should stop receiving {@link ClientStatistics}
     * messages.
     *
     * @throws Exception thrown for {@link Exception}s
     * @see #getClientStatisticsOfClientIDs()
     */
    public void setClientStatsOff() throws Exception {
        sendAdminCommand(AdminCommand.CLIENTSTATS_OFF);
        synchronized (messageReceivedLock) {
            clientStatisticsOfClientIDs.clear();
        }
    }

    //
    // END Feed commands
    //

    /**
     * Gets a {@link SingleMessageFuture} for the next {@link FeedStatistics} message. This method is synchronized with
     * {@link #messageReceivedLock}.
     *
     * @return a {@link SingleMessageFuture} of {@link FeedStatistics}
     */
    public SingleMessageFuture<FeedStatistics> getNextFeedStatistics() {
        synchronized (messageReceivedLock) {
            if (feedStatisticsFuture != null) {
                return feedStatisticsFuture;
            }

            feedStatisticsFuture = new SingleMessageFuture<>();
            return feedStatisticsFuture;
        }
    }

    /**
     * Gets a {@link SingleMessageFuture} for the next {@link ClientStatistics} message. This method is synchronized
     * with {@link #messageReceivedLock}.
     *
     * @return a {@link SingleMessageFuture} of {@link ClientStatistics}
     */
    public SingleMessageFuture<ClientStatistics> getNextClientStatistics() {
        synchronized (messageReceivedLock) {
            if (clientStatisticsFuture != null) {
                return clientStatisticsFuture;
            }

            clientStatisticsFuture = new SingleMessageFuture<>();
            return clientStatisticsFuture;
        }
    }

    /**
     * Gets {@link #clientStatisticsOfClientIDs}. This method is synchronized with {@link #messageReceivedLock}.
     *
     * @return a {@link HashMap}
     */
    public HashMap<Integer, ClientStatistics> getClientStatisticsOfClientIDs() {
        synchronized (messageReceivedLock) {
            return clientStatisticsOfClientIDs;
        }
    }

    /**
     * Gets {@link #lastFeedStatistics}. This method is synchronized with {@link #messageReceivedLock}.
     *
     * @return the last {@link FeedStatistics}
     */
    public FeedStatistics getLastFeedStatistics() {
        synchronized (messageReceivedLock) {
            return lastFeedStatistics;
        }
    }
}
