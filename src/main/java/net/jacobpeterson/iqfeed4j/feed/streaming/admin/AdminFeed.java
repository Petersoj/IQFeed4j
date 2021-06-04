package net.jacobpeterson.iqfeed4j.feed.streaming.admin;

import net.jacobpeterson.iqfeed4j.feed.AbstractFeed;
import net.jacobpeterson.iqfeed4j.feed.message.SingleMessageFuture;
import net.jacobpeterson.iqfeed4j.feed.streaming.StreamingCSVMappers;
import net.jacobpeterson.iqfeed4j.model.feed.common.enums.FeedCommand;
import net.jacobpeterson.iqfeed4j.model.feed.common.enums.FeedMessageType;
import net.jacobpeterson.iqfeed4j.model.feed.streaming.admin.ClientStatistics;
import net.jacobpeterson.iqfeed4j.model.feed.streaming.admin.ClientStatistics.Type;
import net.jacobpeterson.iqfeed4j.model.feed.streaming.admin.enums.AdminSystemCommand;
import net.jacobpeterson.iqfeed4j.model.feed.streaming.admin.enums.AdminSystemMessageType;
import net.jacobpeterson.iqfeed4j.model.feed.streaming.admin.enums.OnOffOption;
import net.jacobpeterson.iqfeed4j.model.feed.streaming.common.FeedStatistics;
import net.jacobpeterson.iqfeed4j.model.feed.streaming.common.FeedStatistics.Status;
import net.jacobpeterson.iqfeed4j.util.csv.mapper.IndexCSVMapper;
import net.jacobpeterson.iqfeed4j.util.string.LineEnding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

import static com.google.common.base.Preconditions.checkNotNull;
import static net.jacobpeterson.iqfeed4j.util.csv.CSVUtil.valueEquals;
import static net.jacobpeterson.iqfeed4j.util.csv.CSVUtil.valueExists;
import static net.jacobpeterson.iqfeed4j.util.csv.CSVUtil.valueNotWhitespace;
import static net.jacobpeterson.iqfeed4j.util.csv.mapper.CSVMapper.DateTimeConverters.DATE_SPACE_TIME;
import static net.jacobpeterson.iqfeed4j.util.csv.mapper.CSVMapper.PrimitiveConvertors.DOUBLE;
import static net.jacobpeterson.iqfeed4j.util.csv.mapper.CSVMapper.PrimitiveConvertors.INTEGER;
import static net.jacobpeterson.iqfeed4j.util.csv.mapper.CSVMapper.PrimitiveConvertors.STRING;

/**
 * {@link AdminFeed} represents the Admin {@link AbstractFeed}. Methods in this class are not synchronized.
 */
