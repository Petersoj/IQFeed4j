package net.jacobpeterson.iqfeed4j.feed.lookup.symbolmarketinfo;

import com.google.common.base.Preconditions;
import net.jacobpeterson.iqfeed4j.feed.MultiMessageListener;
import net.jacobpeterson.iqfeed4j.feed.lookup.AbstractLookupFeed;
import net.jacobpeterson.iqfeed4j.model.feedenums.FeedMessageType;
import net.jacobpeterson.iqfeed4j.model.feedenums.lookup.symbolmarketinfo.FilterType;
import net.jacobpeterson.iqfeed4j.model.feedenums.lookup.symbolmarketinfo.SearchCodeType;
import net.jacobpeterson.iqfeed4j.model.feedenums.lookup.symbolmarketinfo.SearchField;
import net.jacobpeterson.iqfeed4j.model.lookup.symbolmarketinfo.ListedMarket;
import net.jacobpeterson.iqfeed4j.model.lookup.symbolmarketinfo.NIACCode;
import net.jacobpeterson.iqfeed4j.model.lookup.symbolmarketinfo.SICCode;
import net.jacobpeterson.iqfeed4j.model.lookup.symbolmarketinfo.SecurityType;
import net.jacobpeterson.iqfeed4j.model.lookup.symbolmarketinfo.SymbolSearchResult;
import net.jacobpeterson.iqfeed4j.model.lookup.symbolmarketinfo.TradeCondition;
import net.jacobpeterson.iqfeed4j.util.csv.mapper.IndexCSVMapper;
import net.jacobpeterson.iqfeed4j.util.csv.mapper.TrailingCSVMapper;
import net.jacobpeterson.iqfeed4j.util.string.LineEnding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static net.jacobpeterson.iqfeed4j.util.csv.CSVUtil.valueEquals;
import static net.jacobpeterson.iqfeed4j.util.csv.CSVUtil.valuePresent;
import static net.jacobpeterson.iqfeed4j.util.csv.mapper.CSVMapper.PrimitiveConvertors.INT;
import static net.jacobpeterson.iqfeed4j.util.csv.mapper.CSVMapper.PrimitiveConvertors.STRING;

/**
 * {@link SymbolMarketInfoFeed} is an {@link AbstractLookupFeed} for symbol and market lookup data.
 */
public class SymbolMarketInfoFeed extends AbstractLookupFeed {

    private static final Logger LOGGER = LoggerFactory.getLogger(SymbolMarketInfoFeed.class);
    private static final String FEED_NAME_SUFFIX = " Symbol and Market Info";
    private static final TrailingCSVMapper<SymbolSearchResult> FILTER_SYMBOL_SEARCH_RESULT_CSV_MAPPER;
    private static final TrailingCSVMapper<SymbolSearchResult> SIC_CODE_SYMBOL_SEARCH_RESULT_CSV_MAPPER;
    private static final TrailingCSVMapper<SymbolSearchResult> NIAC_CODE_SYMBOL_SEARCH_RESULT_CSV_MAPPER;
    private static final IndexCSVMapper<ListedMarket> LISTED_MARKET_CSV_MAPPER;
    private static final IndexCSVMapper<SecurityType> SECURITY_TYPE_CSV_MAPPER;
    private static final IndexCSVMapper<TradeCondition> TRADE_CONDITION_CSV_MAPPER;
    private static final IndexCSVMapper<SICCode> SIC_CODE_CSV_MAPPER;
    private static final IndexCSVMapper<NIACCode> NIAC_CODE_CSV_MAPPER;

