package net.jacobpeterson.iqfeed4j.feed.streaming;

import com.google.common.base.Splitter;
import net.jacobpeterson.iqfeed4j.feed.AbstractFeed;
import net.jacobpeterson.iqfeed4j.model.feedenums.FeedMessageType;
import net.jacobpeterson.iqfeed4j.model.feedenums.streaming.ServerConnectionStatus;

import static net.jacobpeterson.iqfeed4j.util.csv.CSVUtil.valueEquals;
import static net.jacobpeterson.iqfeed4j.util.csv.CSVUtil.valuePresent;

/**
 * {@link AbstractServerConnectionFeed} is an {@link AbstractFeed} for feeds that receive the <code>S,SERVER
 * CONNECTED&gt;LF&lt;</code> or <code>S,SERVER DISCONNECTED&gt;LF&lt;</code> messages.
 */
public abstract class AbstractServerConnectionFeed extends AbstractFeed {

    protected ServerConnectionStatus serverConnectionStatus;

    /**
     * Instantiates a new {@link AbstractServerConnectionFeed}.
     *
     * @param feedName    the feed name
     * @param hostname    the hostname
     * @param port        the port
     * @param csvSplitter the CSV {@link Splitter}
     */
    public AbstractServerConnectionFeed(String feedName, String hostname, int port, Splitter csvSplitter) {
        super(feedName, hostname, port, csvSplitter);
    }

    /**
     * {@inheritDoc}
     * <br>
     * Note that this will check/set {@link #serverConnectionStatus}.
     */
    @Override
    protected void onMessageReceived(String[] csv) {
        if (serverConnectionStatus == null) {
            serverConnectionStatus = getServerConnectionStatusMessage(csv);
        }
    }

    /**
     * Checks if a CSV message is a {@link FeedMessageType#SYSTEM} message and returns a {@link ServerConnectionStatus}
     * if it is present.
     *
     * @param csv the CSV
     *
     * @return the {@link ServerConnectionStatus} or null if it's not present
     */
    protected ServerConnectionStatus getServerConnectionStatusMessage(String[] csv) {
        if (valueEquals(csv, 0, FeedMessageType.SYSTEM.value()) && valuePresent(csv, 1)) {
            try {
                return ServerConnectionStatus.fromValue(csv[1]);
            } catch (Exception ignored) {
                return null;
            }
        }

        return null;
    }
}
