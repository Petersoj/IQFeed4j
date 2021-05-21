package net.jacobpeterson.iqfeed4j.feed.lookup;

import net.jacobpeterson.iqfeed4j.feed.AbstractFeed;
import net.jacobpeterson.iqfeed4j.model.feedenums.FeedMessageType;
import net.jacobpeterson.iqfeed4j.model.feedenums.FeedSpecialMessage;

import java.util.HashSet;

import static net.jacobpeterson.iqfeed4j.util.csv.CSVUtil.valueEquals;

/**
 * {@link AbstractLookupFeed} represents the Lookup {@link AbstractFeed}. Methods in this class are not synchronized.
 */
public abstract class AbstractLookupFeed extends AbstractFeed {

    private static final String FEED_NAME_SUFFIX = " Lookup Feed";

    private final HashSet<Integer> requestIDs;

    /**
     * Instantiates a new {@link AbstractLookupFeed}.
     *
     * @param lookupFeedName the lookup feed name
     * @param hostname       the host name
     * @param port           the port
     */
    public AbstractLookupFeed(String lookupFeedName, String hostname, int port) {
        super(lookupFeedName + FEED_NAME_SUFFIX, hostname, port);

        requestIDs = new HashSet<>();
    }

    /**
     * Checks for a request ID error message format.
     * <br>
     * e.g. <code>[Request ID], E, &lt;Error Text&gt;</code>
     *
     * @param csv       the CSV
     * @param requestID the request ID
     *
     * @return true if the CSV represents an {@link FeedMessageType#ERROR} message
     */
    protected boolean isRequestErrorMessage(String[] csv, String requestID) {
        return valueEquals(csv, 0, requestID) && valueEquals(csv, 1, FeedMessageType.ERROR.value());
    }

    /**
     * Check if a message matches the following format:
     * <br>
     * <code>[Request ID], {@link FeedSpecialMessage#END_OF_MESSAGE}</code>
     *
     * @param csv       the CSV
     * @param requestID the request ID
     *
     * @return true if the message represents an {@link FeedSpecialMessage#END_OF_MESSAGE} message
     */
    public boolean isRequestEndOfMessage(String[] csv, String requestID) {
        return valueEquals(csv, 0, requestID) && valueEquals(csv, 1, FeedSpecialMessage.END_OF_MESSAGE.value());
    }

    /**
     * Check if a message matches the following format:
     * <br>
     * <code>[Request ID], {@link FeedSpecialMessage#END_OF_MESSAGE}</code>
     *
     * @param csv the CSV
     *
     * @return true if the message represents an {@link FeedSpecialMessage#END_OF_MESSAGE} message
     */
    public boolean isRequestNoDataError(String[] csv) {
        return valueEquals(csv, 1, FeedSpecialMessage.END_OF_MESSAGE.value());
    }

    /**
     * Gets a new Request ID. This method is synchronized.
     *
     * @return a new request ID
     */
    protected String getNewRequestID() {
        synchronized (requestIDs) {
            int maxRequestID = requestIDs.stream().max(Integer::compareTo).orElse(-1) + 1;
            requestIDs.add(maxRequestID);
            return String.valueOf(requestIDs);
        }
    }

    /**
     * Removes a Request ID. This method is synchronized.
     *
     * @param requestID the request ID
     */
    protected void removeRequestID(String requestID) {
        synchronized (requestIDs) {
            requestIDs.remove(Integer.parseInt(requestID));
        }
    }
}
