package net.jacobpeterson.iqfeed4j.feed.abstracts;

import net.jacobpeterson.iqfeed4j.util.csv.CSVUtil;
import net.jacobpeterson.iqfeed4j.util.exception.AsyncExceptionListener;
import net.jacobpeterson.iqfeed4j.util.string.LineEnding;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * {@link AbstractFeed} represents a TCP socket/feed for IQFeed.
 */
public abstract class AbstractFeed implements Runnable, AsyncExceptionListener {

    private static final int SOCKET_THREAD_JOIN_WAIT_MILLIS = 5000;
    private static final String ERROR_MESSAGE_IDENTIFIER = "E";

    protected final String feedName;
    protected final String host;
    protected final int port;
    protected final String protocolVersion;
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
     * @param feedName        the feed name
     * @param host            the host
     * @param port            the port
     * @param protocolVersion the protocol version
     */
    public AbstractFeed(String feedName, String host, int port, String protocolVersion) {
        this.feedName = feedName;
        this.host = host;
        this.port = port;
        this.protocolVersion = protocolVersion;

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

            feedSocket = new Socket(host, port);
            feedWriter = new BufferedWriter(new OutputStreamWriter(feedSocket.getOutputStream(),
                    StandardCharsets.US_ASCII));
            feedReader = new BufferedReader(new InputStreamReader(feedSocket.getInputStream(),
                    StandardCharsets.US_ASCII));

            socketThread = new Thread(this);
            socketThread.start();

            sendSetProtocolMessage();
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
                    String[] csv = line.split(",");

                    checkProtocolMessage(csv);
                    onMessageReceived(csv);
                }
            } catch (Exception exception) {
                if (!intentionalSocketClose) {
                    onAsyncException("Could not read and process feed socket line!", exception);
                }
            }
        }
    }

    /**
     * Check for "CURRENT PROTOCOL" message response.
     *
     * @param csv the CSV
     *
     * @throws IOException thrown for {@link IOException}s
     */
    private void checkProtocolMessage(String[] csv) throws IOException {
        if (!protocolVersionValidated &&
                CSVUtil.valueEquals(csv, 0, "S") &&
                CSVUtil.valueEquals(csv, 1, "CURRENT PROTOCOL") &&
                CSVUtil.valueEquals(csv, 2, protocolVersion)) {
            protocolVersionValidated = true;
            onProtocolVersionValidated();
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
        return CSVUtil.valueEquals(csv, 0, ERROR_MESSAGE_IDENTIFIER) ||
                CSVUtil.valueEquals(csv, 1, ERROR_MESSAGE_IDENTIFIER);
    }

    /**
     * Called when a message is received. This method should not block!
     *
     * @param csv the CSV
     */
    protected abstract void onMessageReceived(String[] csv);

    /**
     * Called when the protocol version has been validated.
     * <br>
     * If this method is overridden, be sure to call the super method!
     *
     * @throws IOException thrown for {@link IOException}s
     */
    protected void onProtocolVersionValidated() throws IOException {
        sendSetClientNameMessage();
    }

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
     * Sends the "SET PROTOCOL" message.
     *
     * @throws IOException thrown for {@link IOException}s
     */
    protected void sendSetProtocolMessage() throws IOException {
        sendMessageLine("S,SET PROTOCOL," + protocolVersion, LineEnding.CR_LF);
    }

    /**
     * Sends the "SET CLIENT NAME" message.
     *
     * @throws IOException thrown for {@link IOException}s
     */
    protected void sendSetClientNameMessage() throws IOException {
        sendMessageLine("S,SET CLIENT NAME," + feedName, LineEnding.CR_LF);
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
     * Gets {@link #protocolVersion}.
     *
     * @return a {@link String}
     */
    public String getProtocolVersion() {
        return protocolVersion;
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
