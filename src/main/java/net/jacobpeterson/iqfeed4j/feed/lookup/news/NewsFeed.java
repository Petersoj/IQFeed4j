package net.jacobpeterson.iqfeed4j.feed.lookup.news;

import com.google.common.base.Preconditions;
import net.jacobpeterson.iqfeed4j.feed.MultiMessageListener;
import net.jacobpeterson.iqfeed4j.feed.lookup.AbstractLookupFeed;
import net.jacobpeterson.iqfeed4j.model.feedenums.lookup.news.XMLTextEmailOption;
import net.jacobpeterson.iqfeed4j.model.feedenums.lookup.news.XMLTextOption;
import net.jacobpeterson.iqfeed4j.model.util.MessageLine;
import net.jacobpeterson.iqfeed4j.util.csv.mapper.CSVMapper;
import net.jacobpeterson.iqfeed4j.util.csv.mapper.TrailingCSVMapper;
import net.jacobpeterson.iqfeed4j.util.string.LineEnding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.*;
import static net.jacobpeterson.iqfeed4j.util.csv.mapper.CSVMapper.DateTimeFormatters.DATE;

/**
 * {@link NewsFeed} is an {@link AbstractLookupFeed} for news data.
 */
public class NewsFeed extends AbstractLookupFeed {

    private static final Logger LOGGER = LoggerFactory.getLogger(NewsFeed.class);
    protected static final String FEED_NAME_SUFFIX = " News";

    protected static final TrailingCSVMapper<MessageLine> MESSAGE_LINE_CSV_MAPPER;

    static {
        MESSAGE_LINE_CSV_MAPPER = new TrailingCSVMapper<>(MessageLine::new);
        MESSAGE_LINE_CSV_MAPPER.setTrailingMapping(MessageLine::setLine, CSVMapper.PrimitiveConvertors.STRING);
    }

    protected final Object messageReceivedLock;
    protected final HashMap<String, MultiMessageListener<MessageLine>> messageLineListenersOfRequestIDs;

    /**
     * Instantiates a new {@link NewsFeed}.
     *
     * @param newsFeedName the {@link NewsFeed} name
     * @param hostname     the hostname
     * @param port         the port
     */
    public NewsFeed(String newsFeedName, String hostname, int port) {
        super(newsFeedName + FEED_NAME_SUFFIX, hostname, port, COMMA_DELIMITED_SPLITTER);

        messageReceivedLock = new Object();
        messageLineListenersOfRequestIDs = new HashMap<>();
    }

    @Override
    protected void onMessageReceived(String[] csv) {
        if (isErrorOrInvalidMessage(csv)) {
            return;
        }

        String requestID = csv[0];

        synchronized (messageReceivedLock) {
            handleStandardMultiMessage(csv, requestID, messageLineListenersOfRequestIDs, MESSAGE_LINE_CSV_MAPPER);
        }
    }

    //
    // START Feed commands
    //

    /**
     * This requests the current News configuration. This sends an NCG request. This method is thread-safe.
     *
     * @param xmlTextOption       the {@link XMLTextOption}. See
     *                            <a href="https://www.iqfeed.net/dev/api/docs//NewsLookupviaTCPIP.cfm">News
     *                            Lookup via TCP/IP</a> for the response formats.
     * @param messageLineListener the {@link MultiMessageListener} for the message {@link MessageLine}s
     *
     * @throws IOException thrown for {@link IOException}s
     */
    public void requestNewsConfiguration(XMLTextOption xmlTextOption,
            MultiMessageListener<MessageLine> messageLineListener) throws IOException {
        checkNotNull(xmlTextOption);
        checkNotNull(messageLineListener);

        String requestID = requestIDFeedHelper.getNewRequestID();
        StringBuilder requestBuilder = new StringBuilder();

        requestBuilder.append("NCG").append(",");
        requestBuilder.append(xmlTextOption.value()).append(",");
        requestBuilder.append(requestID);
        requestBuilder.append(LineEnding.CR_LF.getASCIIString());

        synchronized (messageReceivedLock) {
            messageLineListenersOfRequestIDs.put(requestID, messageLineListener);
        }
        String requestString = requestBuilder.toString();
        LOGGER.debug("Sending request: {}", requestString);
        sendMessage(requestString);
    }

