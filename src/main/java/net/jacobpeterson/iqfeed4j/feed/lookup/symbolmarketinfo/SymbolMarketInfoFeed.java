package net.jacobpeterson.iqfeed4j.feed.lookup.symbolmarketinfo;

import net.jacobpeterson.iqfeed4j.feed.lookup.AbstractLookupFeed;
import net.jacobpeterson.iqfeed4j.feed.message.MultiMessageIteratorListener;
import net.jacobpeterson.iqfeed4j.feed.message.MultiMessageListener;
import net.jacobpeterson.iqfeed4j.model.feed.lookup.symbolmarketinfo.*;
import net.jacobpeterson.iqfeed4j.model.feed.lookup.symbolmarketinfo.enums.SearchCodeType;
import net.jacobpeterson.iqfeed4j.model.feed.lookup.symbolmarketinfo.enums.SearchField;
import net.jacobpeterson.iqfeed4j.model.feed.lookup.symbolmarketinfo.enums.SymbolFilterType;
import net.jacobpeterson.iqfeed4j.model.feed.lookup.symbolmarketinfo.enums.SymbolMarketInfoCommand;
import net.jacobpeterson.iqfeed4j.util.csv.mapper.index.IndexCSVMapper;
import net.jacobpeterson.iqfeed4j.util.csv.mapper.index.TrailingIndexCSVMapper;
import net.jacobpeterson.iqfeed4j.util.string.LineEnding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static net.jacobpeterson.iqfeed4j.util.csv.mapper.AbstractCSVMapper.PrimitiveConvertors.INTEGER;
import static net.jacobpeterson.iqfeed4j.util.csv.mapper.AbstractCSVMapper.PrimitiveConvertors.STRING;

/**
 * {@link SymbolMarketInfoFeed} is an {@link AbstractLookupFeed} for symbol and market lookup data.
 */
public class SymbolMarketInfoFeed extends AbstractLookupFeed {

    private static final Logger LOGGER = LoggerFactory.getLogger(SymbolMarketInfoFeed.class);
    protected static final String FEED_NAME_SUFFIX = " Symbol and Market Info";
    protected static final TrailingIndexCSVMapper<SymbolSearchResult> FILTER_SYMBOL_SEARCH_RESULT_CSV_MAPPER;
    protected static final TrailingIndexCSVMapper<SymbolSearchResult> SIC_CODE_SYMBOL_SEARCH_RESULT_CSV_MAPPER;
    protected static final TrailingIndexCSVMapper<SymbolSearchResult> NIAC_CODE_SYMBOL_SEARCH_RESULT_CSV_MAPPER;
    protected static final IndexCSVMapper<ListedMarket> LISTED_MARKET_CSV_MAPPER;
    protected static final IndexCSVMapper<SecurityType> SECURITY_TYPE_CSV_MAPPER;
    protected static final IndexCSVMapper<TradeCondition> TRADE_CONDITION_CSV_MAPPER;
    protected static final IndexCSVMapper<SICCode> SIC_CODE_CSV_MAPPER;
    protected static final IndexCSVMapper<NIACCode> NIAC_CODE_CSV_MAPPER;

