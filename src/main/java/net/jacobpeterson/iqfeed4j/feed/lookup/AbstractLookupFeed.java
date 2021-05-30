package net.jacobpeterson.iqfeed4j.feed.lookup;

import com.google.common.base.Splitter;
import net.jacobpeterson.iqfeed4j.feed.AbstractFeed;
import net.jacobpeterson.iqfeed4j.feed.RequestIDFeedHelper;
import net.jacobpeterson.iqfeed4j.feed.message.MultiMessageListener;
import net.jacobpeterson.iqfeed4j.model.feed.common.enums.FeedMessageType;
import net.jacobpeterson.iqfeed4j.model.feed.common.enums.FeedSpecialMessage;
import net.jacobpeterson.iqfeed4j.util.csv.mapper.CSVMapper;
import net.jacobpeterson.iqfeed4j.util.exception.IQFeedException;
import net.jacobpeterson.iqfeed4j.util.exception.NoDataException;
import net.jacobpeterson.iqfeed4j.util.exception.SyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Map;

import static net.jacobpeterson.iqfeed4j.util.csv.CSVUtil.*;

/**
 * {@link AbstractLookupFeed} represents a {@link AbstractFeed} for Lookup data.
 */
public abstract class AbstractLookupFeed extends AbstractFeed {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractLookupFeed.class);
    private static final String FEED_NAME_SUFFIX = " Lookup Feed";

    protected final RequestIDFeedHelper requestIDFeedHelper;

    /**
     * Instantiates a new {@link AbstractLookupFeed}.
     *
     * @param lookupFeedName the lookup feed name
     * @param hostname       the host name
     * @param port           the port
     * @param csvSplitter    the CSV {@link Splitter}
     */
    public AbstractLookupFeed(String lookupFeedName, String hostname, int port, Splitter csvSplitter) {
        super(lookupFeedName + FEED_NAME_SUFFIX, hostname, port, csvSplitter, true, true);

        requestIDFeedHelper = new RequestIDFeedHelper();
    }

    /**
     * Handles a standard message for a {@link MultiMessageListener} by: checking for request error messages, handling
     * {@link FeedSpecialMessage#END_OF_MESSAGE} messages, and performing {@link CSVMapper#map(String[], int)} on the
     * 'CSV' to call {@link MultiMessageListener#onMessageReceived(Object)}.
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

        if (requestIDFeedHelper.isRequestErrorMessage(csv, requestID)) {
            if (requestIDFeedHelper.isRequestNoDataError(csv)) {
                listener.onMessageException(new NoDataException());
            } else if (requestIDFeedHelper.isRequestSyntaxError(csv)) {
                listener.onMessageException(new SyntaxException());
            } else {
                listener.onMessageException(new IQFeedException(
                        valuePresent(csv, 2) ?
                                String.join(",", Arrays.copyOfRange(csv, 2, csv.length)) :
                                "Error message not present."));
            }
        } else if (requestIDFeedHelper.isRequestEndOfMessage(csv, requestID)) {
            listenersOfRequestIDs.remove(requestID);
            requestIDFeedHelper.removeRequestID(requestID);
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
     * Checks if a message is a {@link FeedMessageType#ERROR} message or that the first CSV value is whitespace to check
     * if a Request ID is present.
     *
     * @param csv the CSV
     *
     * @return true if the message is an error or is invalid
     */
    protected boolean isErrorOrInvalidMessage(String[] csv) {
        if (valueEquals(csv, 0, FeedMessageType.ERROR.value())) {
            LOGGER.error("Received error message! {}", (Object) csv);
            return true;
        }

        // All messages sent on this feed must have a numeric Request ID first
        if (!valueNotWhitespace(csv, 0)) {
            LOGGER.error("Received unknown message format: {}", (Object) csv);
            return true;
        }

        return false;
    }
}