    /**
     * This requests News headlines. This sends an NHL request. This method is thread-safe.
     *
     * @param sources             a {@link List} of news sources which can be retrieved via {@link
     *                            #requestNewsConfiguration(XMLTextOption, MultiMessageListener)}. (optional)
     * @param symbols             a {@link List} of symbols for which to receive headlines (optional)
     * @param xmlTextOption       the {@link XMLTextOption}. See
     *                            <a href="https://www.iqfeed.net/dev/api/docs//NewsLookupviaTCPIP.cfm">News
     *                            Lookup via TCP/IP</a> for the response formats.
     * @param limit               the maximum number of headlines to retrieve per source (optional)
     * @param dates               the dates of the News headlines. Requires a symbol be specified. (optional)
     * @param dateRanges          the date ranges of the News headlines where the key in the {@link Map} is the 'from'
     *                            and the value in the {@link Map} is the 'to'. Requires a symbol be specified.
     *                            (optional)
     * @param messageLineListener the {@link MultiMessageListener} for the message {@link MessageLine}s
     *
     * @throws IOException thrown for {@link IOException}s
     */
    public void requestNewsHeadlines(List<String> sources, List<String> symbols, XMLTextOption xmlTextOption,
            Integer limit, List<LocalDate> dates, Map<LocalDate, LocalDate> dateRanges,
            MultiMessageListener<MessageLine> messageLineListener) throws IOException {
        checkNotNull(xmlTextOption);
        checkNotNull(messageLineListener);

        String requestID = requestIDFeedHelper.getNewRequestID();
        StringBuilder requestBuilder = new StringBuilder();

        requestBuilder.append("NHL").append(",");

        if (sources != null) {
            requestBuilder.append(String.join(":", sources));
        }
        requestBuilder.append(",");

        if (symbols != null) {
            requestBuilder.append(String.join(":", symbols));
        }
        requestBuilder.append(",");

        requestBuilder.append(xmlTextOption.value()).append(",");

        if (limit != null) {
            requestBuilder.append(limit);
        }
        requestBuilder.append(",");

        StringBuilder dateBuilder = new StringBuilder();
        if (dates != null) {
            dateBuilder.append(dates.stream().map(DATE::format)
                    .collect(Collectors.joining(":")));
        }
        if (dateRanges != null) {
            dateBuilder.append(dateRanges.entrySet().stream()
                    .map(entry -> DATE.format(entry.getKey()) + "-" + DATE.format(entry.getValue()))
                    .collect(Collectors.joining(":")));
        }
        requestBuilder.append(dateBuilder).append(",");

        requestBuilder.append(requestID);
        requestBuilder.append(LineEnding.CR_LF.getASCIIString());

        synchronized (messageReceivedLock) {
            messageLineListenersOfRequestIDs.put(requestID, messageLineListener);
        }
        String requestString = requestBuilder.toString();
        LOGGER.debug("Sending request: {}", requestString);
        sendMessage(requestString);
    }