    static {
        // Add mappings with CSV indices analogous to line of execution

        FILTER_SYMBOL_SEARCH_RESULT_CSV_MAPPER = new TrailingCSVMapper<>(SymbolSearchResult::new);
        FILTER_SYMBOL_SEARCH_RESULT_CSV_MAPPER.addMapping(SymbolSearchResult::setSymbol, STRING);
        FILTER_SYMBOL_SEARCH_RESULT_CSV_MAPPER.addMapping(SymbolSearchResult::setListedMarketID, INT);
        FILTER_SYMBOL_SEARCH_RESULT_CSV_MAPPER.addMapping(SymbolSearchResult::setSecurityTypeID, INT);
        FILTER_SYMBOL_SEARCH_RESULT_CSV_MAPPER.setTrailingMapping(SymbolSearchResult::setDescription, STRING);

        SIC_CODE_SYMBOL_SEARCH_RESULT_CSV_MAPPER = new TrailingCSVMapper<>(SymbolSearchResult::new);
        SIC_CODE_SYMBOL_SEARCH_RESULT_CSV_MAPPER.addMapping(SymbolSearchResult::setSICCode, INT);
        SIC_CODE_SYMBOL_SEARCH_RESULT_CSV_MAPPER.addMapping(SymbolSearchResult::setSymbol, STRING);
        SIC_CODE_SYMBOL_SEARCH_RESULT_CSV_MAPPER.addMapping(SymbolSearchResult::setListedMarketID, INT);
        SIC_CODE_SYMBOL_SEARCH_RESULT_CSV_MAPPER.addMapping(SymbolSearchResult::setSecurityTypeID, INT);
        SIC_CODE_SYMBOL_SEARCH_RESULT_CSV_MAPPER.setTrailingMapping(SymbolSearchResult::setDescription, STRING);

        NIAC_CODE_SYMBOL_SEARCH_RESULT_CSV_MAPPER = new TrailingCSVMapper<>(SymbolSearchResult::new);
        NIAC_CODE_SYMBOL_SEARCH_RESULT_CSV_MAPPER.addMapping(SymbolSearchResult::setNIACCode, INT);
        NIAC_CODE_SYMBOL_SEARCH_RESULT_CSV_MAPPER.addMapping(SymbolSearchResult::setSymbol, STRING);
        NIAC_CODE_SYMBOL_SEARCH_RESULT_CSV_MAPPER.addMapping(SymbolSearchResult::setListedMarketID, INT);
        NIAC_CODE_SYMBOL_SEARCH_RESULT_CSV_MAPPER.addMapping(SymbolSearchResult::setSecurityTypeID, INT);
        NIAC_CODE_SYMBOL_SEARCH_RESULT_CSV_MAPPER.setTrailingMapping(SymbolSearchResult::setDescription, STRING);

        LISTED_MARKET_CSV_MAPPER = new IndexCSVMapper<>(ListedMarket::new);
        LISTED_MARKET_CSV_MAPPER.addMapping(ListedMarket::setListedMarketID, INT);
        LISTED_MARKET_CSV_MAPPER.addMapping(ListedMarket::setShortName, STRING);
        LISTED_MARKET_CSV_MAPPER.addMapping(ListedMarket::setLongName, STRING);
        LISTED_MARKET_CSV_MAPPER.addMapping(ListedMarket::setGroupID, INT);
        LISTED_MARKET_CSV_MAPPER.addMapping(ListedMarket::setShortGroupName, STRING);

        SECURITY_TYPE_CSV_MAPPER = new IndexCSVMapper<>(SecurityType::new);
        SECURITY_TYPE_CSV_MAPPER.addMapping(SecurityType::setSecurityTypeID, INT);
        SECURITY_TYPE_CSV_MAPPER.addMapping(SecurityType::setShortName, STRING);
        SECURITY_TYPE_CSV_MAPPER.addMapping(SecurityType::setLongName, STRING);

        TRADE_CONDITION_CSV_MAPPER = new IndexCSVMapper<>(TradeCondition::new);
        TRADE_CONDITION_CSV_MAPPER.addMapping(TradeCondition::setTradeConditionID, INT);
        TRADE_CONDITION_CSV_MAPPER.addMapping(TradeCondition::setShortName, STRING);
        TRADE_CONDITION_CSV_MAPPER.addMapping(TradeCondition::setLongName, STRING);

        SIC_CODE_CSV_MAPPER = new IndexCSVMapper<>(SICCode::new);
        SIC_CODE_CSV_MAPPER.addMapping(SICCode::setSICCode, INT);
        SIC_CODE_CSV_MAPPER.addMapping(SICCode::setDescription, STRING);

        NIAC_CODE_CSV_MAPPER = new IndexCSVMapper<>(NIACCode::new);
        NIAC_CODE_CSV_MAPPER.addMapping(NIACCode::setNIACCode, INT);
        NIAC_CODE_CSV_MAPPER.addMapping(NIACCode::setDescription, STRING);
    }

