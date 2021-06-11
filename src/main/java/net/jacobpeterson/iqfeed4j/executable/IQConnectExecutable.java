package net.jacobpeterson.iqfeed4j.executable;

import net.jacobpeterson.iqfeed4j.properties.IQFeed4jProperties;
import net.jacobpeterson.iqfeed4j.util.split.SplitUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * {@link IQConnectExecutable} provides a convenient way to start/stop the IQConnect.exe program.
 */
public class IQConnectExecutable {

    private static final Logger LOGGER = LoggerFactory.getLogger(IQConnectExecutable.class);

    private final String iqConnectCommand;
    private final String productID;
    private final String applicationVersion;
    private final String login;
    private final String password;
    private final Boolean autoconnect;
    private final Boolean saveLoginInfo;
    private final Object startStopLock;

    private Process iqConnectProcess;
    private OutputStream processOutputStream;

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
     * @param iqConnectCommand   the IQConnect.exe command (optional)
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

        LOGGER.debug("{}", this);
    }

    /**
     * Starts the IQConnect executable with the given parameters. Does nothing if it's already started. This method is
     * synchronized with {@link #stop()}.
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

                createProcessReader();
            }
        }
    }

    /**
     * Creates a process reader for {@link #iqConnectProcess} outputting to either {@link #processOutputStream} or
     * {@link #LOGGER}. Used for debugging purposes. This creates a {@link Thread} that dies on its own.
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
                    } else if (processOutputStream != null) {
                        processOutputStream.write(line.getBytes(StandardCharsets.UTF_8));
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
     * Stops the IQConnect executable. Does nothing if it's already stopped. This method is synchronized with {@link
     * #start()}.
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
     * Gets {@link #processOutputStream}.
     *
     * @return the process {@link OutputStream}
     */
    public OutputStream getProcessOutputStream() {
        return processOutputStream;
    }

    /**
     * Sets {@link #processOutputStream}.
     *
     * @param processOutputStream the process {@link OutputStream}
     */
    public void setProcessOutputStream(OutputStream processOutputStream) {
        this.processOutputStream = processOutputStream;
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
