package net.jacobpeterson.iqfeed4j.executable;

import net.jacobpeterson.iqfeed4j.feed.AbstractFeed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link ExecutablePollingFeed} is an {@link AbstractFeed} used exclusively for {@link
 * IQConnectExecutable#waitForConnection(String, int, int, long)}.
 */
class ExecutablePollingFeed extends AbstractFeed {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExecutablePollingFeed.class);

    /**
     * Instantiates a new {@link ExecutablePollingFeed}.
     *
     * @param hostname the hostname
     * @param port     the port
     */
    public ExecutablePollingFeed(String hostname, int port) {
        super(LOGGER, "IQFeed4j Polling Feed", hostname, port, COMMA_DELIMITED_SPLITTER, false, false);
    }

    @Override
    protected void onMessageReceived(String[] csv) {}

    @Override
    protected void onFeedSocketException(Exception exception) {}

    @Override
    protected void onFeedSocketClose() {}
}