    protected final Object messageReceivedLock;
    protected final HashMap<String, MultiMessageListener<SymbolSearchResult>> symbolSearchResultListenersOfRequestIDs;
    protected final HashMap<String, MultiMessageListener<ListedMarket>> listedMarketListenersOfRequestIDs;
    protected final HashMap<String, MultiMessageListener<SecurityType>> securityTypeListenersOfRequestIDs;
    protected final HashMap<String, MultiMessageListener<TradeCondition>> tradeConditionListenersOfRequestIDs;
    protected final HashMap<String, MultiMessageListener<SICCode>> sicCodeListenersOfRequestIDs;
    protected final HashMap<String, MultiMessageListener<NIACCode>> niacCodeListenersOfRequestIDs;

    /**
     * Instantiates a new {@link SymbolMarketInfoFeed}.
     *
     * @param symbolMarketInfoFeedName the {@link SymbolMarketInfoFeed} name
     * @param hostname                 the hostname
     * @param port                     the port
     */
    public SymbolMarketInfoFeed(String symbolMarketInfoFeedName, String hostname, int port) {
        super(symbolMarketInfoFeedName + FEED_NAME_SUFFIX, hostname, port, COMMA_DELIMITED_SPLITTER);

        messageReceivedLock = new Object();
        symbolSearchResultListenersOfRequestIDs = new HashMap<>();
        listedMarketListenersOfRequestIDs = new HashMap<>();
        securityTypeListenersOfRequestIDs = new HashMap<>();
        tradeConditionListenersOfRequestIDs = new HashMap<>();
        sicCodeListenersOfRequestIDs = new HashMap<>();
        niacCodeListenersOfRequestIDs = new HashMap<>();
    }

    @Override
    protected void onMessageReceived(String[] csv) {
        if (valueEquals(csv, 0, FeedMessageType.ERROR.value())) {
            LOGGER.error("Received error message! {}", (Object) csv);
            return;
        }

        // All messages sent on this feed must have a Request ID first
        if (!valuePresent(csv, 0)) {
            LOGGER.error("Received unknown message format: {}", (Object) csv);
            return;
        }

        String requestID = csv[0];

        synchronized (messageReceivedLock) {
            if (handleMultiMessage(csv, requestID, symbolSearchResultListenersOfRequestIDs,
                    FILTER_SYMBOL_SEARCH_RESULT_CSV_MAPPER)) {
                return;
            }

            if (handleMultiMessage(csv, requestID, symbolSearchResultListenersOfRequestIDs,
                    SIC_CODE_SYMBOL_SEARCH_RESULT_CSV_MAPPER)) {
                return;
            }

            if (handleMultiMessage(csv, requestID, symbolSearchResultListenersOfRequestIDs,
                    NIAC_CODE_SYMBOL_SEARCH_RESULT_CSV_MAPPER)) {
                return;
            }

            if (handleMultiMessage(csv, requestID, listedMarketListenersOfRequestIDs, LISTED_MARKET_CSV_MAPPER)) {
                return;
            }

            if (handleMultiMessage(csv, requestID, securityTypeListenersOfRequestIDs, SECURITY_TYPE_CSV_MAPPER)) {
                return;
            }

            if (handleMultiMessage(csv, requestID, tradeConditionListenersOfRequestIDs, TRADE_CONDITION_CSV_MAPPER)) {
                return;
            }

            if (handleMultiMessage(csv, requestID, sicCodeListenersOfRequestIDs, SIC_CODE_CSV_MAPPER)) {
                return;
            }

            if (handleMultiMessage(csv, requestID, niacCodeListenersOfRequestIDs, NIAC_CODE_CSV_MAPPER)) {
                return;
            }
        }
    }

