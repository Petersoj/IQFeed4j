package net.jacobpeterson.iqfeed4j.feed.lookup.news;

import net.jacobpeterson.iqfeed4j.feed.lookup.AbstractLookupFeed;
import net.jacobpeterson.iqfeed4j.feed.lookup.news.xml.configuration.NewsConfiguration;
import net.jacobpeterson.iqfeed4j.feed.lookup.news.xml.headline.NewsHeadlines;
import net.jacobpeterson.iqfeed4j.feed.lookup.news.xml.story.NewsStories;
import net.jacobpeterson.iqfeed4j.feed.lookup.news.xml.storycount.NewsStoryCounts;
import net.jacobpeterson.iqfeed4j.feed.message.MultiMessageAccumulator;
import net.jacobpeterson.iqfeed4j.feed.message.MultiMessageListener;
import net.jacobpeterson.iqfeed4j.model.feed.common.enums.FeedMessageType;
import net.jacobpeterson.iqfeed4j.model.feed.common.message.MessageLine;
import net.jacobpeterson.iqfeed4j.model.feed.lookup.news.enums.NewsCommand;
import net.jacobpeterson.iqfeed4j.model.feed.lookup.news.enums.XMLTextEmailOption;
import net.jacobpeterson.iqfeed4j.model.feed.lookup.news.enums.XMLTextOption;
import net.jacobpeterson.iqfeed4j.util.csv.mapper.index.TrailingIndexCSVMapper;
import net.jacobpeterson.iqfeed4j.util.string.LineEnding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static net.jacobpeterson.iqfeed4j.util.csv.CSVUtil.valueEquals;
import static net.jacobpeterson.iqfeed4j.util.csv.CSVUtil.valueNotWhitespace;
import static net.jacobpeterson.iqfeed4j.util.csv.mapper.AbstractCSVMapper.DateTimeFormatters.DATE;
import static net.jacobpeterson.iqfeed4j.util.csv.mapper.AbstractCSVMapper.PrimitiveConvertors.STRING;
import static net.jacobpeterson.iqfeed4j.util.xml.XMLUtil.STANDARD_XML_MAPPER;

/**
 * {@link NewsFeed} is an {@link AbstractLookupFeed} for news data.
 */
public class NewsFeed extends AbstractLookupFeed {

    private static final Logger LOGGER = LoggerFactory.getLogger(NewsFeed.class);
    protected static final String FEED_NAME_SUFFIX = " News";

    protected static final TrailingIndexCSVMapper<MessageLine> MESSAGE_LINE_CSV_MAPPER;

    static {
        MESSAGE_LINE_CSV_MAPPER = new TrailingIndexCSVMapper<>(MessageLine::new);
        MESSAGE_LINE_CSV_MAPPER.setTrailingMapping(MessageLine::setLine, STRING);
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
        super(LOGGER, newsFeedName + FEED_NAME_SUFFIX, hostname, port, COMMA_DELIMITED_SPLITTER);

        messageReceivedLock = new Object();
        messageLineListenersOfRequestIDs = new HashMap<>();
    }

    @Override
    protected void onMessageReceived(String[] csv) {
        if (valueEquals(csv, 0, FeedMessageType.ERROR.value())) {
            LOGGER.error("Received error message! {}", (Object) csv);
            return;
        }

        // This feed may deliver empty messages so just ignore them
        if (!valueNotWhitespace(csv, 0)) {
            return;
        }

        String requestID = csv[0];

        synchronized (messageReceivedLock) {
            handleStandardMultiMessage(csv, requestID, 2, messageLineListenersOfRequestIDs, MESSAGE_LINE_CSV_MAPPER);
        }
    }

    @Override
    protected void onFeedSocketException(Exception exception) {
        messageLineListenersOfRequestIDs.values().forEach(listener -> listener.onMessageException(exception));
    }

    @Override
    protected void onFeedSocketClose() {
        onFeedSocketException(new RuntimeException("Feed socket closed normally while a request was active!"));
    }

    //
    // START Feed commands
    //