    static {
        // Add mappings with CSV indices analogous to line of execution

        FILTER_SYMBOL_SEARCH_RESULT_CSV_MAPPER = new TrailingIndexCSVMapper<>(SymbolSearchResult::new);
        FILTER_SYMBOL_SEARCH_RESULT_CSV_MAPPER.addMapping(SymbolSearchResult::setSymbol, STRING);
        FILTER_SYMBOL_SEARCH_RESULT_CSV_MAPPER.addMapping(SymbolSearchResult::setListedMarketID, INTEGER);
        FILTER_SYMBOL_SEARCH_RESULT_CSV_MAPPER.addMapping(SymbolSearchResult::setSecurityTypeID, INTEGER);
        FILTER_SYMBOL_SEARCH_RESULT_CSV_MAPPER.setTrailingMapping(SymbolSearchResult::setDescription, STRING);

        SIC_CODE_SYMBOL_SEARCH_RESULT_CSV_MAPPER = new TrailingIndexCSVMapper<>(SymbolSearchResult::new);
        SIC_CODE_SYMBOL_SEARCH_RESULT_CSV_MAPPER.addMapping(SymbolSearchResult::setSICCode, INTEGER);
        SIC_CODE_SYMBOL_SEARCH_RESULT_CSV_MAPPER.addMapping(SymbolSearchResult::setSymbol, STRING);
        SIC_CODE_SYMBOL_SEARCH_RESULT_CSV_MAPPER.addMapping(SymbolSearchResult::setListedMarketID, INTEGER);
        SIC_CODE_SYMBOL_SEARCH_RESULT_CSV_MAPPER.addMapping(SymbolSearchResult::setSecurityTypeID, INTEGER);
        SIC_CODE_SYMBOL_SEARCH_RESULT_CSV_MAPPER.setTrailingMapping(SymbolSearchResult::setDescription, STRING);

        NIAC_CODE_SYMBOL_SEARCH_RESULT_CSV_MAPPER = new TrailingIndexCSVMapper<>(SymbolSearchResult::new);
        NIAC_CODE_SYMBOL_SEARCH_RESULT_CSV_MAPPER.addMapping(SymbolSearchResult::setNIACCode, INTEGER);
        NIAC_CODE_SYMBOL_SEARCH_RESULT_CSV_MAPPER.addMapping(SymbolSearchResult::setSymbol, STRING);
        NIAC_CODE_SYMBOL_SEARCH_RESULT_CSV_MAPPER.addMapping(SymbolSearchResult::setListedMarketID, INTEGER);
        NIAC_CODE_SYMBOL_SEARCH_RESULT_CSV_MAPPER.addMapping(SymbolSearchResult::setSecurityTypeID, INTEGER);
        NIAC_CODE_SYMBOL_SEARCH_RESULT_CSV_MAPPER.setTrailingMapping(SymbolSearchResult::setDescription, STRING);

        LISTED_MARKET_CSV_MAPPER = new IndexCSVMapper<>(ListedMarket::new);
        LISTED_MARKET_CSV_MAPPER.addMapping(ListedMarket::setListedMarketID, INTEGER);
        LISTED_MARKET_CSV_MAPPER.addMapping(ListedMarket::setShortName, STRING);
        LISTED_MARKET_CSV_MAPPER.addMapping(ListedMarket::setLongName, STRING);
        LISTED_MARKET_CSV_MAPPER.addMapping(ListedMarket::setGroupID, INTEGER);
        LISTED_MARKET_CSV_MAPPER.addMapping(ListedMarket::setShortGroupName, STRING);

        SECURITY_TYPE_CSV_MAPPER = new IndexCSVMapper<>(SecurityType::new);
        SECURITY_TYPE_CSV_MAPPER.addMapping(SecurityType::setSecurityTypeID, INTEGER);
        SECURITY_TYPE_CSV_MAPPER.addMapping(SecurityType::setShortName, STRING);
        SECURITY_TYPE_CSV_MAPPER.addMapping(SecurityType::setLongName, STRING);

        TRADE_CONDITION_CSV_MAPPER = new IndexCSVMapper<>(TradeCondition::new);
        TRADE_CONDITION_CSV_MAPPER.addMapping(TradeCondition::setTradeConditionID, INTEGER);
        TRADE_CONDITION_CSV_MAPPER.addMapping(TradeCondition::setShortName, STRING);
        TRADE_CONDITION_CSV_MAPPER.addMapping(TradeCondition::setLongName, STRING);

        SIC_CODE_CSV_MAPPER = new IndexCSVMapper<>(SICCode::new);
        SIC_CODE_CSV_MAPPER.addMapping(SICCode::setSICCode, INTEGER);
        SIC_CODE_CSV_MAPPER.addMapping(SICCode::setDescription, STRING);

        NIAC_CODE_CSV_MAPPER = new IndexCSVMapper<>(NIACCode::new);
        NIAC_CODE_CSV_MAPPER.addMapping(NIACCode::setNIACCode, INTEGER);
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
        if (isErrorOrInvalidMessage(csv)) {
            return;
        }

        String requestID = csv[0];

        synchronized (messageReceivedLock) {
            if (handleStandardMultiMessage(csv, requestID, 2, symbolSearchResultListenersOfRequestIDs,
                    FILTER_SYMBOL_SEARCH_RESULT_CSV_MAPPER)) {
                return;
            }

            if (handleStandardMultiMessage(csv, requestID, 2, symbolSearchResultListenersOfRequestIDs,
                    SIC_CODE_SYMBOL_SEARCH_RESULT_CSV_MAPPER)) {
                return;
            }

            if (handleStandardMultiMessage(csv, requestID, 2, symbolSearchResultListenersOfRequestIDs,
                    NIAC_CODE_SYMBOL_SEARCH_RESULT_CSV_MAPPER)) {
                return;
            }

            if (handleStandardMultiMessage(csv, requestID, 2, listedMarketListenersOfRequestIDs,
                    LISTED_MARKET_CSV_MAPPER)) {
                return;
            }

            if (handleStandardMultiMessage(csv, requestID, 2, securityTypeListenersOfRequestIDs,
                    SECURITY_TYPE_CSV_MAPPER)) {
                return;
            }

            if (handleStandardMultiMessage(csv, requestID, 2, tradeConditionListenersOfRequestIDs,
                    TRADE_CONDITION_CSV_MAPPER)) {
                return;
            }

            if (handleStandardMultiMessage(csv, requestID, 2, sicCodeListenersOfRequestIDs, SIC_CODE_CSV_MAPPER)) {
                return;
            }

            if (handleStandardMultiMessage(csv, requestID, 2, niacCodeListenersOfRequestIDs, NIAC_CODE_CSV_MAPPER)) {
                return;
            }
        }
    }