    @Override
    protected void onAsyncException(String message, Exception exception) {
        LOGGER.error(message, exception);
        LOGGER.info("Attempting to close {}...", feedName);
        try {
            stop();
        } catch (Exception stopException) {
            LOGGER.error("Could not close {}!", feedName, stopException);
        }
    }

    //
    // START Feed commands
    //

    /**
     * A symbol search by symbol or description. Can be filtered by providing a list of Listed Markets or Security
     * Types. This sends an SBF request. This method is thread-safe.
     *
     * @param searchField                the {@link SearchField}
     * @param searchString               the string to search for
     * @param filterType                 the {@link FilterType}
     * @param filterValues               the {@link FilterType} values
     * @param symbolSearchResultListener the {@link MultiMessageListener} for the requested {@link SymbolSearchResult}s
     *
     * @throws IOException thrown for {@link IOException}s
     */
    public void searchSymbols(SearchField searchField, String searchString, FilterType filterType,
            List<Integer> filterValues, MultiMessageListener<SymbolSearchResult> symbolSearchResultListener)
            throws IOException {
        Preconditions.checkNotNull(searchField);
        Preconditions.checkNotNull(searchString);
        Preconditions.checkNotNull(filterType);
        Preconditions.checkNotNull(filterValues);
        Preconditions.checkNotNull(symbolSearchResultListener);

        String requestID = getNewRequestID();
        StringBuilder requestBuilder = new StringBuilder();

        requestBuilder.append("SBF").append(",");
        requestBuilder.append(searchField.value()).append(",");
        requestBuilder.append(searchString).append(",");
        requestBuilder.append(filterType.value()).append(",");
        // Create a space delimited list
        requestBuilder.append(filterValues.stream().map(String::valueOf).collect(Collectors.joining(" "))).append(",");
        requestBuilder.append(requestID);

        requestBuilder.append(LineEnding.CR_LF.getASCIIString());

        synchronized (messageReceivedLock) {
            symbolSearchResultListenersOfRequestIDs.put(requestID, symbolSearchResultListener);
        }
        String requestString = requestBuilder.toString();
        LOGGER.debug("Sending request: {}", requestString);
        sendMessage(requestString);
    }

    /**
     * Searches for a list of symbols existing within a specific {@link SearchCodeType} or group of {@link
     * SearchCodeType} codes. This sends an SBS or SBN request. This method is thread-safe.
     *
     * @param searchCodeType             the {@link SearchCodeType}
     * @param searchString               at least the first 2 digits of an existing {@link SearchCodeType}.
     * @param symbolSearchResultListener the {@link MultiMessageListener} for the requested {@link SymbolSearchResult}s
     *
     * @throws IOException thrown for {@link IOException}s
     */
    public void searchSymbols(SearchCodeType searchCodeType, String searchString,
            MultiMessageListener<SymbolSearchResult> symbolSearchResultListener) throws IOException {
        Preconditions.checkNotNull(searchCodeType);
        Preconditions.checkNotNull(searchString);
        Preconditions.checkNotNull(symbolSearchResultListener);

        Preconditions.checkArgument(searchString.length() >= 2);

        String requestID = getNewRequestID();
        StringBuilder requestBuilder = new StringBuilder();

        requestBuilder.append(searchCodeType.value()).append(",");
        requestBuilder.append(searchString).append(",");
        requestBuilder.append(requestID);

        requestBuilder.append(LineEnding.CR_LF.getASCIIString());

        synchronized (messageReceivedLock) {
            symbolSearchResultListenersOfRequestIDs.put(requestID, symbolSearchResultListener);
        }
        String requestString = requestBuilder.toString();
        LOGGER.debug("Sending request: {}", requestString);
        sendMessage(requestString);
    }