    /**
     * This requests the current News configuration. This sends a {@link NewsCommand#NEWS_CONFIGURATION} request.
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

        requestBuilder.append(NewsCommand.NEWS_CONFIGURATION.value()).append(",");
        requestBuilder.append(xmlTextOption.value()).append(",");
        requestBuilder.append(requestID);
        requestBuilder.append(LineEnding.CR_LF.getASCIIString());

        synchronized (messageReceivedLock) {
            messageLineListenersOfRequestIDs.put(requestID, messageLineListener);
        }

        sendAndLogMessage(requestBuilder.toString());
    }

    /**
     * Calls {@link #requestNewsConfiguration(XMLTextOption, MultiMessageListener)} and parses the XML data into POJOs.
     *
     * @return a {@link NewsConfiguration}
     *
     * @throws IOException          thrown for {@link IOException}s
     * @throws ExecutionException   thrown for {@link ExecutionException}s
     * @throws InterruptedException thrown for {@link InterruptedException}s
     */
    public NewsConfiguration requestNewsConfiguration() throws IOException, ExecutionException, InterruptedException {
        MultiMessageAccumulator<MessageLine> asyncListener = new MultiMessageAccumulator<>();
        requestNewsConfiguration(XMLTextOption.XML, asyncListener);

        String xmlMessage = asyncListener.getMessages().stream()
                .map(MessageLine::getLine)
                .collect(Collectors.joining());
        return STANDARD_XML_MAPPER.readValue(xmlMessage, NewsConfiguration.class);
    }

    /**
     * This requests News headlines. This sends a {@link NewsCommand#NEWS_HEADLINE} request.
     *
     * @param sources             a {@link Collection} of news sources which can be retrieved via {@link
     *                            #requestNewsConfiguration(XMLTextOption, MultiMessageListener)}. (optional)
     * @param symbols             a {@link Collection} of symbols for which to receive headlines (optional)
     * @param xmlTextOption       the {@link XMLTextOption}. See
     *                            <a href="https://www.iqfeed.net/dev/api/docs//NewsLookupviaTCPIP.cfm">News
     *                            Lookup via TCP/IP</a> for the response formats.
     * @param limit               the maximum number of headlines to retrieve per source (optional)
     * @param dates               a {@link Collection} of {@link LocalDate}s of the News headlines. Requires a symbol be
     *                            specified. (optional)
     * @param dateRanges          the date ranges of the News headlines where the key in the {@link Map} is the 'from'
     *                            and the value in the {@link Map} is the 'to'. Requires a symbol be specified.
     *                            (optional)
     * @param messageLineListener the {@link MultiMessageListener} for the message {@link MessageLine}s
     *
     * @throws IOException thrown for {@link IOException}s
     */
    public void requestNewsHeadlines(Collection<String> sources, Collection<String> symbols,
            XMLTextOption xmlTextOption, Integer limit, Collection<LocalDate> dates,
            Map<LocalDate, LocalDate> dateRanges, MultiMessageListener<MessageLine> messageLineListener)
            throws IOException {
        checkNotNull(xmlTextOption);
        checkNotNull(messageLineListener);

        String requestID = requestIDFeedHelper.getNewRequestID();
        StringBuilder requestBuilder = new StringBuilder();

        requestBuilder.append(NewsCommand.NEWS_HEADLINE.value()).append(",");

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

        sendAndLogMessage(requestBuilder.toString());
    }

    /**
     * Calls {@link #requestNewsHeadlines(Collection, Collection, XMLTextOption, Integer, Collection, Map,
     * MultiMessageListener)} and parses the XML data into POJOs.
     *
     * @return {@link NewsHeadlines}
     *
     * @throws IOException          thrown for {@link IOException}s
     * @throws ExecutionException   thrown for {@link ExecutionException}s
     * @throws InterruptedException thrown for {@link InterruptedException}s
     */
    public NewsHeadlines requestNewsHeadlines(Collection<String> sources, Collection<String> symbols, Integer limit,
            Collection<LocalDate> dates, Map<LocalDate, LocalDate> dateRanges)
            throws IOException, ExecutionException, InterruptedException {
        MultiMessageAccumulator<MessageLine> asyncListener = new MultiMessageAccumulator<>();
        requestNewsHeadlines(sources, symbols, XMLTextOption.XML, limit, dates, dateRanges, asyncListener);

        String xmlMessage = asyncListener.getMessages().stream()
                .map(MessageLine::getLine)
                .collect(Collectors.joining());
        return STANDARD_XML_MAPPER.readValue(xmlMessage, NewsHeadlines.class);
    }