public class AdminFeed extends AbstractFeed {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminFeed.class);
    protected static final String FEED_NAME_SUFFIX = " Admin Feed";
    protected static final IndexCSVMapper<ClientStatistics> CLIENT_STATISTICS_CSV_MAPPER;

    static {
        // Add mappings with CSV indices analogous to line of execution

        CLIENT_STATISTICS_CSV_MAPPER = new IndexCSVMapper<>(ClientStatistics::new);
        CLIENT_STATISTICS_CSV_MAPPER.addMapping(ClientStatistics::setType, Type::fromValue);
        CLIENT_STATISTICS_CSV_MAPPER.addMapping(ClientStatistics::setClientID, INTEGER);
        CLIENT_STATISTICS_CSV_MAPPER.addMapping(ClientStatistics::setClientName, STRING);
        CLIENT_STATISTICS_CSV_MAPPER.addMapping(ClientStatistics::setStartTime, DATE_SPACE_TIME);
        CLIENT_STATISTICS_CSV_MAPPER.addMapping(ClientStatistics::setSymbols, INTEGER);
        CLIENT_STATISTICS_CSV_MAPPER.addMapping(ClientStatistics::setRegionalSymbols, INTEGER);
        CLIENT_STATISTICS_CSV_MAPPER.addMapping(ClientStatistics::setKiloBytesReceived, DOUBLE);
        CLIENT_STATISTICS_CSV_MAPPER.addMapping(ClientStatistics::setKiloBytesSent, DOUBLE);
        CLIENT_STATISTICS_CSV_MAPPER.addMapping(ClientStatistics::setKiloBytesQueued, DOUBLE);
    }

    protected final Object messageReceivedLock;
    protected final HashMap<Integer, ClientStatistics> clientStatisticsOfClientIDs;
    protected final HashMap<AdminSystemMessageType, SingleMessageFuture<Void>> voidFutureOfAdminSystemMessageTypes;
    protected final HashMap<AdminSystemMessageType, SingleMessageFuture<String>> stringFutureOfAdminSystemMessageTypes;
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
        super(adminFeedName + FEED_NAME_SUFFIX, hostname, port, COMMA_DELIMITED_SPLITTER, true, true);

        messageReceivedLock = new Object();
        clientStatisticsOfClientIDs = new HashMap<>();
        voidFutureOfAdminSystemMessageTypes = new HashMap<>();
        stringFutureOfAdminSystemMessageTypes = new HashMap<>();
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
                AdminSystemMessageType parsedAdminSystemMessageType = AdminSystemMessageType.fromValue(csv[1]);

                switch (parsedAdminSystemMessageType) {
                    // Complete Void Futures
                    case REGISTER_CLIENT_APP_COMPLETED:
                    case REMOVE_CLIENT_APP_COMPLETED:
                    case LOGIN_INFO_SAVED:
                    case LOGIN_INFO_NOT_SAVED:
                    case AUTOCONNECT_ON:
                    case AUTOCONNECT_OFF:
                        SingleMessageFuture<Void> voidFuture =
                                voidFutureOfAdminSystemMessageTypes.get(parsedAdminSystemMessageType);
                        if (voidFuture != null) {
                            voidFuture.complete(null);
                            voidFutureOfAdminSystemMessageTypes.put(parsedAdminSystemMessageType, null);
                        } else {
                            LOGGER.error("Could not complete {} future!", parsedAdminSystemMessageType);
                        }
                        break;
                    // Complete String Futures
                    case CURRENT_LOGINID:
                    case CURRENT_PASSWORD:
                        SingleMessageFuture<String> stringFuture =
                                stringFutureOfAdminSystemMessageTypes.get(parsedAdminSystemMessageType);
                        if (stringFuture != null) {
                            if (valueExists(csv, 2)) {
                                stringFuture.complete(csv[2]);
                            } else {
                                stringFuture.completeExceptionally(new RuntimeException("CSV response value missing!"));
                            }
                            stringFutureOfAdminSystemMessageTypes.put(parsedAdminSystemMessageType, null);
                        } else {
                            LOGGER.error("Could not complete {} future!", parsedAdminSystemMessageType);
                        }
                        break;
                    case STATS:
                        try {
                            FeedStatistics feedStatistics = StreamingCSVMappers.FEED_STATISTICS_CSV_MAPPER.map(csv, 2);
                            lastFeedStatistics = feedStatistics;

                            if (feedStatisticsFuture != null) {
                                feedStatisticsFuture.complete(feedStatistics);
                                feedStatisticsFuture = null;
                            } else {
                                LOGGER.error("Could not complete {} future!", parsedAdminSystemMessageType);
                            }
                        } catch (Exception exception) {
                            if (feedStatisticsFuture != null) {
                                feedStatisticsFuture.completeExceptionally(exception);
                                feedStatisticsFuture = null;
                            } else {
                                LOGGER.error("Could not complete {} future!", parsedAdminSystemMessageType);
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
                            } else {
                                LOGGER.error("Could not complete {} future!", parsedAdminSystemMessageType);
                            }
                        } catch (Exception exception) {
                            if (clientStatisticsFuture != null) {
                                clientStatisticsFuture.completeExceptionally(exception);
                                clientStatisticsFuture = null;
                            } else {
                                LOGGER.error("Could not complete {} future!", parsedAdminSystemMessageType);
                            }
                        }
                        break;
                    default:
                        LOGGER.error("Unhandled message type: {}", parsedAdminSystemMessageType);
                }
            } catch (Exception exception) {
                LOGGER.error("Received unknown message type: {}", csv[1], exception);
            }
        }
    }

    /**
     * Sends a {@link FeedCommand#SYSTEM} {@link AdminSystemCommand}.
     *
     * @param adminSystemCommand the {@link AdminSystemCommand}
     * @param arguments          the arguments. Null for no arguments.
     *
     * @throws IOException thrown for {@link IOException}s
     */
    private void sendAdminSystemCommand(AdminSystemCommand adminSystemCommand, String... arguments) throws IOException {
        StringJoiner commandJoiner = new StringJoiner(",", "", LineEnding.CR_LF.getASCIIString());
        commandJoiner.add(FeedCommand.SYSTEM.value());
        commandJoiner.add(adminSystemCommand.value());
        if (arguments != null && arguments.length != 0) {
            for (String argument : arguments) {
                commandJoiner.add(argument);
            }
        }

        sendAndLogMessage(commandJoiner.toString());
    }

    /**
     * Gets the {@link AdminSystemCommand} {@link SingleMessageFuture} or calls {@link
     * #sendAdminSystemCommand(AdminSystemCommand, String...)} and creates/puts a new {@link SingleMessageFuture}. This
     * method is synchronized with {@link #messageReceivedLock}.
     *
     * @param <T>                             the {@link SingleMessageFuture} type
     * @param futureOfAdminSystemMessageTypes the {@link SingleMessageFuture}s of {@link AdminSystemMessageType}s
     * @param AdminSystemMessageType          the {@link AdminSystemMessageType}
     * @param AdminSystemCommand              the {@link AdminSystemCommand} for the {@link AdminSystemMessageType}
     * @param arguments                       the arguments
     *
     * @return a {@link SingleMessageFuture}
     *
     * @throws IOException thrown for {@link IOException}s
     */
    private <T> SingleMessageFuture<T> getOrSendAdminSystemCommandFuture(
            Map<AdminSystemMessageType, SingleMessageFuture<T>> futureOfAdminSystemMessageTypes,
            AdminSystemMessageType AdminSystemMessageType,
            AdminSystemCommand AdminSystemCommand, String... arguments) throws IOException {
        synchronized (messageReceivedLock) {
            SingleMessageFuture<T> messageFuture = futureOfAdminSystemMessageTypes.get(AdminSystemMessageType);
            if (messageFuture != null) {
                return messageFuture;
            }

            sendAdminSystemCommand(AdminSystemCommand, arguments);
            messageFuture = new SingleMessageFuture<>();
            futureOfAdminSystemMessageTypes.put(AdminSystemMessageType, messageFuture);
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
        checkNotNull(productID);
        checkNotNull(productVersion);

        return getOrSendAdminSystemCommandFuture(voidFutureOfAdminSystemMessageTypes,
                AdminSystemMessageType.REGISTER_CLIENT_APP_COMPLETED,
                AdminSystemCommand.REGISTER_CLIENT_APP, productID, productVersion);
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
        checkNotNull(productID);
        checkNotNull(productVersion);

        return getOrSendAdminSystemCommandFuture(voidFutureOfAdminSystemMessageTypes,
                AdminSystemMessageType.REMOVE_CLIENT_APP_COMPLETED,
                AdminSystemCommand.REMOVE_CLIENT_APP, productID, productVersion);
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
        checkNotNull(loginID);

        return getOrSendAdminSystemCommandFuture(stringFutureOfAdminSystemMessageTypes,
                AdminSystemMessageType.CURRENT_LOGINID,
                AdminSystemCommand.SET_LOGINID, loginID);
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
        checkNotNull(password);

        return getOrSendAdminSystemCommandFuture(stringFutureOfAdminSystemMessageTypes,
                AdminSystemMessageType.CURRENT_PASSWORD,
                AdminSystemCommand.SET_PASSWORD, password);
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
        checkNotNull(onOffOption);

        switch (onOffOption) {
            case ON:
                return getOrSendAdminSystemCommandFuture(voidFutureOfAdminSystemMessageTypes,
                        AdminSystemMessageType.LOGIN_INFO_SAVED,
                        AdminSystemCommand.SET_SAVE_LOGIN_INFO, onOffOption.value());
            case OFF:
                return getOrSendAdminSystemCommandFuture(voidFutureOfAdminSystemMessageTypes,
                        AdminSystemMessageType.LOGIN_INFO_NOT_SAVED,
                        AdminSystemCommand.SET_SAVE_LOGIN_INFO, onOffOption.value());
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
        checkNotNull(onOffOption);

        switch (onOffOption) {
            case ON:
                return getOrSendAdminSystemCommandFuture(voidFutureOfAdminSystemMessageTypes,
                        AdminSystemMessageType.AUTOCONNECT_ON,
                        AdminSystemCommand.SET_AUTOCONNECT, onOffOption.value());
            case OFF:
                return getOrSendAdminSystemCommandFuture(voidFutureOfAdminSystemMessageTypes,
                        AdminSystemMessageType.AUTOCONNECT_OFF,
                        AdminSystemCommand.SET_AUTOCONNECT, onOffOption.value());
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
        sendAdminSystemCommand(AdminSystemCommand.CONNECT);
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
        sendAdminSystemCommand(AdminSystemCommand.DISCONNECT);
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
        sendAdminSystemCommand(AdminSystemCommand.CLIENTSTATS_ON);
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
        sendAdminSystemCommand(AdminSystemCommand.CLIENTSTATS_OFF);
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