    /**
     * Sends a generic request for a {@link MultiMessageListener} given the below parameters.
     *
     * @param <T>                   the type of {@link MultiMessageListener}
     * @param requestCode           the request code
     * @param listenersOfRequestIDs the {@link Map} with the keys being the Request IDs and the values being the
     *                              corresponding {@link MultiMessageListener}s
     * @param listener              the {@link MultiMessageListener}
     *
     * @throws IOException the io exception
     */
    private <T> void requestGenericMultiMessage(String requestCode,
            Map<String, MultiMessageListener<T>> listenersOfRequestIDs, MultiMessageListener<T> listener)
            throws IOException {
        Preconditions.checkNotNull(listener);

        String requestID = getNewRequestID();
        StringBuilder requestBuilder = new StringBuilder();

        requestBuilder.append(requestCode).append(",");
        requestBuilder.append(requestID);

        requestBuilder.append(LineEnding.CR_LF.getASCIIString());

        synchronized (messageReceivedLock) {
            listenersOfRequestIDs.put(requestID, listener);
        }
        String requestString = requestBuilder.toString();
        LOGGER.debug("Sending request: {}", requestString);
        sendMessage(requestString);
    }

    /**
     * Request a list of {@link ListedMarket}s from the feed. This sends an SLM request. This method is thread-safe.
     *
     * @param listedMarketListener the {@link MultiMessageListener} for the requested {@link ListedMarket}s
     *
     * @throws IOException thrown for {@link IOException}s
     */
    public void requestListedMarkets(MultiMessageListener<ListedMarket> listedMarketListener) throws IOException {
        requestGenericMultiMessage("SLM", listedMarketListenersOfRequestIDs, listedMarketListener);
    }

    /**
     * Request a list of {@link SecurityType}s from the feed. This sends an SST request. This method is thread-safe.
     *
     * @param securityTypeListener the {@link MultiMessageListener} for the requested {@link SecurityType}s
     *
     * @throws IOException thrown for {@link IOException}s
     */
    public void requestSecurityTypes(MultiMessageListener<SecurityType> securityTypeListener) throws IOException {
        requestGenericMultiMessage("SST", securityTypeListenersOfRequestIDs, securityTypeListener);
    }

    /**
     * Request a list of {@link TradeCondition}s from the feed. This sends an STC request. This method is thread-safe.
     *
     * @param tradeConditionListener the {@link MultiMessageListener} for the requested {@link TradeCondition}s
     *
     * @throws IOException thrown for {@link IOException}s
     */
    public void requestTradeConditions(MultiMessageListener<TradeCondition> tradeConditionListener) throws IOException {
        requestGenericMultiMessage("STC", tradeConditionListenersOfRequestIDs, tradeConditionListener);
    }

    /**
     * Request a list of {@link SICCode}s from the feed. This sends an SSC request. This method is thread-safe.
     *
     * @param sicCodeListener the {@link MultiMessageListener} for the requested {@link SICCode}s
     *
     * @throws IOException thrown for {@link IOException}s
     */
    public void requestSICCodes(MultiMessageListener<SICCode> sicCodeListener) throws IOException {
        requestGenericMultiMessage("SSC", sicCodeListenersOfRequestIDs, sicCodeListener);
    }

    /**
     * Request a list of {@link NIACCode}s from the feed. This sends an SNC request. This method is thread-safe.
     *
     * @param niacCodeListener the {@link MultiMessageListener} for the requested {@link NIACCode}s
     *
     * @throws IOException thrown for {@link IOException}s
     */
    public void requestNIACCodeCodes(MultiMessageListener<NIACCode> niacCodeListener) throws IOException {
        requestGenericMultiMessage("SNC", niacCodeListenersOfRequestIDs, niacCodeListener);
    }

    //
    // END Feed commands
    //
}