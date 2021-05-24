package net.jacobpeterson.iqfeed4j.feed.lookup;

import com.google.common.base.Splitter;
import net.jacobpeterson.iqfeed4j.feed.AbstractFeed;
import net.jacobpeterson.iqfeed4j.feed.MultiMessageListener;
import net.jacobpeterson.iqfeed4j.model.feedenums.FeedMessageType;
import net.jacobpeterson.iqfeed4j.model.feedenums.FeedSpecialMessage;
import net.jacobpeterson.iqfeed4j.util.csv.mapper.CSVMapper;
import net.jacobpeterson.iqfeed4j.util.exception.IQFeedException;
import net.jacobpeterson.iqfeed4j.util.exception.NoDataException;
import net.jacobpeterson.iqfeed4j.util.exception.SyntaxException;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;

import static net.jacobpeterson.iqfeed4j.util.csv.CSVUtil.valueEquals;
import static net.jacobpeterson.iqfeed4j.util.csv.CSVUtil.valuePresent;

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
     * @param csvSplitter    the CSV {@link Splitter}
     */
    public AbstractLookupFeed(String lookupFeedName, String hostname, int port, Splitter csvSplitter) {
        super(lookupFeedName + FEED_NAME_SUFFIX, hostname, port, csvSplitter);

        requestIDs = new HashSet<>();
    }

    /**
     * Handles a standard message for a {@link MultiMessageListener} by: checking for request error messages, handling
     * 'End of Message' messages, and performing {@link CSVMapper#map(String[], int)} on the 'CSV' to call {@link
     * MultiMessageListener#onMessageReceived(Object)}.
     *
     * @param <T>                   the type of {@link MultiMessageListener}
     * @param csv                   the CSV
     * @param requestID             the Request ID
     * @param listenersOfRequestIDs the {@link Map} with the keys being the Request IDs and the values being the
     *                              corresponding {@link MultiMessageListener}s
     * @param csvMapper             the {@link CSVMapper} for the message
     *
     * @return true if the 'requestID' was a key inside 'listenersOfRequestIDs', false otherwise
     */
    protected <T> boolean handleStandardMultiMessage(String[] csv, String requestID,
            Map<String, MultiMessageListener<T>> listenersOfRequestIDs, CSVMapper<T> csvMapper) {
        MultiMessageListener<T> listener = listenersOfRequestIDs.get(requestID);

        if (listener == null) {
            return false;
        }

        if (isRequestErrorMessage(csv, requestID)) {
            if (isRequestNoDataError(csv)) {
                listener.onMessageException(new NoDataException());
            } else if (isRequestSyntaxError(csv)) {
                listener.onMessageException(new SyntaxException());
            } else {
                listener.onMessageException(new IQFeedException(
                        valuePresent(csv, 2) ?
                                String.join(",", Arrays.copyOfRange(csv, 2, csv.length)) :
                                "Error message not present."));
            }
        } else if (isRequestEndOfMessage(csv, requestID)) {
            listenersOfRequestIDs.remove(requestID);
            removeRequestID(requestID);
            listener.onEndOfMultiMessage();
        } else {
            try {
                T message = csvMapper.map(csv, 1);
                listener.onMessageReceived(message);
            } catch (Exception exception) {
                listener.onMessageException(exception);
            }
        }

        return true;
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
     * <code>[Request ID], {@link FeedSpecialMessage#NO_DATA_ERROR}</code>
     *
     * @param csv the CSV
     *
     * @return true if the message represents a {@link FeedSpecialMessage#NO_DATA_ERROR} message
     */
    public boolean isRequestNoDataError(String[] csv) {
        return valueEquals(csv, 1, FeedSpecialMessage.NO_DATA_ERROR.value());
    }

    /**
     * Check if a message matches the following format:
     * <br>
     * <code>[Request ID], {@link FeedSpecialMessage#SYNTAX_ERROR}</code>
     *
     * @param csv the CSV
     *
     * @return true if the message represents a {@link FeedSpecialMessage#SYNTAX_ERROR} message
     */
    public boolean isRequestSyntaxError(String[] csv) {
        return valueEquals(csv, 1, FeedSpecialMessage.SYNTAX_ERROR.value());
    }

    /**
     * Gets a new Request ID. This method is synchronized.
     *
     * @return a new request ID
     */
    protected String getNewRequestID() {
        synchronized (requestIDs) {
            int maxRequestID = requestIDs.stream().max(Integer::compareTo).orElse(0) + 1;
            requestIDs.add(maxRequestID);
            return String.valueOf(maxRequestID);
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