    /**
     * This requests News stories. This sends a {@link NewsCommand#NEWS_STORY} request.
     *
     * @param id                  the headline/story identifier which can be retrieved via {@link
     *                            #requestNewsHeadlines(Collection, Collection, XMLTextOption, Integer, Collection, Map,
     *                            MultiMessageListener)}.
     * @param xmlTextEmailOption  the {@link XMLTextEmailOption}. See
     *                            <a href="https://www.iqfeed.net/dev/api/docs//NewsLookupviaTCPIP.cfm">News
     *                            Lookup via TCP/IP</a> for the response formats.
     * @param deliverTo           email address to deliver story to if <code>xmlTextEmailOption</code> is {@link
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

        requestBuilder.append(NewsCommand.NEWS_STORY.value()).append(",");
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

        sendAndLogMessage(requestBuilder.toString());
    }

    /**
     * Calls {@link #requestNewsStory(String, XMLTextEmailOption, String, MultiMessageListener)} and parses the XML data
     * into POJOs.
     *
     * @return {@link NewsStories}
     *
     * @throws IOException          thrown for {@link IOException}s
     * @throws ExecutionException   thrown for {@link ExecutionException}s
     * @throws InterruptedException thrown for {@link InterruptedException}s
     */
    public NewsStories requestNewsStory(String id)
            throws IOException, ExecutionException, InterruptedException {
        MultiMessageAccumulator<MessageLine> asyncListener = new MultiMessageAccumulator<>();
        requestNewsStory(id, XMLTextEmailOption.XML, null, asyncListener);

        String xmlMessage = asyncListener.getMessages().stream()
                .map(MessageLine::getLine)
                .collect(Collectors.joining());
        return STANDARD_XML_MAPPER.readValue(xmlMessage, NewsStories.class);
    }

    /**
     * This requests the count of News stories. This sends a {@link NewsCommand#NEWS_STORY_COUNT} request.
     *
     * @param symbols             a {@link Collection} of symbols
     * @param xmlTextOption       the {@link XMLTextOption}. See
     *                            <a href="https://www.iqfeed.net/dev/api/docs//NewsLookupviaTCPIP.cfm">News
     *                            Lookup via TCP/IP</a> for the response formats.
     * @param sources             a {@link Collection} of news sources which can be retrieved via {@link
     *                            #requestNewsConfiguration(XMLTextOption, MultiMessageListener)}. (optional)
     * @param fromDate            the 'from' {@link LocalDate} (optional if <code>toDate</code> is also
     *                            <code>null</code>)
     * @param toDate              the 'to' {@link LocalDate} (optional if <code>fromDate</code> is also
     *                            <code>null</code>)
     * @param messageLineListener the {@link MultiMessageListener} for the message {@link MessageLine}s
     *
     * @throws IOException thrown for {@link IOException}s
     */
    public void requestNewsStoryCount(Collection<String> symbols, XMLTextOption xmlTextOption,
            Collection<String> sources, LocalDate fromDate, LocalDate toDate,
            MultiMessageListener<MessageLine> messageLineListener) throws IOException {
        checkNotNull(symbols);
        checkArgument(!symbols.isEmpty(), "'symbols' cannot be empty!");
        checkNotNull(xmlTextOption);
        checkArgument(fromDate == null || toDate != null, "You  must have both 'from' and 'to' dates!");
        checkArgument(toDate == null || fromDate != null, "You  must have both 'from' and 'to' dates!");
        checkNotNull(messageLineListener);

        String requestID = requestIDFeedHelper.getNewRequestID();
        StringBuilder requestBuilder = new StringBuilder();

        requestBuilder.append(NewsCommand.NEWS_STORY_COUNT.value()).append(",");
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

        sendAndLogMessage(requestBuilder.toString());
    }

    /**
     * Calls {@link #requestNewsStoryCount(Collection, XMLTextOption, Collection, LocalDate, LocalDate,
     * MultiMessageListener)} and parses the XML data into POJOs.
     *
     * @return {@link NewsStoryCounts}
     *
     * @throws IOException          thrown for {@link IOException}s
     * @throws ExecutionException   thrown for {@link ExecutionException}s
     * @throws InterruptedException thrown for {@link InterruptedException}s
     */
    public NewsStoryCounts requestNewsStoryCount(Collection<String> symbols, Collection<String> sources,
            LocalDate fromDate, LocalDate toDate) throws IOException, ExecutionException, InterruptedException {
        MultiMessageAccumulator<MessageLine> asyncListener = new MultiMessageAccumulator<>();
        requestNewsStoryCount(symbols, XMLTextOption.XML, sources, fromDate, toDate, asyncListener);

        String xmlMessage = asyncListener.getMessages().stream()
                .map(MessageLine::getLine)
                .collect(Collectors.joining());
        return STANDARD_XML_MAPPER.readValue(xmlMessage, NewsStoryCounts.class);
    }

    //
    // END Feed commands
    //
}
