package net.jacobpeterson.iqfeed4j.executable;

import net.jacobpeterson.iqfeed4j.properties.IQFeed4jProperties;
import net.jacobpeterson.iqfeed4j.util.split.SplitUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * {@link IQConnectExecutable} provides a convenient way to start/stop the <code>IQConnect.exe</code> program.
 */
public class IQConnectExecutable {

    private static final Logger LOGGER = LoggerFactory.getLogger(IQConnectExecutable.class);

    private static final long ADMIN_FEED_POLLING_INTERVAL = 250;

    private final String iqConnectCommand;
    private final String productID;
    private final String applicationVersion;
    private final String login;
    private final String password;
    private final Boolean autoconnect;
    private final Boolean saveLoginInfo;
    private final Object startStopLock;

    private boolean disableInternalProcessLogging;
    private Process iqConnectProcess;

    /**
     * Instantiates a new {@link IQConnectExecutable} with properties defined in {@link
     * IQFeed4jProperties#PROPERTIES_FILE}.
     */
    public IQConnectExecutable() {
        this(IQFeed4jProperties.IQCONNECT_COMMAND,
                IQFeed4jProperties.PRODUCT_ID,
                IQFeed4jProperties.APPLICATION_VERSION,
                IQFeed4jProperties.LOGIN,
                IQFeed4jProperties.PASSWORD,
                Boolean.parseBoolean(IQFeed4jProperties.AUTOCONNECT),
                Boolean.parseBoolean(IQFeed4jProperties.SAVE_LOGIN_INFO));
    }

    /**
     * Instantiates a new {@link IQConnectExecutable}.
     *
     * @param iqConnectCommand   the <code>IQConnect.exe</code> command (optional)
     * @param productID          the product ID (optional)
     * @param applicationVersion the application version (optional)
     * @param login              the login (optional)
     * @param password           the password (optional)
     * @param autoconnect        the autoconnect (optional)
     * @param saveLoginInfo      the save login info (optional)
     */
    public IQConnectExecutable(String iqConnectCommand, String productID, String applicationVersion, String login,
            String password, Boolean autoconnect, Boolean saveLoginInfo) {
        checkNotNull(iqConnectCommand);

        this.iqConnectCommand = iqConnectCommand;
        this.productID = productID;
        this.applicationVersion = applicationVersion;
        this.login = login;
        this.password = password;
        this.autoconnect = autoconnect;
        this.saveLoginInfo = saveLoginInfo;

        startStopLock = new Object();

        disableInternalProcessLogging = false;

        LOGGER.debug("{}", this);
    }

    /**
     * Starts the <code>IQConnect.exe</code> executable with the given parameters asynchronously. Does nothing if it's
     * already started. This method is synchronized with {@link #stop()}.
     *
     * @throws IOException thrown for {@link IOException}s
     */
    public void start() throws IOException {
        synchronized (startStopLock) {
            if (iqConnectProcess == null || !iqConnectProcess.isAlive()) {
                ProcessBuilder processBuilder = new ProcessBuilder();
                processBuilder.redirectErrorStream(true);

                List<String> command = new ArrayList<>();
                command.addAll(SplitUtil.splitQuoteEscapedSpaces(iqConnectCommand));

                if (productID != null) {
                    command.add("‑product");
                    command.add(productID);
                }

                if (applicationVersion != null) {
                    command.add("-version");
                    command.add(applicationVersion);
                }

                if (login != null) {
                    command.add("-login");
                    command.add(login);
                }

                if (password != null) {
                    command.add("-password");
                    command.add(password);
                }

                if (autoconnect != null && autoconnect) {
                    command.add("-autoconnect");
                }

                if (saveLoginInfo != null && saveLoginInfo) {
                    command.add("-savelogininfo");
                }

                processBuilder.command(command);

                LOGGER.debug("Starting IQConnect process with the following command: {}", command);
                iqConnectProcess = processBuilder.start();

                if (!disableInternalProcessLogging) {
                    createProcessReader();
                }
            }
        }
    }

