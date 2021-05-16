package net.jacobpeterson.iqfeed4j.feed;

import net.jacobpeterson.iqfeed4j.model.feedenums.FeedCommand;
import net.jacobpeterson.iqfeed4j.model.feedenums.FeedMessageType;
import net.jacobpeterson.iqfeed4j.util.exception.AsyncExceptionListener;
import net.jacobpeterson.iqfeed4j.util.string.LineEnding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static net.jacobpeterson.iqfeed4j.util.csv.CSVUtil.valueEquals;

/**
 * {@link AbstractFeed} represents a TCP socket/feed for IQFeed.
 */
public abstract class AbstractFeed implements Runnable, AsyncExceptionListener {

    /**
     * Protocol versions are managed by the version control branches.
     */
    public static final String CURRENTLY_SUPPORTED_PROTOCOL_VERSION = "6.1";

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractFeed.class);
    private static final int SOCKET_THREAD_JOIN_WAIT_MILLIS = 5000;

    protected final String feedName;
    protected final String hostname;
    protected final int port;
    private final Object startStopLock;

    private Thread socketThread;
    private Socket feedSocket;
    private BufferedWriter feedWriter;
    private BufferedReader feedReader;
    private boolean intentionalSocketClose;
    private boolean protocolVersionValidated;

    /**
     * Instantiates a new {@link AbstractFeed}.
     *
     * @param feedName the feed name
     * @param hostname the host name
     * @param port     the port
     */
    public AbstractFeed(String feedName, String hostname, int port) {
        this.feedName = feedName;
        this.hostname = hostname;
        this.port = port;

        startStopLock = new Object();

        intentionalSocketClose = false;
    }

    /**
     * Starts this feed connection (starts a new socket, new thread, and send protocol message). This method is
     * synchronized with {@link #stop()}.
     *
     * @throws Exception thrown for {@link Exception}s
     */
    public void start() throws Exception {
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

            socketThread = new Thread(this);
            socketThread.start();
        }
    }

    /**
     * Stop this feed connection (close socket and interrupt thread). This method is synchronized with {@link
     * #start()}.
     *
     * @throws Exception thrown for {@link Exception}s
     */
    public void stop() throws Exception {
        synchronized (startStopLock) {
            closeSocket();
            interruptAndJoinThread();
            cleanupState();
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
    private void cleanupState() {
        intentionalSocketClose = false;
        protocolVersionValidated = false;
    }

    @Override
    public void run() {
        // Send and check protocol version messages
        try {
            sendSetProtocolMessage();
            String[] protocolCSV = feedReader.readLine().split(",");

            if (valueEquals(protocolCSV, 0, FeedMessageType.SYSTEM.value()) &&
                    valueEquals(protocolCSV, 1, FeedMessageType.CURRENT_PROTOCOL.value()) &&
                    valueEquals(protocolCSV, 2, CURRENTLY_SUPPORTED_PROTOCOL_VERSION)) {
                LOGGER.debug("Protocol version validated: {}", (Object) protocolCSV);

                sendSetClientNameMessage();

                protocolVersionValidated = true;
                onProtocolVersionValidated();
            } else {
                throw new RuntimeException("Protocol version not validated! Message: " + Arrays.toString(protocolCSV));
            }
        } catch (Exception exception) {
            onAsyncException("Could not set up feed!", exception);
        }

        while (!Thread.currentThread().isInterrupted()) { // Check if thread has been closed/interrupted
            try {
                String line = feedReader.readLine();

                if (line == null) { // The socket was closed (EOF was sent)
                    closeSocket();
                    cleanupState();
                    return;
                } else {
                    // Notes that no comma-separated value should have a comma in it, otherwise
                    // it will be treated like two individual comma-separated values.
                    onMessageReceived(line.split(",")); // Splitting by one char doesn't use Regex
                }
            } catch (Exception exception) {
                if (!intentionalSocketClose) {
                    onAsyncException("Could not read and process feed socket message line!", exception);
                }
            }
        }
    }

    /**
     * Checks for error message format.
     * <br>
     * e.g. <code>[Request ID], E, [Error Text]</code> or <code>E, [Error Text]</code>
     * <br>
     * If the 'Request ID' is the char literal 'E', then this will always return true unfortunately (this is a flaw with
     * the IQFeed API)
     *
     * @param csv the CSV
     *
     * @return true if the 'csv' represents an error message
     */
    public boolean isErrorMessage(String[] csv) {
        return valueEquals(csv, 0, FeedMessageType.ERROR.value()) ||
                valueEquals(csv, 1, FeedMessageType.ERROR.value());
    }

    /**
     * Called when the protocol version has been validated.
     */
    protected abstract void onProtocolVersionValidated();

    /**
     * Called when a message is received. This method should not block!
     *
     * @param csv the CSV
     */
    protected abstract void onMessageReceived(String[] csv);

    /**
     * Sends a message line. This method is synchronized with this object instance.
     *
     * @param message    the message
     * @param lineEnding the {@link LineEnding} for the message
     *
     * @throws IOException thrown for {@link IOException}s
     */
    protected synchronized void sendMessageLine(String message, LineEnding lineEnding) throws IOException {
        feedWriter.write(message);
        feedWriter.write(lineEnding.getASCIIString());
        feedWriter.flush();
    }

    /**
     * Sends the {@link FeedCommand#SET_PROTOCOL} message.
     *
     * @throws IOException thrown for {@link IOException}s
     */
    protected void sendSetProtocolMessage() throws IOException {
        sendMessageLine(String.format("%s,%s,%s",
                FeedMessageType.SYSTEM.value(), FeedCommand.SET_PROTOCOL.value(), CURRENTLY_SUPPORTED_PROTOCOL_VERSION),
                LineEnding.CR_LF);
    }

    /**
     * Sends the {@link FeedCommand#SET_CLIENT_NAME} message.
     *
     * @throws IOException thrown for {@link IOException}s
     */
    protected void sendSetClientNameMessage() throws IOException {
        sendMessageLine(String.format("%s,%s,%s",
                FeedMessageType.SYSTEM.value(), FeedCommand.SET_CLIENT_NAME.value(), feedName),
                LineEnding.CR_LF);
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
        return protocolVersionValidated;
    }
}
