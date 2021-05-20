package net.jacobpeterson.iqfeed4j.feed;

import net.jacobpeterson.iqfeed4j.model.feedenums.FeedCommand;
import net.jacobpeterson.iqfeed4j.model.feedenums.FeedMessageType;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static net.jacobpeterson.iqfeed4j.util.csv.CSVUtil.valueEquals;

/**
 * {@link AbstractFeed} represents a TCP socket/feed for IQFeed.
 */
public abstract class AbstractFeed implements Runnable {

    /**
     * Protocol versions are managed by the version control branches.
     */
    public static final String CURRENTLY_SUPPORTED_PROTOCOL_VERSION = "6.1";
    /**
     * A comma (,).
     */
    public static final String COMMA_DELIMITER = ",";

    /**
     * This is an {@link ExecutorService} thread pool with the minimum number of threads being the available processors,
     * the maximum number of thread being <code>5 IQFeed feeds * 2 average simultaneous message handlers</code>, and the
     * idle thread keep alive time being 5 minutes.
     */
    protected static final ExecutorService MESSAGE_HANDLING_THREADS = new ThreadPoolExecutor(
            Runtime.getRuntime().availableProcessors(), 5 * 2,
            5, TimeUnit.MINUTES,
            new LinkedBlockingQueue<>()); // Unbounded capacity

    /**
     * Completes a {@link SingleMessageFuture} with the given 'message' asynchronously using {@link
     * #MESSAGE_HANDLING_THREADS}.
     *
     * @param <T>                 the type of the {@link SingleMessageFuture}
     * @param singleMessageFuture the {@link SingleMessageFuture}
     * @param message             the message
     */
    protected static <T> void completeAsync(SingleMessageFuture<T> singleMessageFuture, T message) {
        MESSAGE_HANDLING_THREADS.submit(() -> singleMessageFuture.complete(message));
    }

    /**
     * Exceptionally completes a {@link SingleMessageFuture} with the given {@link Throwable} asynchronously using
     * {@link #MESSAGE_HANDLING_THREADS}.
     *
     * @param <T>                 the type of the {@link SingleMessageFuture}
     * @param singleMessageFuture the {@link SingleMessageFuture}
     * @param throwable           the {@link Throwable}
     */
    protected static <T> void completeExceptionallyAsync(SingleMessageFuture<T> singleMessageFuture,
            Throwable throwable) {
        MESSAGE_HANDLING_THREADS.submit(() -> singleMessageFuture.completeExceptionally(throwable));
    }

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
    protected FeedMessageListener customFeedMessageListener;

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

            // Send protocol version command first
            String protocolVersionCommand = String.format("%s,%s,%s%s",
                    FeedMessageType.SYSTEM.value(), FeedCommand.SET_PROTOCOL.value(),
                    CURRENTLY_SUPPORTED_PROTOCOL_VERSION,
                    LineEnding.CR_LF.getASCIIString());
            LOGGER.debug("Setting protocol version: {}", protocolVersionCommand);
            sendMessage(protocolVersionCommand);

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
    private void cleanupState() {
        intentionalSocketClose = false;
        protocolVersionValidated = false;
    }

    @Override
    public void run() {
        // Confirm #start() method has released 'startStopLock' and exited synchronized scope
        synchronized (startStopLock) {}

        while (!Thread.currentThread().isInterrupted()) { // Check if thread has been closed/interrupted
            try {
                String line = feedReader.readLine();

                if (line == null) { // The socket was closed (EOF was sent)
                    closeSocket();
                    cleanupState();
                    return;
                } else {
                    // TODO remove in production
                    LOGGER.trace("Received message line: {}", line);

                    // Note that comma-separated values that have a comma will be treated like two individual
                    // comma-separated values.
                    String[] csv = line.split(COMMA_DELIMITER); // Splitting by one char doesn't use slow Regex

                    // Confirm protocol version valid
                    if (protocolVersionValidated) {
                        // Call message handlers
                        onMessageReceived(csv);
                        if (customFeedMessageListener != null) {
                            customFeedMessageListener.onMessageReceived(csv);
                        }
                    } else if (valueEquals(csv, 0, FeedMessageType.SYSTEM.value()) &&
                            valueEquals(csv, 1, FeedMessageType.CURRENT_PROTOCOL.value()) &&
                            valueEquals(csv, 2, CURRENTLY_SUPPORTED_PROTOCOL_VERSION)) {
                        LOGGER.debug("Protocol version validated: {}", (Object) csv);

                        String clientNameCommand = String.format("%s,%s,%s%s",
                                FeedMessageType.SYSTEM.value(), FeedCommand.SET_CLIENT_NAME.value(), feedName,
                                LineEnding.CR_LF.getASCIIString());
                        LOGGER.debug("Setting client name: {}", clientNameCommand);
                        sendMessage(clientNameCommand);

                        protocolVersionValidated = true;
                        onProtocolVersionValidated();
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
    protected abstract void onProtocolVersionValidated();

    /**
     * Called when a message is received. Note: This method should NEVER block and should NEVER throw an exception.
     *
     * @param csv the CSV
     */
    protected abstract void onMessageReceived(String[] csv);

    /**
     * Called when an asynchronous {@link Exception} has occurred.
     *
     * @param message   the message
     * @param exception the {@link Exception}
     */
    protected abstract void onAsyncException(String message, Exception exception);

    /**
     * Sends a message. This method is synchronized.
     *
     * @param message the message
     *
     * @throws IOException thrown for {@link IOException}s
     */
    protected synchronized void sendMessage(String message) throws IOException {
        feedWriter.write(message);
        feedWriter.flush();
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

    /**
     * Sets {@link #customFeedMessageListener}.
     *
     * @param customFeedMessageListener the custom {@link FeedMessageListener}
     */
    public void setCustomFeedMessageListener(FeedMessageListener customFeedMessageListener) {
        this.customFeedMessageListener = customFeedMessageListener;
    }
}
