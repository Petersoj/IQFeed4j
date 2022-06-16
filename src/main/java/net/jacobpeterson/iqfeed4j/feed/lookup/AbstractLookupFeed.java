package net.jacobpeterson.iqfeed4j.feed.lookup;

import com.google.common.base.Splitter;
import net.jacobpeterson.iqfeed4j.feed.AbstractFeed;
import net.jacobpeterson.iqfeed4j.feed.RequestIDFeedHelper;
import net.jacobpeterson.iqfeed4j.feed.exception.IQFeedRuntimeException;
import net.jacobpeterson.iqfeed4j.feed.exception.NoDataException;
import net.jacobpeterson.iqfeed4j.feed.exception.SyntaxException;
import net.jacobpeterson.iqfeed4j.feed.message.MultiMessageListener;
import net.jacobpeterson.iqfeed4j.model.feed.common.enums.FeedMessageType;
import net.jacobpeterson.iqfeed4j.model.feed.common.enums.FeedSpecialMessage;
import net.jacobpeterson.iqfeed4j.util.csv.mapper.index.AbstractIndexCSVMapper;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.Map;

import static net.jacobpeterson.iqfeed4j.util.csv.CSVUtil.valueEquals;
import static net.jacobpeterson.iqfeed4j.util.csv.CSVUtil.valueNotWhitespace;
import static net.jacobpeterson.iqfeed4j.util.csv.CSVUtil.valuePresent;

/**
 * {@link AbstractLookupFeed} represents a {@link AbstractFeed} for Lookup data.
 */
public abstract class AbstractLookupFeed extends AbstractFeed {

    private static final String FEED_NAME_SUFFIX = " Lookup Feed";

    protected final RequestIDFeedHelper requestIDFeedHelper;

    /**
     * Instantiates a new {@link AbstractLookupFeed}.
     *
     * @param logger         the {@link Logger}
     * @param lookupFeedName the lookup feed name
     * @param hostname       the host name
     * @param port           the port
     * @param csvSplitter    the CSV {@link Splitter}
     */
    public AbstractLookupFeed(Logger logger, String lookupFeedName, String hostname, int port, Splitter csvSplitter) {
        super(logger, lookupFeedName + FEED_NAME_SUFFIX, hostname, port, csvSplitter, true, true);

        requestIDFeedHelper = new RequestIDFeedHelper();
    }

    /**
     * Handles a standard message for a {@link MultiMessageListener} by: checking for request error messages, handling
     * {@link FeedSpecialMessage#END_OF_MESSAGE} messages, and performing
     * {@link AbstractIndexCSVMapper#map(String[], int)} on the <code>csv</code> to call
     * {@link MultiMessageListener#onMessageReceived(Object)}.
     *
     * @param <T>                   the type of {@link MultiMessageListener}
     * @param csv                   the CSV
     * @param requestID             the Request ID
     * @param offset                the offset to add to CSV indices
     * @param listenersOfRequestIDs the {@link Map} with the keys being the Request IDs and the values being the
     *                              corresponding {@link MultiMessageListener}s
     * @param indexCSVMapper        the {@link AbstractIndexCSVMapper} for the message
     *
     * @return true if the <code>requestID</code> was a key inside <code>listenersOfRequestIDs</code>, false otherwise
     */
    protected <T> boolean handleStandardMultiMessage(String[] csv, String requestID, int offset,
            Map<String, MultiMessageListener<T>> listenersOfRequestIDs, AbstractIndexCSVMapper<T> indexCSVMapper) {
        MultiMessageListener<T> listener = listenersOfRequestIDs.get(requestID);

        if (listener == null) {
            return false;
        }

        if (requestIDFeedHelper.isRequestErrorMessage(csv, requestID)) {
            if (requestIDFeedHelper.isRequestNoDataError(csv)) {
                listener.onMessageException(new NoDataException());
            } else if (requestIDFeedHelper.isRequestSyntaxError(csv)) {
                listener.onMessageException(new SyntaxException());
            } else {
                listener.onMessageException(new IQFeedRuntimeException(
                        valuePresent(csv, 2) ?
                                String.join(",", Arrays.copyOfRange(csv, 2, csv.length)) :
                                "Error message not present."));
            }
        } else if (requestIDFeedHelper.isRequestEndOfMessage(csv, requestID)) {
            listenersOfRequestIDs.remove(requestID);
            requestIDFeedHelper.removeRequestID(requestID);
            listener.handleEndOfMultiMessage();
        } else {
            try {
                T message = indexCSVMapper.map(csv, offset);
                listener.onMessageReceived(message);
            } catch (Exception exception) {
                listener.onMessageException(exception);
            }
        }

        return true;
    }

    /**
     * Checks if a message is a {@link FeedMessageType#ERROR} message or that the first CSV value is whitespace to check
     * if a Request ID is present.
     *
     * @param csv the CSV
     *
     * @return true if the message is an error or is invalid
     */
    protected boolean isErrorOrInvalidMessage(String[] csv) {
        if (valueEquals(csv, 0, FeedMessageType.ERROR.value())) {
            logger.error("Received error message! {}", (Object) csv);
            return true;
        }

        // Messages sent on this feed have a numeric Request ID first
        if (!valueNotWhitespace(csv, 0)) {
            logger.error("Received unknown message format: {}", (Object) csv);
            return true;
        }

        return false;
    }
}