    /**
     * This requests News stories. This sends an NSY request. This method is thread-safe.
     *
     * @param id                  the headline/story identifier which can be retrieved via {@link
     *                            #requestNewsHeadlines(List, List, XMLTextOption, Integer, List, Map,
     *                            MultiMessageListener)}.
     * @param xmlTextEmailOption  the {@link XMLTextEmailOption}. See
     *                            <a href="https://www.iqfeed.net/dev/api/docs//NewsLookupviaTCPIP.cfm">News
     *                            Lookup via TCP/IP</a> for the response formats.
     * @param deliverTo           email address to deliver story to if 'xmlTextEmailOption' is {@link
     *                            XMLTextEmailOption#EMAIL}
     * @param messageLineListener the {@link MultiMessageListener} for the message {@link MessageLine}s
     *
     * @throws IOException thrown for {@link IOException}s
     */
    public void requestNewsStory(String id, XMLTextEmailOption xmlTextEmailOption, String deliverTo,
            MultiMessageListener<MessageLine> messageLineListener) throws IOException {
        checkNotNull(id);
        checkNotNull(xmlTextEmailOption);
        checkArgument(xmlTextEmailOption != XMLTextEmailOption.EMAIL || deliverTo != null,
                "'deliverTo' must be present with EMAIL option!");
        checkNotNull(messageLineListener);

        String requestID = requestIDFeedHelper.getNewRequestID();
        StringBuilder requestBuilder = new StringBuilder();

        requestBuilder.append("NSY").append(",");
        requestBuilder.append(id).append(",");
        requestBuilder.append(xmlTextEmailOption.value()).append(",");

        if (deliverTo != null) {
            requestBuilder.append(deliverTo);
        }
        requestBuilder.append(",");

        requestBuilder.append(requestID);
        requestBuilder.append(LineEnding.CR_LF.getASCIIString());

        synchronized (messageReceivedLock) {
            messageLineListenersOfRequestIDs.put(requestID, messageLineListener);
        }
        String requestString = requestBuilder.toString();
        LOGGER.debug("Sending request: {}", requestString);
        sendMessage(requestString);
    }

    /**
     * This requests the count of News stories. This sends an NSC request. This method is thread-safe.
     *
     * @param symbols             a {@link List} of symbols
     * @param xmlTextOption       the {@link XMLTextOption}. See
     *                            <a href="https://www.iqfeed.net/dev/api/docs//NewsLookupviaTCPIP.cfm">News
     *                            Lookup via TCP/IP</a> for the response formats.
     * @param sources             a {@link List} of news sources which can be retrieved via {@link
     *                            #requestNewsConfiguration(XMLTextOption, MultiMessageListener)}. (optional)
     * @param fromDate            the 'from' {@link LocalDate} (optional if 'toDate' is also <code>null</code>)
     * @param toDate              the 'to' {@link LocalDate} (optional if 'fromDate' is also <code>null</code>)
     * @param messageLineListener the {@link MultiMessageListener} for the message {@link MessageLine}s
     *
     * @throws IOException thrown for {@link IOException}s
     */
    public void requestNewsStoryCount(List<String> symbols, XMLTextOption xmlTextOption, List<String> sources,
            LocalDate fromDate, LocalDate toDate, MultiMessageListener<MessageLine> messageLineListener)
            throws IOException {
        checkNotNull(symbols);
        checkArgument(!symbols.isEmpty(), "'symbols' cannot be empty!");
        checkNotNull(xmlTextOption);
        checkArgument(fromDate == null || toDate != null, "You  must have both 'from' and 'to' dates!");
        checkArgument(toDate == null || fromDate != null, "You  must have both 'from' and 'to' dates!");
        checkNotNull(messageLineListener);

        String requestID = requestIDFeedHelper.getNewRequestID();
        StringBuilder requestBuilder = new StringBuilder();

        requestBuilder.append("NSC").append(",");
        requestBuilder.append(String.join(":", symbols)).append(",");
        requestBuilder.append(xmlTextOption.value()).append(",");

        if (sources != null) {
            requestBuilder.append(String.join(":", sources));
        }
        requestBuilder.append(",");

        if (fromDate != null) {
            // 'toDate' will also be != null here
            requestBuilder.append(DATE.format(fromDate)).append("-").append(DATE.format(toDate));
        }
        requestBuilder.append(",");

        requestBuilder.append(requestID);
        requestBuilder.append(LineEnding.CR_LF.getASCIIString());

        synchronized (messageReceivedLock) {
            messageLineListenersOfRequestIDs.put(requestID, messageLineListener);
        }
        String requestString = requestBuilder.toString();
        LOGGER.debug("Sending request: {}", requestString);
        sendMessage(requestString);
    }

    //
    // END Feed commands
    //
}
