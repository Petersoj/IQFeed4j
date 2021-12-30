package net.jacobpeterson.iqfeed4j.feed;

import com.google.common.base.Splitter;
import net.jacobpeterson.iqfeed4j.feed.message.FeedMessageListener;
import net.jacobpeterson.iqfeed4j.model.feed.common.enums.FeedCommand;
import net.jacobpeterson.iqfeed4j.model.feed.common.enums.FeedMessageType;
import net.jacobpeterson.iqfeed4j.util.string.LineEnding;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.StringJoiner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;

import static net.jacobpeterson.iqfeed4j.util.csv.CSVUtil.valueEquals;

/**
 * {@link AbstractFeed} represents a TCP socket/feed for IQFeed.
 */
public abstract class AbstractFeed implements Runnable {

    /**
     * Protocol versions are managed by the version control branches.
     */
    public static final String CURRENTLY_SUPPORTED_PROTOCOL_VERSION = "6.2";

    /**
     * A comma (,) delimited {@link Splitter} that ignores commas surrounded in quotes using a Regex {@link Pattern}.
     *
     * @see <a href="https://stackoverflow.com/a/1757107">"Java: splitting a comma-separated string" reference</a>
     */
    public static final Splitter QUOTE_ESCAPED_COMMA_DELIMITED_SPLITTER =
            Splitter.on(Pattern.compile(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)"));
    /**
     * A comma (,) delimited {@link Splitter}. This avoids slow Regex splitting.
     */
    public static final Splitter COMMA_DELIMITED_SPLITTER = Splitter.on(',');

    private static final int SOCKET_THREAD_JOIN_WAIT_MILLIS = 5000;

    protected final Logger logger;
    protected final String feedName;
    protected final String hostname;
    protected final int port;
    protected final Splitter csvSplitter;
    protected final boolean validateProtocolVersion;
    protected final boolean sendClientName;

    private Thread socketThread;
    private boolean socketThreadRunning;
    private Socket feedSocket;
    private BufferedWriter feedWriter;
    private BufferedReader feedReader;
    private volatile boolean intentionalSocketClose;
    private boolean protocolVersionValidated; // Non-volatile to allow cache use even though used across threads
    private volatile CompletableFuture<Void> protocolVersionValidatedFuture;
    protected FeedMessageListener<String[]> customFeedMessageListener;

    /**
     * Instantiates a new {@link AbstractFeed}.
     *
     * @param logger                  the {@link Logger}
     * @param feedName                the feed name
     * @param hostname                the host name
     * @param port                    the port
     * @param csvSplitter             the CSV {@link Splitter}
     * @param validateProtocolVersion true to send and validate the {@link #CURRENTLY_SUPPORTED_PROTOCOL_VERSION}
     * @param sendClientName          true to send the client <code>feedName</code>
     */
    public AbstractFeed(Logger logger, String feedName, String hostname, int port, Splitter csvSplitter,
            boolean validateProtocolVersion, boolean sendClientName) {
        this.logger = logger;
        this.feedName = feedName;
        this.hostname = hostname;
        this.port = port;
        this.csvSplitter = csvSplitter;
        this.validateProtocolVersion = validateProtocolVersion;
        this.sendClientName = sendClientName;
    }

    /**
     * Starts this feed connection (creates a new socket connection on a new thread and sets protocol version) or does
     * nothing if the feed is currently connected or has been previously connected.
     *
     * @throws IOException thrown for {@link IOException}s
     */
    public void start() throws IOException {
        if (socketThread != null) {
            return;
        }

        feedSocket = new Socket(hostname, port);
        feedWriter = new BufferedWriter(new OutputStreamWriter(feedSocket.getOutputStream(),
                StandardCharsets.US_ASCII));
        feedReader = new BufferedReader(new InputStreamReader(feedSocket.getInputStream(),
                StandardCharsets.US_ASCII));

        logger.debug("{} feed socket connection established.", feedName);

        if (validateProtocolVersion) {
            // Send protocol version command first
            String protocolVersionCommand = String.format("%s,%s,%s%s",
                    FeedCommand.SYSTEM.value(), FeedCommand.SET_PROTOCOL.value(),
                    CURRENTLY_SUPPORTED_PROTOCOL_VERSION,
                    LineEnding.CR_LF.getASCIIString());
            logger.debug("Setting protocol version: {}", protocolVersionCommand);
            sendMessage(protocolVersionCommand);
        }

        if (sendClientName) {
            String clientNameCommand = String.format("%s,%s,%s%s",
                    FeedCommand.SYSTEM.value(), FeedCommand.SET_CLIENT_NAME.value(), feedName,
                    LineEnding.CR_LF.getASCIIString());
            logger.debug("Setting client name: {}", clientNameCommand);
            sendMessage(clientNameCommand);
        }

        socketThreadRunning = true;

        socketThread = new Thread(this, feedName);
        socketThread.start();
    }

    /**
     * Stop this feed connection (closes the socket connection and stops the socket thread).
     * <br>
     * Note that once this {@link AbstractFeed} has been stopped, it cannot be started again, so a new instance of
     * {@link AbstractFeed} needs to be created.
     *
     * @throws IOException thrown for {@link IOException}s
     */
    public void stop() throws IOException {
        socketThreadRunning = false;
        closeSocket();

        logger.debug("{} feed socket stopped.", feedName);
    }

    /**
     * Closes the {@link #feedSocket}.
     *
     * @throws IOException thrown for {@link IOException}s
     */
    private void closeSocket() throws IOException {
        intentionalSocketClose = true;
        feedSocket.close(); // Interrupt blocking IO operations and close socket
    }

    @Override
    public void run() {
        while (socketThreadRunning) { // Check if thread should continue running
            try {
                String line = feedReader.readLine(); // Uses any line ending: CR, LF, or CRLF

                if (line == null) { // End of stream has been reached (EOF was sent)
                    closeSocket();
                    onFeedSocketClose();
                    return;
                } else {
                    logger.trace("Received message line: {}", line);

                    String[] csv = csvSplitter.splitToList(line).toArray(new String[0]);

                    // Confirm protocol version valid
                    if (validateProtocolVersion && !protocolVersionValidated &&
                            valueEquals(csv, 0, FeedMessageType.SYSTEM.value()) &&
                            valueEquals(csv, 1, FeedMessageType.CURRENT_PROTOCOL.value()) &&
                            valueEquals(csv, 2, CURRENTLY_SUPPORTED_PROTOCOL_VERSION)) {
                        logger.debug("Protocol version validated: {}", (Object) csv);

                        protocolVersionValidated = true;
                        onProtocolVersionValidated();
                        if (protocolVersionValidatedFuture != null) {
                            protocolVersionValidatedFuture.complete(null);
                        }
                    } else {
                        // Call message handlers
                        onMessageReceived(csv);
                        if (customFeedMessageListener != null) {
                            customFeedMessageListener.onMessageReceived(csv);
                        }
                    }
                }
            } catch (Exception exception) {
                if (intentionalSocketClose) {
                    onFeedSocketClose();
                } else {
                    try {
                        closeSocket();
                    } catch (Exception stopException) {
                        logger.error("Could not close {} socket!", feedName, stopException);
                    }

                    onFeedSocketException(exception);
                }

                return;
            }
        }
    }

    /**
     * Called when the protocol version has been validated.
     */
    protected void onProtocolVersionValidated() {}

    /**
     * Called when a message is received. Note: This method should NEVER block and should NEVER throw an exception!
     *
     * @param csv the CSV
     */
    protected abstract void onMessageReceived(String[] csv);

    /**
     * Called when the underlying {@link Socket} of this {@link AbstractFeed} throws an unexpected {@link Exception}.
     * {@link #onMessageReceived(String[])} is guaranteed to never be called on this instance after this method has been
     * invoked.
     *
     * @param exception the {@link Exception} thrown
     */
    protected abstract void onFeedSocketException(Exception exception);

    /**
     * Called when the underlying {@link Socket} of this {@link AbstractFeed} is closed gracefully. {@link
     * #onMessageReceived(String[])} is guaranteed to never be called on this instance after this method has been
     * invoked.
     */
    protected abstract void onFeedSocketClose();

    /**
     * Sends a message. This method is synchronized.
     *
     * @param message the message
     *
     * @throws IOException thrown for {@link IOException}s
     */
    protected void sendMessage(String message) throws IOException {
        feedWriter.write(message);
        feedWriter.flush();
    }

    /**
     * Calls {@link #sendMessage(String)} and logs the given <code>message</code>.
     *
     * @param message the message
     *
     * @throws IOException thrown for {@link IOException}s
     */
    protected void sendAndLogMessage(String message) throws IOException {
        logger.debug("Sending message: {}", message);
        sendMessage(message);
    }

    /**
     * Sends a {@link FeedCommand#SYSTEM} command.
     *
     * @param systemCommand the system command
     * @param arguments     the arguments. <code>null</code> for no arguments.
     *
     * @throws IOException thrown for {@link IOException}s
     */
    protected void sendSystemCommand(String systemCommand, String... arguments) throws IOException {
        StringJoiner commandJoiner = new StringJoiner(",", "", LineEnding.CR_LF.getASCIIString());
        commandJoiner.add(FeedCommand.SYSTEM.value());
        commandJoiner.add(systemCommand);
        if (arguments != null && arguments.length != 0) {
            for (String argument : arguments) {
                commandJoiner.add(argument);
            }
        }

        sendAndLogMessage(commandJoiner.toString());
    }

    /**
     * Checks if the {@link #feedSocket} is open.
     *
     * @return a boolean
     */
    public boolean isFeedSocketOpen() {
        return feedSocket != null && !feedSocket.isClosed();
    }

    /**
     * Checks if the feed is valid. Returns true if {@link #isFeedSocketOpen()} is true and {@link
     * #isProtocolVersionValidated()} is true
     *
     * @return a boolean
     */
    public boolean isValid() {
        return isFeedSocketOpen() && isProtocolVersionValidated();
    }

    /**
     * Gets {@link #feedName}.
     *
     * @return a {@link String}
     */
    public String getFeedName() {
        return feedName;
    }

    /**
     * Gets {@link #port}.
     *
     * @return an int
     */
    public int getPort() {
        return port;
    }

    /**
     * Is {@link #protocolVersionValidated}.
     *
     * @return a boolean
     */
    public boolean isProtocolVersionValidated() {
        return !validateProtocolVersion || protocolVersionValidated;
    }

    /**
     * Blocks until the protocol version is validated for this feed (uses {@link #protocolVersionValidated} {@link
     * CompletableFuture} internally).
     *
     * @param timeout     the timeout time
     * @param timeoutUnit the {@link TimeUnit} of <code>timeout</code>
     *
     * @throws TimeoutException thrown when <code>timeout</code> time has elapsed
     */
    public void waitForProtocolVersionValidation(long timeout, TimeUnit timeoutUnit) throws TimeoutException {
        if (isProtocolVersionValidated()) {
            return;
        }

        if (protocolVersionValidatedFuture == null) {
            protocolVersionValidatedFuture = new CompletableFuture<>();
        }

        try {
            protocolVersionValidatedFuture.get(timeout, timeoutUnit);
        } catch (InterruptedException | ExecutionException ignored) {}
    }

    /**
     * Sets {@link #customFeedMessageListener}.
     *
     * @param customFeedMessageListener the custom {@link FeedMessageListener}
     */
    public void setCustomFeedMessageListener(FeedMessageListener<String[]> customFeedMessageListener) {
        this.customFeedMessageListener = customFeedMessageListener;
    }
}
