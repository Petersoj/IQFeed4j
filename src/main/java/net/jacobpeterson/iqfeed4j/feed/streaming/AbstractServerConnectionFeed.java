package net.jacobpeterson.iqfeed4j.feed.streaming;

import com.google.common.base.Splitter;
import net.jacobpeterson.iqfeed4j.feed.AbstractFeed;
import net.jacobpeterson.iqfeed4j.model.feed.common.enums.FeedMessageType;
import net.jacobpeterson.iqfeed4j.model.feed.streaming.common.enums.ServerConnectionStatus;
import org.slf4j.Logger;

/**
 * {@link AbstractServerConnectionFeed} is an {@link AbstractFeed} for feeds that receive the <code>S,SERVER
 * CONNECTED&gt;LF&lt;</code> or <code>S,SERVER DISCONNECTED&gt;LF&lt;</code> messages.
 */
public abstract class AbstractServerConnectionFeed extends AbstractFeed {

    protected ServerConnectionStatus serverConnectionStatus;

    /**
     * Instantiates a new {@link AbstractServerConnectionFeed}.
     *
     * @param logger                  the {@link Logger}
     * @param feedName                the feed name
     * @param hostname                the hostname
     * @param port                    the port
     * @param csvSplitter             the CSV {@link Splitter}
     * @param validateProtocolVersion true to send and validate the {@link #CURRENTLY_SUPPORTED_PROTOCOL_VERSION}
     * @param sendClientName          true to send the client <code>feedName</code>
     */
    public AbstractServerConnectionFeed(Logger logger, String feedName, String hostname, int port, Splitter csvSplitter,
            boolean validateProtocolVersion, boolean sendClientName) {
        super(logger, feedName, hostname, port, csvSplitter, validateProtocolVersion, sendClientName);
    }

    /**
     * Checks and sets {@link #serverConnectionStatus} if a {@link ServerConnectionStatus} is present. Note this will
     * not check if the CSV message is a {@link FeedMessageType#SYSTEM} message so that should be done before calling
     * this method.
     *
     * @param systemMessageType the {@link FeedMessageType#SYSTEM} message type
     *
     * @return true if the message was a {@link ServerConnectionStatus} message, false otherwise
     */
    protected boolean checkServerConnectionStatusMessage(String systemMessageType) {
        try {
            serverConnectionStatus = ServerConnectionStatus.fromValue(systemMessageType);

            if (serverConnectionStatus == ServerConnectionStatus.SERVER_CONNECTED) {
                logger.info("Server is connected.");
            } else {
                logger.warn("Server is disconnected!");
            }

            return true;
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    /**
     * Gets {@link #serverConnectionStatus}.
     *
     * @return the {@link ServerConnectionStatus}
     */
    public ServerConnectionStatus getServerConnectionStatus() {
        return serverConnectionStatus;
    }
}