    /**
     * Creates a process reader for {@link #iqConnectProcess} outputting to {@link #LOGGER}. Used for debugging
     * purposes. This creates a {@link Thread} that dies on its own.
     */
    private void createProcessReader() {
        new Thread(() -> {
            BufferedReader processReader = new BufferedReader(new InputStreamReader(iqConnectProcess.getInputStream()));

            while (!Thread.currentThread().isInterrupted()) {
                try {
                    String line = processReader.readLine();

                    if (line == null) {
                        processReader.close();
                        return;
                    } else {
                        LOGGER.debug("IQConnect.exe process output: {}", line);
                    }
                } catch (IOException exception) {
                    LOGGER.debug("Ignored IQConnect.exe process output reading error", exception);
                    return;
                }
            }
        }).start();
    }

    /**
     * Stops the <code>IQConnect.exe</code> executable. Does nothing if it's already stopped. This method is
     * synchronized with {@link #start()}.
     */
    public void stop() {
        synchronized (startStopLock) {
            if (iqConnectProcess != null && iqConnectProcess.isAlive()) {
                iqConnectProcess.destroy();
                LOGGER.debug("Stopped IQConnect process.");
            }
        }
    }

    /**
     * This method is used to block the current thread until <code>IQConnect.exe</code> has successfully started up. It
     * will used the passed in 'hostname' and 'port' to continually attempt to connect to <code>IQConnect.exe</code>
     * until 'timoutMillis' have elapsed or a successful connection was made.
     *
     * @param hostname     the hostname
     * @param port         the port
     * @param timoutMillis the timeout time in milliseconds
     *
     * @return the number of attempts it took to connect
     *
     * @throws TimeoutException thrown when 'timoutMillis' have elapsed without a successful connection
     */
    public int waitForConnection(String hostname, int port, long timoutMillis) throws TimeoutException {
        checkNotNull(hostname);
        checkArgument(port > 0);
        checkArgument(timoutMillis > 0);

        ExecutablePollingFeed executablePollingFeed = new ExecutablePollingFeed(hostname, port);
        int attempts = 0;
        long startTime = System.currentTimeMillis();
        while ((System.currentTimeMillis() - startTime) < timoutMillis) {
            try {
                Thread.sleep(ADMIN_FEED_POLLING_INTERVAL);
                attempts++;

                executablePollingFeed.start();
                executablePollingFeed.stop(); // This will execute upon successful 'start()'

                return attempts;
            } catch (IOException | InterruptedException ignored) {}
        }

        throw new TimeoutException(String.format("Could not establish connection after %d attempts!", attempts));
    }

    /**
     * Calls {@link #waitForConnection(String, int, long)} with {@link IQFeed4jProperties#FEED_HOSTNAME} and {@link
     * IQFeed4jProperties#ADMIN_FEED_PORT}.
     *
     * @see #waitForConnection(String, int, long)
     */
    public int waitForConnection(long timeoutMillis) throws TimeoutException {
        return waitForConnection(IQFeed4jProperties.FEED_HOSTNAME, Integer.parseInt(IQFeed4jProperties.ADMIN_FEED_PORT),
                timeoutMillis);
    }

    /**
     * Checks if {@link #getIQConnectProcess()} is not <code>null</code> and is {@link Process#isAlive() alive}.
     *
     * @return a boolean
     */
    public boolean isIQConnectRunning() {
        return iqConnectProcess != null && !iqConnectProcess.isAlive();
    }

    /**
     * Sets whether to disable internal logging of the {@link #iqConnectProcess} or not. Must be called before {@link
     * #start()}.
     *
     * @param disableInternalProcessLogging a boolean
     */
    public void disableInternalProcessLogging(boolean disableInternalProcessLogging) {
        this.disableInternalProcessLogging = disableInternalProcessLogging;
    }

    /**
     * Gets {@link #iqConnectProcess}.
     *
     * @return a {@link Process}
     */
    public Process getIQConnectProcess() {
        return iqConnectProcess;
    }

    @Override
    public String toString() {
        return "IQConnectExecutable{" +
                "iqConnectCommand='" + iqConnectCommand + '\'' +
                ", productID='" + productID + '\'' +
                ", applicationVersion='" + applicationVersion + '\'' +
                ", login='" + login + '\'' +
                ", password='" + password + '\'' +
                ", autoconnect=" + autoconnect +
                ", saveLoginInfo=" + saveLoginInfo +
                '}';
    }
}
