package net.jacobpeterson.iqfeed4j.feed;

import com.google.common.base.Splitter;
import net.jacobpeterson.iqfeed4j.feed.message.FeedMessageListener;
import net.jacobpeterson.iqfeed4j.model.feed.common.enums.FeedCommand;
import net.jacobpeterson.iqfeed4j.model.feed.common.enums.FeedMessageType;
import net.jacobpeterson.iqfeed4j.util.string.LineEnding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
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

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractFeed.class);
    private static final int SOCKET_THREAD_JOIN_WAIT_MILLIS = 5000;

    protected final String feedName;
    protected final String hostname;
    protected final int port;
    protected final Splitter csvSplitter;
    protected final boolean validateProtocolVersion;
    protected final boolean sendClientName;
    private final Object startStopLock;

    private Thread socketThread;
    private Socket feedSocket;
    private BufferedWriter feedWriter;
    private BufferedReader feedReader;
    private boolean intentionalSocketClose;
    private boolean protocolVersionValidated;
    protected FeedMessageListener<String[]> customFeedMessageListener;

    /**
     * Instantiates a new {@link AbstractFeed}.
     *
     * @param feedName                the feed name
     * @param hostname                the host name
     * @param port                    the port
     * @param csvSplitter             the CSV {@link Splitter}
     * @param validateProtocolVersion true to send and validate the {@link #CURRENTLY_SUPPORTED_PROTOCOL_VERSION}
     * @param sendClientName          true to send the client 'feedName'
     */
    public AbstractFeed(String feedName, String hostname, int port, Splitter csvSplitter,
            boolean validateProtocolVersion, boolean sendClientName) {
        this.feedName = feedName;
        this.hostname = hostname;
        this.port = port;
        this.csvSplitter = csvSplitter;
        this.validateProtocolVersion = validateProtocolVersion;
        this.sendClientName = sendClientName;

        startStopLock = new Object();

        intentionalSocketClose = false;
    }

    /**
     * Starts this feed connection (starts a new socket, new thread, and send protocol message). This method is
     * synchronized with {@link #stop()}.
     *
     * @throws InterruptedException thrown for {@link InterruptedException}s
     * @throws IOException          thrown for {@link IOException}s
     */
    public void start() throws InterruptedException, IOException {
        synchronized (startStopLock) {
            if (socketThread != null && socketThread.isAlive()) {
                if (isFeedSocketOpen()) {
                    return; // thread is alive and socket is connected
                } else { // thread is alive, but socket is closed or null
                    interruptAndJoinThread();
                    cleanupState();
                }
            } else if (isFeedSocketOpen()) { // thread is dead, but socket is alive
                closeSocket();
                cleanupState();
            }

            feedSocket = new Socket(hostname, port);
            feedWriter = new BufferedWriter(new OutputStreamWriter(feedSocket.getOutputStream(),
                    StandardCharsets.US_ASCII));
            feedReader = new BufferedReader(new InputStreamReader(feedSocket.getInputStream(),
                    StandardCharsets.US_ASCII));

            LOGGER.debug("{} feed socket connection established.", feedName);

            if (validateProtocolVersion) {
                // Send protocol version command first
                String protocolVersionCommand = String.format("%s,%s,%s%s",
                        FeedCommand.SYSTEM.value(), FeedCommand.SET_PROTOCOL.value(),
                        CURRENTLY_SUPPORTED_PROTOCOL_VERSION,
                        LineEnding.CR_LF.getASCIIString());
                LOGGER.debug("Setting protocol version: {}", protocolVersionCommand);
                sendMessage(protocolVersionCommand);
            }

            if (sendClientName) {
                String clientNameCommand = String.format("%s,%s,%s%s",
                        FeedCommand.SYSTEM.value(), FeedCommand.SET_CLIENT_NAME.value(), feedName,
                        LineEnding.CR_LF.getASCIIString());
                LOGGER.debug("Setting client name: {}", clientNameCommand);
                sendMessage(clientNameCommand);
            }

            socketThread = new Thread(this, feedName);
            socketThread.start();
        }
    }

    /**
     * Stop this feed connection (close socket and interrupt thread). This method is synchronized with {@link
     * #start()}.
     *
     * @throws InterruptedException thrown for {@link InterruptedException}s
     * @throws IOException          thrown for {@link IOException}s
     */
    public void stop() throws InterruptedException, IOException {
        synchronized (startStopLock) {
            closeSocket();
            interruptAndJoinThread();
            cleanupState();

            LOGGER.debug("{} feed socket stopped.", feedName);
        }
    }

    /**
     * Closes the {@link #feedSocket}.
     *
     * @throws IOException thrown for {@link IOException}s
     */
    private void closeSocket() throws IOException {
        if (isFeedSocketOpen()) {
            intentionalSocketClose = true;
            feedSocket.close(); // Interrupt blocking IO operations and close socket
        }
    }

    /**
     * Interrupts the {@link #socketThread} and joins it to wait for thread completion.
     *
     * @throws InterruptedException thrown for {@link InterruptedException}s
     */
    private void interruptAndJoinThread() throws InterruptedException {
        if (socketThread != null) {
            socketThread.interrupt(); // Stops the thread if alive
            socketThread.join(SOCKET_THREAD_JOIN_WAIT_MILLIS); // Wait for thread to complete
        }
    }

    /**
     * Cleans ups this {@link AbstractFeed} state subsequent feed connections operated correctly.
     */
    protected void cleanupState() {
        intentionalSocketClose = false;
        protocolVersionValidated = false;
    }

    @Override
    public void run() {
        // Confirm start() method has released 'startStopLock' and exited synchronized scope
        synchronized (startStopLock) {}

        while (!Thread.currentThread().isInterrupted()) { // Check if thread has been closed/interrupted
            try {
                String line = feedReader.readLine(); // Uses any line ending: CR, LF, or CRLF

                if (line == null) { // The socket was closed (EOF was sent)
                    closeSocket();
                    cleanupState();
                    return;
                } else {
                    // Removed in production for version 6.2-1.4
                    LOGGER.trace("Received message line: {}", line);

                    String[] csv = csvSplitter.splitToList(line).toArray(new String[0]);

                    // Confirm protocol version valid
                    if (validateProtocolVersion && !protocolVersionValidated &&
                            valueEquals(csv, 0, FeedMessageType.SYSTEM.value()) &&
                            valueEquals(csv, 1, FeedMessageType.CURRENT_PROTOCOL.value()) &&
                            valueEquals(csv, 2, CURRENTLY_SUPPORTED_PROTOCOL_VERSION)) {
                        LOGGER.debug("Protocol version validated: {}", (Object) csv);

                        protocolVersionValidated = true;
                        onProtocolVersionValidated();
                    } else {
                        // Call message handlers
                        onMessageReceived(csv);
                        if (customFeedMessageListener != null) {
                            customFeedMessageListener.onMessageReceived(csv);
                        }
                    }
                }
            } catch (Exception exception) {
                if (!intentionalSocketClose) {
                    onAsyncException("Could not read and process feed socket message line!", exception);
                }
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
     * Called when an asynchronous {@link Exception} relating to the feed connection has occurred.
     *
     * @param message   the message
     * @param exception the {@link Exception}
     */
    protected void onAsyncException(String message, Exception exception) {
        LOGGER.error(message, exception);
        LOGGER.debug("Attempting to close {}...", feedName);
        try {
            stop();
        } catch (Exception stopException) {
            LOGGER.error("Could not close {}!", feedName, stopException);
        }
    }

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
     * Calls {@link #sendMessage(String)} and logs the given 'message'.
     *
     * @param message the message
     *
     * @throws IOException thrown for {@link IOException}s
     */
    protected void sendAndLogMessage(String message) throws IOException {
        LOGGER.debug("Sending message: {}", message);
        sendMessage(message);
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
     * Sets {@link #customFeedMessageListener}.
     *
     * @param customFeedMessageListener the custom {@link FeedMessageListener}
     */
    public void setCustomFeedMessageListener(FeedMessageListener<String[]> customFeedMessageListener) {
        this.customFeedMessageListener = customFeedMessageListener;
    }
}