    //
    // START Feed commands
    //

    /**
     * A symbol search by symbol or description. Can be filtered by providing a list of Listed Markets or Security
     * Types. This sends a {@link SymbolMarketInfoCommand#SYMBOLS_BY_FILTER} request.
     *
     * @param searchField                the {@link SearchField}
     * @param searchString               the string to search for
     * @param symbolFilterType           the {@link SymbolFilterType}
     * @param filterValues               the {@link SymbolFilterType} values
     * @param symbolSearchResultListener the {@link MultiMessageListener} for the requested {@link SymbolSearchResult}s
     *
     * @throws IOException thrown for {@link IOException}s
     */
    public void searchSymbols(SearchField searchField, String searchString, SymbolFilterType symbolFilterType,
            List<Integer> filterValues, MultiMessageListener<SymbolSearchResult> symbolSearchResultListener)
            throws IOException {
        checkNotNull(searchField);
        checkNotNull(searchString);
        checkNotNull(symbolFilterType);
        checkNotNull(filterValues);
        checkNotNull(symbolSearchResultListener);

        String requestID = requestIDFeedHelper.getNewRequestID();
        StringBuilder requestBuilder = new StringBuilder();

        requestBuilder.append(SymbolMarketInfoCommand.SYMBOLS_BY_FILTER.value()).append(",");
        requestBuilder.append(searchField.value()).append(",");
        requestBuilder.append(searchString).append(",");
        requestBuilder.append(symbolFilterType.value()).append(",");
        // Create a space delimited list
        requestBuilder.append(filterValues.stream().map(String::valueOf).collect(Collectors.joining(" "))).append(",");
        requestBuilder.append(requestID);
        requestBuilder.append(LineEnding.CR_LF.getASCIIString());

        synchronized (messageReceivedLock) {
            symbolSearchResultListenersOfRequestIDs.put(requestID, symbolSearchResultListener);
        }

        sendAndLogMessage(requestBuilder.toString());
    }

    /**
     * Calls {@link #searchSymbols(SearchField, String, SymbolFilterType, List, MultiMessageListener)} and will
     * accumulate all requested data into an {@link Iterator} which can be consumed later.
     *
     * @return an {@link Iterator} of {@link SymbolSearchResult}s
     *
     * @throws IOException          thrown for {@link IOException}s
     * @throws ExecutionException   thrown for {@link ExecutionException}s
     * @throws InterruptedException thrown for {@link InterruptedException}s
     */
    public Iterator<SymbolSearchResult> searchSymbols(SearchField searchField, String searchString,
            SymbolFilterType symbolFilterType, List<Integer> filterValues)
            throws IOException, ExecutionException, InterruptedException {
        MultiMessageIteratorListener<SymbolSearchResult> asyncListener = new MultiMessageIteratorListener<>();
        searchSymbols(searchField, searchString, symbolFilterType, filterValues, asyncListener);
        return asyncListener.getIterator();
    }

    /**
     * Searches for a list of symbols existing within a specific {@link SearchCodeType} or group of {@link
     * SearchCodeType} codes. This sends a {@link SymbolMarketInfoCommand#SYMBOLS_BY_SIC_CODE} or {@link
     * SymbolMarketInfoCommand#SYMBOLS_BY_NIAC_CODE} request.
     *
     * @param searchCodeType             the {@link SearchCodeType}
     * @param searchString               at least the first 2 digits of an existing {@link SearchCodeType}.
     * @param symbolSearchResultListener the {@link MultiMessageListener} for the requested {@link SymbolSearchResult}s
     *
     * @throws IOException thrown for {@link IOException}s
     */
    public void searchSymbols(SearchCodeType searchCodeType, String searchString,
            MultiMessageListener<SymbolSearchResult> symbolSearchResultListener) throws IOException {
        checkNotNull(searchCodeType);
        checkNotNull(searchString);
        checkNotNull(symbolSearchResultListener);

        checkArgument(searchString.length() >= 2);

        String requestID = requestIDFeedHelper.getNewRequestID();
        StringBuilder requestBuilder = new StringBuilder();

        requestBuilder.append(searchCodeType.value()).append(",");
        requestBuilder.append(searchString).append(",");
        requestBuilder.append(requestID);
        requestBuilder.append(LineEnding.CR_LF.getASCIIString());

        synchronized (messageReceivedLock) {
            symbolSearchResultListenersOfRequestIDs.put(requestID, symbolSearchResultListener);
        }

        sendAndLogMessage(requestBuilder.toString());
    }

