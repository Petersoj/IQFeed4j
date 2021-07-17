package net.jacobpeterson.iqfeed4j.executable;

import net.jacobpeterson.iqfeed4j.feed.AbstractFeed;

/**
 * {@link ExecutablePollingFeed} is an {@link AbstractFeed} used exclusively for {@link
 * IQConnectExecutable#waitForConnection(String, int, int, long)}.
 */
class ExecutablePollingFeed extends AbstractFeed {

    /**
     * Instantiates a new {@link ExecutablePollingFeed}.
     *
     * @param hostname the hostname
     * @param port     the port
     */
    public ExecutablePollingFeed(String hostname, int port) {
        super("IQFeed4j Polling Feed", hostname, port, COMMA_DELIMITED_SPLITTER, false, false);
    }

    @Override
    protected void onMessageReceived(String[] csv) {}
}
