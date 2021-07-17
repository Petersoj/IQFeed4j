package net.jacobpeterson.iqfeed4j.feed;

import net.jacobpeterson.iqfeed4j.feed.lookup.AbstractLookupFeed;
import net.jacobpeterson.iqfeed4j.model.feed.common.enums.FeedMessageType;
import net.jacobpeterson.iqfeed4j.model.feed.common.enums.FeedSpecialMessage;

import java.util.HashSet;

import static net.jacobpeterson.iqfeed4j.util.csv.CSVUtil.valueEquals;

/**
 * {@link RequestIDFeedHelper} is a helper for an {@link AbstractFeed} that uses Request IDs.
 */
public class RequestIDFeedHelper {

    private final HashSet<Integer> requestIDs;

    /**
     * Instantiates a new {@link AbstractLookupFeed}.
     */
    public RequestIDFeedHelper() {
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
    public boolean isRequestErrorMessage(String[] csv, String requestID) {
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
     * <code>[Request ID], E, {@link FeedSpecialMessage#NO_DATA_ERROR}</code>
     *
     * @param csv the CSV
     *
     * @return true if the message represents a {@link FeedSpecialMessage#NO_DATA_ERROR} message
     */
    public boolean isRequestNoDataError(String[] csv) {
        return valueEquals(csv, 2, FeedSpecialMessage.NO_DATA_ERROR.value());
    }

    /**
     * Check if a message matches the following format:
     * <br>
     * <code>[Request ID], E, {@link FeedSpecialMessage#SYNTAX_ERROR}</code>
     *
     * @param csv the CSV
     *
     * @return true if the message represents a {@link FeedSpecialMessage#SYNTAX_ERROR} message
     */
    public boolean isRequestSyntaxError(String[] csv) {
        return valueEquals(csv, 2, FeedSpecialMessage.SYNTAX_ERROR.value());
    }

    /**
     * Gets a new Request ID. This method is synchronized with {@link #removeRequestID(String)}.
     *
     * @return a new request ID
     */
    public String getNewRequestID() {
        synchronized (requestIDs) {
            int maxRequestID = requestIDs.stream().max(Integer::compareTo).orElse(0) + 1;
            requestIDs.add(maxRequestID);
            return String.valueOf(maxRequestID);
        }
    }

    /**
     * Removes a Request ID. This method is synchronized with {@link #getNewRequestID()}.
     *
     * @param requestID the request ID
     */
    public void removeRequestID(String requestID) {
        synchronized (requestIDs) {
            requestIDs.remove(Integer.parseInt(requestID));
        }
    }
}