    /**
     * Calls {@link #searchSymbols(SearchCodeType, String, MultiMessageListener)} and will accumulate all requested data
     * into an {@link Iterator} which can be consumed later.
     *
     * @return an {@link Iterator} of {@link SymbolSearchResult}s
     *
     * @throws IOException          thrown for {@link IOException}s
     * @throws ExecutionException   thrown for {@link ExecutionException}s
     * @throws InterruptedException thrown for {@link InterruptedException}s
     */
    public Iterator<SymbolSearchResult> searchSymbols(SearchCodeType searchCodeType, String searchString)
            throws IOException, ExecutionException, InterruptedException {
        MultiMessageIteratorListener<SymbolSearchResult> asyncListener = new MultiMessageIteratorListener<>();
        searchSymbols(searchCodeType, searchString, asyncListener);
        return asyncListener.getIterator();
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
     * @throws IOException thrown for {@link IOException}s
     */
    private <T> void requestGenericMultiMessage(String requestCode,
            Map<String, MultiMessageListener<T>> listenersOfRequestIDs, MultiMessageListener<T> listener)
            throws IOException {
        checkNotNull(listener);

        String requestID = requestIDFeedHelper.getNewRequestID();
        StringBuilder requestBuilder = new StringBuilder();

        requestBuilder.append(requestCode).append(",");
        requestBuilder.append(requestID);
        requestBuilder.append(LineEnding.CR_LF.getASCIIString());

        synchronized (messageReceivedLock) {
            listenersOfRequestIDs.put(requestID, listener);
        }

        sendAndLogMessage(requestBuilder.toString());
    }

    /**
     * Request a list of {@link ListedMarket}s from the feed. This sends a
     * {@link SymbolMarketInfoCommand#LISTED_MARKETS}
     * request.
     *
     * @param listedMarketListener the {@link MultiMessageListener} for the requested {@link ListedMarket}s
     *
     * @throws IOException thrown for {@link IOException}s
     */
    public void requestListedMarkets(MultiMessageListener<ListedMarket> listedMarketListener) throws IOException {
        requestGenericMultiMessage(SymbolMarketInfoCommand.LISTED_MARKETS.value(), listedMarketListenersOfRequestIDs,
                listedMarketListener);
    }

    /**
     * Calls {@link #requestListedMarkets(MultiMessageListener)} and will accumulate all requested data into an {@link
     * Iterator} which can be consumed later.
     *
     * @return an {@link Iterator} of {@link ListedMarket}s
     *
     * @throws IOException          thrown for {@link IOException}s
     * @throws ExecutionException   thrown for {@link ExecutionException}s
     * @throws InterruptedException thrown for {@link InterruptedException}s
     */
    public Iterator<ListedMarket> requestListedMarkets() throws IOException, ExecutionException, InterruptedException {
        MultiMessageIteratorListener<ListedMarket> asyncListener = new MultiMessageIteratorListener<>();
        requestListedMarkets(asyncListener);
        return asyncListener.getIterator();
    }

    /**
     * Request a list of {@link SecurityType}s from the feed. This sends a
     * {@link SymbolMarketInfoCommand#SECURITY_TYPES}
     * request.
     *
     * @param securityTypeListener the {@link MultiMessageListener} for the requested {@link SecurityType}s
     *
     * @throws IOException thrown for {@link IOException}s
     */
    public void requestSecurityTypes(MultiMessageListener<SecurityType> securityTypeListener) throws IOException {
        requestGenericMultiMessage(SymbolMarketInfoCommand.SECURITY_TYPES.value(), securityTypeListenersOfRequestIDs,
                securityTypeListener);
    }

    /**
     * Calls {@link #requestSecurityTypes(MultiMessageListener)} and will accumulate all requested data into an {@link
     * Iterator} which can be consumed later.
     *
     * @return an {@link Iterator} of {@link SecurityType}s
     *
     * @throws IOException          thrown for {@link IOException}s
     * @throws ExecutionException   thrown for {@link ExecutionException}s
     * @throws InterruptedException thrown for {@link InterruptedException}s
     */
    public Iterator<SecurityType> requestSecurityTypes() throws IOException, ExecutionException, InterruptedException {
        MultiMessageIteratorListener<SecurityType> asyncListener = new MultiMessageIteratorListener<>();
        requestSecurityTypes(asyncListener);
        return asyncListener.getIterator();
    }

    /**
     * Request a list of {@link TradeCondition}s from the feed. This sends a {@link
     * SymbolMarketInfoCommand#TRADE_CONDITIONS} request.
     *
     * @param tradeConditionListener the {@link MultiMessageListener} for the requested {@link TradeCondition}s
     *
     * @throws IOException thrown for {@link IOException}s
     */
    public void requestTradeConditions(MultiMessageListener<TradeCondition> tradeConditionListener) throws IOException {
        requestGenericMultiMessage(SymbolMarketInfoCommand.TRADE_CONDITIONS.value(),
                tradeConditionListenersOfRequestIDs, tradeConditionListener);
    }

    /**
     * Calls {@link #requestTradeConditions(MultiMessageListener)} and will accumulate all requested data into an {@link
     * Iterator} which can be consumed later.
     *
     * @return an {@link Iterator} of {@link TradeCondition}s
     *
     * @throws IOException          thrown for {@link IOException}s
     * @throws ExecutionException   thrown for {@link ExecutionException}s
     * @throws InterruptedException thrown for {@link InterruptedException}s
     */
    public Iterator<TradeCondition> requestTradeConditions()
            throws IOException, ExecutionException, InterruptedException {
        MultiMessageIteratorListener<TradeCondition> asyncListener = new MultiMessageIteratorListener<>();
        requestTradeConditions(asyncListener);
        return asyncListener.getIterator();
    }

    /**
     * Request a list of {@link SICCode}s from the feed. This sends a {@link SymbolMarketInfoCommand#SIC_CODES}
     * request.
     *
     * @param sicCodeListener the {@link MultiMessageListener} for the requested {@link SICCode}s
     *
     * @throws IOException thrown for {@link IOException}s
     */
    public void requestSICCodes(MultiMessageListener<SICCode> sicCodeListener) throws IOException {
        requestGenericMultiMessage(SymbolMarketInfoCommand.SIC_CODES.value(), sicCodeListenersOfRequestIDs,
                sicCodeListener);
    }

    /**
     * Calls {@link #requestSICCodes(MultiMessageListener)} and will accumulate all requested data into an {@link
     * Iterator} which can be consumed later.
     *
     * @return an {@link Iterator} of {@link SICCode}s
     *
     * @throws IOException          thrown for {@link IOException}s
     * @throws ExecutionException   thrown for {@link ExecutionException}s
     * @throws InterruptedException thrown for {@link InterruptedException}s
     */
    public Iterator<SICCode> requestSICCodes() throws IOException, ExecutionException, InterruptedException {
        MultiMessageIteratorListener<SICCode> asyncListener = new MultiMessageIteratorListener<>();
        requestSICCodes(asyncListener);
        return asyncListener.getIterator();
    }

    /**
     * Request a list of {@link NIACCode}s from the feed. This sends a {@link SymbolMarketInfoCommand#NIAC_CODES}
     * request.
     *
     * @param niacCodeListener the {@link MultiMessageListener} for the requested {@link NIACCode}s
     *
     * @throws IOException thrown for {@link IOException}s
     */
    public void requestNIACCodeCodes(MultiMessageListener<NIACCode> niacCodeListener) throws IOException {
        requestGenericMultiMessage(SymbolMarketInfoCommand.NIAC_CODES.value(), niacCodeListenersOfRequestIDs,
                niacCodeListener);
    }

    /**
     * Calls {@link #requestNIACCodeCodes(MultiMessageListener)} and will accumulate all requested data into an {@link
     * Iterator} which can be consumed later.
     *
     * @return an {@link Iterator} of {@link NIACCode}s
     *
     * @throws IOException          thrown for {@link IOException}s
     * @throws ExecutionException   thrown for {@link ExecutionException}s
     * @throws InterruptedException thrown for {@link InterruptedException}s
     */
    public Iterator<NIACCode> requestNIACCodeCodes() throws IOException, ExecutionException, InterruptedException {
        MultiMessageIteratorListener<NIACCode> asyncListener = new MultiMessageIteratorListener<>();
        requestNIACCodeCodes(asyncListener);
        return asyncListener.getIterator();
    }

    //
    // END Feed commands
    //
}
