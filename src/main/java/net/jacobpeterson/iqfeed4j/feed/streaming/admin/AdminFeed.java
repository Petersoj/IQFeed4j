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
import net.jacobpeterson.iqfeed4j.util.csv.mapper.index.IndexCSVMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static net.jacobpeterson.iqfeed4j.util.csv.CSVUtil.valueEquals;
import static net.jacobpeterson.iqfeed4j.util.csv.CSVUtil.valueExists;
import static net.jacobpeterson.iqfeed4j.util.csv.CSVUtil.valueNotWhitespace;
import static net.jacobpeterson.iqfeed4j.util.csv.mapper.AbstractCSVMapper.DateTimeConverters.DATE_SPACE_TIME;
import static net.jacobpeterson.iqfeed4j.util.csv.mapper.AbstractCSVMapper.PrimitiveConvertors.DOUBLE;
import static net.jacobpeterson.iqfeed4j.util.csv.mapper.AbstractCSVMapper.PrimitiveConvertors.INTEGER;
import static net.jacobpeterson.iqfeed4j.util.csv.mapper.AbstractCSVMapper.PrimitiveConvertors.STRING;

/**
 * {@link AdminFeed} represents the Admin {@link AbstractFeed}.
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
    protected FeedStatistics latestFeedStatistics;

    /**
     * Instantiates a new {@link AdminFeed}.
     *
     * @param adminFeedName the {@link AdminFeed} name
     * @param hostname      the host name
     * @param port          the port
     */
    public AdminFeed(String adminFeedName, String hostname, int port) {
        super(LOGGER, adminFeedName + FEED_NAME_SUFFIX, hostname, port, COMMA_DELIMITED_SPLITTER, true, true);

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
                AdminSystemMessageType adminSystemMessageType = AdminSystemMessageType.fromValue(csv[1]);

                switch (adminSystemMessageType) {
                    // Complete Void Futures
                    case REGISTER_CLIENT_APP_COMPLETED:
                    case REMOVE_CLIENT_APP_COMPLETED:
                    case LOGIN_INFO_SAVED:
                    case LOGIN_INFO_NOT_SAVED:
                    case AUTOCONNECT_ON:
                    case AUTOCONNECT_OFF:
                        handleVoidFutureMessage(adminSystemMessageType);
                        break;
                    // Complete String Futures
                    case CURRENT_LOGINID:
                    case CURRENT_PASSWORD:
                        handleStringFutureMessage(adminSystemMessageType, csv);
                        break;
                    case STATS:
                        handleStatsMessage(csv);
                        break;
                    case CLIENTSTATS:
                        handleClientStatsMessage(csv);
                        break;
                    default:
                        LOGGER.error("Unhandled message type: {}", adminSystemMessageType);
                }
            } catch (IllegalArgumentException illegalArgumentException) {
                LOGGER.error("Received unknown message type: {}", csv[1], illegalArgumentException);
            }
        }
    }

    private void handleVoidFutureMessage(AdminSystemMessageType adminSystemMessageType) {
        SingleMessageFuture<Void> voidFuture = voidFutureOfAdminSystemMessageTypes.get(adminSystemMessageType);
        if (voidFuture != null) {
            voidFuture.complete(null);
            voidFutureOfAdminSystemMessageTypes.put(adminSystemMessageType, null);
        } else {
            LOGGER.error("Could not complete {} future!", adminSystemMessageType);
        }
    }

    private void handleStringFutureMessage(AdminSystemMessageType adminSystemMessageType, String[] csv) {
        SingleMessageFuture<String> stringFuture =
                stringFutureOfAdminSystemMessageTypes.get(adminSystemMessageType);
        if (stringFuture != null) {
            if (valueExists(csv, 2)) {
                stringFuture.complete(csv[2]);
            } else {
                stringFuture.completeExceptionally(new RuntimeException("CSV response value missing!"));
            }
            stringFutureOfAdminSystemMessageTypes.put(adminSystemMessageType, null);
        } else {
            LOGGER.error("Could not complete {} future!", adminSystemMessageType);
        }
    }

    private void handleStatsMessage(String[] csv) {
        try {
            FeedStatistics feedStatistics = StreamingCSVMappers.FEED_STATISTICS_CSV_MAPPER.map(csv, 2);
            latestFeedStatistics = feedStatistics;

            if (feedStatisticsFuture != null) {
                feedStatisticsFuture.complete(feedStatistics);
                feedStatisticsFuture = null;
            }
        } catch (Exception exception) {
            if (feedStatisticsFuture != null) {
                feedStatisticsFuture.completeExceptionally(exception);
                feedStatisticsFuture = null;
            }
        }
    }

    private void handleClientStatsMessage(String[] csv) {
        try {
            ClientStatistics clientStatistics = CLIENT_STATISTICS_CSV_MAPPER.map(csv, 2);
            clientStatisticsOfClientIDs.put(clientStatistics.getClientID(), clientStatistics);

            if (clientStatisticsFuture != null) {
                clientStatisticsFuture.complete(clientStatistics);
                clientStatisticsFuture = null;
            }
        } catch (Exception exception) {
            if (clientStatisticsFuture != null) {
                clientStatisticsFuture.completeExceptionally(exception);
                clientStatisticsFuture = null;
            }
        }
    }

    @Override
    protected void onFeedSocketException(Exception exception) {
        voidFutureOfAdminSystemMessageTypes.values().forEach(future -> future.completeExceptionally(exception));
        stringFutureOfAdminSystemMessageTypes.values().forEach(future -> future.completeExceptionally(exception));
        if (feedStatisticsFuture != null) {
            feedStatisticsFuture.completeExceptionally(exception);
        }
        if (clientStatisticsFuture != null) {
            clientStatisticsFuture.completeExceptionally(exception);
        }
    }

    @Override
    protected void onFeedSocketClose() {
        onFeedSocketException(new RuntimeException("Feed socket closed normally while a request was active!"));
    }

    //
    // START Feed commands
    //

    /**
     * Sends a {@link FeedCommand#SYSTEM} {@link AdminSystemCommand}.
     *
     * @param adminSystemCommand the {@link AdminSystemCommand}
     * @param arguments          the arguments. <code>null</code> for no arguments.
     *
     * @throws IOException thrown for {@link IOException}s
     */
    private void sendAdminSystemCommand(AdminSystemCommand adminSystemCommand, String... arguments) throws IOException {
        super.sendSystemCommand(adminSystemCommand.value(), arguments);
    }

    /**
     * Gets the {@link AdminSystemCommand} {@link SingleMessageFuture} or calls {@link
     * #sendAdminSystemCommand(AdminSystemCommand, String...)} and creates/puts a new {@link SingleMessageFuture}. This
     * method is synchronized with {@link #messageReceivedLock}.
     *
     * @param <T>                             the {@link SingleMessageFuture} type
     * @param futureOfAdminSystemMessageTypes the {@link SingleMessageFuture}s of {@link AdminSystemMessageType}s
     * @param adminSystemMessageType          the {@link AdminSystemMessageType}
     * @param adminSystemCommand              the {@link AdminSystemCommand} for the {@link AdminSystemMessageType}
     * @param arguments                       the arguments
     *
     * @return a {@link SingleMessageFuture}
     *
     * @throws IOException thrown for {@link IOException}s
     */
    private <T> SingleMessageFuture<T> getOrSendAdminSystemCommandFuture(
            Map<AdminSystemMessageType, SingleMessageFuture<T>> futureOfAdminSystemMessageTypes,
            AdminSystemMessageType adminSystemMessageType,
            AdminSystemCommand adminSystemCommand, String... arguments) throws IOException {
        synchronized (messageReceivedLock) {
            SingleMessageFuture<T> messageFuture = futureOfAdminSystemMessageTypes.get(adminSystemMessageType);
            if (messageFuture != null) {
                return messageFuture;
            }

            messageFuture = new SingleMessageFuture<>();
            futureOfAdminSystemMessageTypes.put(adminSystemMessageType, messageFuture);
            sendAdminSystemCommand(adminSystemCommand, arguments);
            return messageFuture;
        }
    }

    /**
     * Registers your application with the feed. Users will not be able to login to the feed until an application is
     * registered. This sends a {@link AdminSystemCommand#REGISTER_CLIENT_APP} request.
     *
     * @param productID      the Registered Product ID that you were assigned when you created your developer API
     *                       account.
     * @param productVersion the version of YOUR application.
     *
     * @return a {@link SingleMessageFuture} completed upon command response
     *
     * @throws IOException thrown for {@link IOException}s
     */
    public SingleMessageFuture<Void> registerClientApp(String productID, String productVersion) throws IOException {
        checkNotNull(productID);
        checkNotNull(productVersion);

        return getOrSendAdminSystemCommandFuture(voidFutureOfAdminSystemMessageTypes,
                AdminSystemMessageType.REGISTER_CLIENT_APP_COMPLETED,
                AdminSystemCommand.REGISTER_CLIENT_APP, productID, productVersion);
    }

    /**
     * Removes the registration of your application with the feed. This sends a {@link
     * AdminSystemCommand#REMOVE_CLIENT_APP} request.
     *
     * @param productID      the Registered Product ID that you were assigned when you created your developer API
     *                       account.
     * @param productVersion the version of YOUR application.
     *
     * @return a {@link SingleMessageFuture} completed upon command response
     *
     * @throws IOException thrown for {@link IOException}s
     */
    public SingleMessageFuture<Void> removeClientApp(String productID, String productVersion) throws IOException {
        checkNotNull(productID);
        checkNotNull(productVersion);

        return getOrSendAdminSystemCommandFuture(voidFutureOfAdminSystemMessageTypes,
                AdminSystemMessageType.REMOVE_CLIENT_APP_COMPLETED,
                AdminSystemCommand.REMOVE_CLIENT_APP, productID, productVersion);
    }

    /**
     * Sets the user loginID for IQFeed. This sends a {@link AdminSystemCommand#SET_LOGINID} request.
     *
     * @param loginID the loginID that was assigned to the user when they created their datafeed account.
     *
     * @return a {@link SingleMessageFuture} completed upon command response
     *
     * @throws IOException thrown for {@link IOException}s
     */
    public SingleMessageFuture<String> setLoginID(String loginID) throws IOException {
        checkNotNull(loginID);

        return getOrSendAdminSystemCommandFuture(stringFutureOfAdminSystemMessageTypes,
                AdminSystemMessageType.CURRENT_LOGINID,
                AdminSystemCommand.SET_LOGINID, loginID);
    }

    /**
     * Sets the user password for IQFeed. This sends a {@link AdminSystemCommand#SET_PASSWORD} request.
     *
     * @param password the password that was assigned to the user when they created their datafeed account.
     *
     * @return a {@link SingleMessageFuture} completed upon command response
     *
     * @throws IOException thrown for {@link IOException}s
     */
    public SingleMessageFuture<String> setPassword(String password) throws IOException {
        checkNotNull(password);

        return getOrSendAdminSystemCommandFuture(stringFutureOfAdminSystemMessageTypes,
                AdminSystemMessageType.CURRENT_PASSWORD,
                AdminSystemCommand.SET_PASSWORD, password);
    }

    /**
     * Sets the save login info (loginID/password) flag for IQFeed. This will be ignored at the time of connection if
     * either the loginID, password are not set. This sends a {@link AdminSystemCommand#SET_SAVE_LOGIN_INFO} request.
     *
     * @param onOffOption {@link OnOffOption#ON} if you want IQConnect to save the user's loginID and password or {@link
     *                    OnOffOption#OFF} if you do not want IQConnect to save the user's loginID and password.
     *
     * @return a {@link SingleMessageFuture} completed upon command response
     *
     * @throws IOException thrown for {@link IOException}s
     */
    public SingleMessageFuture<Void> setSaveLoginInfo(OnOffOption onOffOption) throws IOException {
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
     * password are not set. This sends a {@link AdminSystemCommand#SET_AUTOCONNECT} request.
     *
     * @param onOffOption {@link OnOffOption#ON} if you want IQConnect to automatically connect to the servers or {@link
     *                    OnOffOption#OFF} if you do not want IQConnect to automatically connect.
     *
     * @return a {@link SingleMessageFuture} completed upon command response
     *
     * @throws IOException thrown for {@link IOException}s
     */
    public SingleMessageFuture<Void> setAutoconnect(OnOffOption onOffOption) throws IOException {
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
     * connected. This sends a {@link AdminSystemCommand#CONNECT} request.
     * <br>
     * There is no set message associated with this command but you should notice the connection status in the {@link
     * #getLatestFeedStatistics()} message change from {@link Status#NOT_CONNECTED} to {@link Status#CONNECTED} if the
     * connection was successful.
     *
     * @throws IOException thrown for {@link IOException}s
     */
    public void connect() throws IOException {
        sendAdminSystemCommand(AdminSystemCommand.CONNECT);
    }

    /**
     * Tells IQConnect.exe to disconnect from the IQFeed servers. This happens automatically as soon as the last client
     * connection to IQConnect is terminated and the ClientsConnected value in the {@link ClientStatistics} message
     * returns to zero (after having incremented above zero). This message is ignored if the feed is already
     * disconnected. This sends a {@link AdminSystemCommand#DISCONNECT} request.
     * <br>
     * There is no set message associated with this command but you should notice the connection status in the {@link
     * #getLatestFeedStatistics()} message change from {@link Status#CONNECTED} to {@link Status#NOT_CONNECTED} if the
     * disconnection was successful.
     *
     * @throws IOException thrown for {@link IOException}s
     */
    public void disconnect() throws IOException {
        sendAdminSystemCommand(AdminSystemCommand.DISCONNECT);
    }

    /**
     * Tells IQConnect.exe to start streaming client stats to your connection. This sends a {@link
     * AdminSystemCommand#CLIENTSTATS_ON} request.
     * <br>
     * There is no set message associated with this command but you should start receiving {@link ClientStatistics}
     * messages (detailed on the Admin System Messages page). You will receive 1 stats message per second per client
     * currently connected to IQFeed.
     *
     * @throws IOException thrown for {@link IOException}s
     * @see #getClientStatisticsOfClientIDs()
     */
    public void setClientStatsOn() throws IOException {
        sendAdminSystemCommand(AdminSystemCommand.CLIENTSTATS_ON);
    }

    /**
     * Tells IQConnect.exe to stop streaming client stats to your connection. This sends a {@link
     * AdminSystemCommand#CLIENTSTATS_OFF} request.
     * <br>
     * There is no set message associated with this command but you should stop receiving {@link ClientStatistics}
     * messages.
     *
     * @throws IOException thrown for {@link IOException}s
     * @see #getClientStatisticsOfClientIDs()
     */
    public void setClientStatsOff() throws IOException {
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
     * Gets {@link #latestFeedStatistics}. This method is synchronized with {@link #messageReceivedLock}.
     *
     * @return the last {@link FeedStatistics}
     */
    public FeedStatistics getLatestFeedStatistics() {
        synchronized (messageReceivedLock) {
            return latestFeedStatistics;
        }
    }
}
