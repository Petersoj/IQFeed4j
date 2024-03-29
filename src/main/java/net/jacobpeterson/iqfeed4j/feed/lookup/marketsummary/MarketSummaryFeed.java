package net.jacobpeterson.iqfeed4j.feed.lookup.marketsummary;

import net.jacobpeterson.iqfeed4j.feed.exception.IQFeedRuntimeException;
import net.jacobpeterson.iqfeed4j.feed.exception.NoDataException;
import net.jacobpeterson.iqfeed4j.feed.exception.SyntaxException;
import net.jacobpeterson.iqfeed4j.feed.lookup.AbstractLookupFeed;
import net.jacobpeterson.iqfeed4j.feed.lookup.symbolmarketinfo.SymbolMarketInfoFeed;
import net.jacobpeterson.iqfeed4j.feed.message.MultiMessageAccumulator;
import net.jacobpeterson.iqfeed4j.feed.message.MultiMessageListener;
import net.jacobpeterson.iqfeed4j.model.feed.lookup.marketsummary.EndOfDaySnapshot;
import net.jacobpeterson.iqfeed4j.model.feed.lookup.marketsummary.FiveMinuteSnapshot;
import net.jacobpeterson.iqfeed4j.model.feed.lookup.marketsummary.FundamentalSnapshot;
import net.jacobpeterson.iqfeed4j.model.feed.lookup.marketsummary.enums.MarketSummaryCommand;
import net.jacobpeterson.iqfeed4j.util.csv.mapper.AbstractCSVMapper.DateTimeFormatters;
import net.jacobpeterson.iqfeed4j.util.csv.mapper.map.NamedCSVMapper;
import net.jacobpeterson.iqfeed4j.util.string.LineEnding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static com.google.common.base.Preconditions.checkNotNull;
import static net.jacobpeterson.iqfeed4j.util.csv.CSVUtil.valuePresent;
import static net.jacobpeterson.iqfeed4j.util.csv.mapper.AbstractCSVMapper.DateTimeConverters.DATE;
import static net.jacobpeterson.iqfeed4j.util.csv.mapper.AbstractCSVMapper.DateTimeConverters.OPTIONAL_1_OR_2_DIGIT_HOUR_TIME_NULLABLE;
import static net.jacobpeterson.iqfeed4j.util.csv.mapper.AbstractCSVMapper.PrimitiveConvertors.DOUBLE;
import static net.jacobpeterson.iqfeed4j.util.csv.mapper.AbstractCSVMapper.PrimitiveConvertors.INTEGER;
import static net.jacobpeterson.iqfeed4j.util.csv.mapper.AbstractCSVMapper.PrimitiveConvertors.STRING;

/**
 * {@link MarketSummaryFeed} is an {@link AbstractLookupFeed} for market summary (snapshot) data.
 */
public class MarketSummaryFeed extends AbstractLookupFeed {

    private static final Logger LOGGER = LoggerFactory.getLogger(MarketSummaryFeed.class);
    protected static final String FEED_NAME_SUFFIX = " Market Summary";
    protected static final NamedCSVMapper<EndOfDaySnapshot> END_OF_DAY_SNAPSHOT_CSV_MAPPER;
    protected static final NamedCSVMapper<FundamentalSnapshot> FUNDAMENTAL_SNAPSHOT_CSV_MAPPER;
    protected static final NamedCSVMapper<FiveMinuteSnapshot> FIVE_MINUTE_SNAPSHOT_CSV_MAPPER;

    static {
        END_OF_DAY_SNAPSHOT_CSV_MAPPER = new NamedCSVMapper<>(EndOfDaySnapshot::new);
        END_OF_DAY_SNAPSHOT_CSV_MAPPER.setMapping("Symbol", EndOfDaySnapshot::setSymbol, STRING);
        END_OF_DAY_SNAPSHOT_CSV_MAPPER.setMapping("Exchange", EndOfDaySnapshot::setExchange, INTEGER);
        END_OF_DAY_SNAPSHOT_CSV_MAPPER.setMapping("Type", EndOfDaySnapshot::setType, INTEGER);
        END_OF_DAY_SNAPSHOT_CSV_MAPPER.setMapping("Last", EndOfDaySnapshot::setLast, DOUBLE);
        END_OF_DAY_SNAPSHOT_CSV_MAPPER.setMapping("TradeSize", EndOfDaySnapshot::setTradeSize, DOUBLE);
        END_OF_DAY_SNAPSHOT_CSV_MAPPER.setMapping("TradedMarket", EndOfDaySnapshot::setTradedMarket, INTEGER);
        END_OF_DAY_SNAPSHOT_CSV_MAPPER.setMapping("TradeDate", EndOfDaySnapshot::setTradeDate, DATE);
        END_OF_DAY_SNAPSHOT_CSV_MAPPER.setMapping("TradeTime",
                EndOfDaySnapshot::setTradeTime, OPTIONAL_1_OR_2_DIGIT_HOUR_TIME_NULLABLE);
        END_OF_DAY_SNAPSHOT_CSV_MAPPER.setMapping("Open", EndOfDaySnapshot::setOpen, DOUBLE);
        END_OF_DAY_SNAPSHOT_CSV_MAPPER.setMapping("High", EndOfDaySnapshot::setHigh, DOUBLE);
        END_OF_DAY_SNAPSHOT_CSV_MAPPER.setMapping("Low", EndOfDaySnapshot::setLow, DOUBLE);
        END_OF_DAY_SNAPSHOT_CSV_MAPPER.setMapping("Close", EndOfDaySnapshot::setClose, DOUBLE);
        END_OF_DAY_SNAPSHOT_CSV_MAPPER.setMapping("Bid", EndOfDaySnapshot::setBid, DOUBLE);
        END_OF_DAY_SNAPSHOT_CSV_MAPPER.setMapping("BidMarket", EndOfDaySnapshot::setBidMarket, INTEGER);
        END_OF_DAY_SNAPSHOT_CSV_MAPPER.setMapping("BidSize", EndOfDaySnapshot::setBidSize, DOUBLE);
        END_OF_DAY_SNAPSHOT_CSV_MAPPER.setMapping("Ask", EndOfDaySnapshot::setAsk, DOUBLE);
        END_OF_DAY_SNAPSHOT_CSV_MAPPER.setMapping("AskMarket", EndOfDaySnapshot::setAskMarket, INTEGER);
        END_OF_DAY_SNAPSHOT_CSV_MAPPER.setMapping("AskSize", EndOfDaySnapshot::setAskSize, DOUBLE);
        END_OF_DAY_SNAPSHOT_CSV_MAPPER.setMapping("Volume", EndOfDaySnapshot::setVolume, DOUBLE);
        END_OF_DAY_SNAPSHOT_CSV_MAPPER.setMapping("PDayVolume", EndOfDaySnapshot::setPDayVolume, DOUBLE);
        END_OF_DAY_SNAPSHOT_CSV_MAPPER.setMapping("UpVolume", EndOfDaySnapshot::setUpVolume, DOUBLE);
        END_OF_DAY_SNAPSHOT_CSV_MAPPER.setMapping("DownVolume", EndOfDaySnapshot::setDownVolume, DOUBLE);
        END_OF_DAY_SNAPSHOT_CSV_MAPPER.setMapping("NeutralVolume", EndOfDaySnapshot::setNeutralVolume, DOUBLE);
        END_OF_DAY_SNAPSHOT_CSV_MAPPER.setMapping("TradeCount", EndOfDaySnapshot::setTradeCount, DOUBLE);
        END_OF_DAY_SNAPSHOT_CSV_MAPPER.setMapping("UpTrades", EndOfDaySnapshot::setUpTrades, DOUBLE);
        END_OF_DAY_SNAPSHOT_CSV_MAPPER.setMapping("DownTrades", EndOfDaySnapshot::setDownTrades, DOUBLE);
        END_OF_DAY_SNAPSHOT_CSV_MAPPER.setMapping("NeutralTrades", EndOfDaySnapshot::setNeutralTrades, DOUBLE);
        END_OF_DAY_SNAPSHOT_CSV_MAPPER.setMapping("VWAP", EndOfDaySnapshot::setVwap, DOUBLE);
        END_OF_DAY_SNAPSHOT_CSV_MAPPER.setMapping("MutualDiv", EndOfDaySnapshot::setMutualDiv, DOUBLE);
        END_OF_DAY_SNAPSHOT_CSV_MAPPER.setMapping("SevenDayYield", EndOfDaySnapshot::setSevenDayYield, DOUBLE);
        END_OF_DAY_SNAPSHOT_CSV_MAPPER.setMapping("OpenInterest", EndOfDaySnapshot::setOpenInterest, DOUBLE);
        END_OF_DAY_SNAPSHOT_CSV_MAPPER.setMapping("Settlement", EndOfDaySnapshot::setSettlement, DOUBLE);
        END_OF_DAY_SNAPSHOT_CSV_MAPPER.setMapping("SettlementDate", EndOfDaySnapshot::setSettlementDate, DATE);
        END_OF_DAY_SNAPSHOT_CSV_MAPPER.setMapping("ExpirationDate", EndOfDaySnapshot::setExpirationDate, DATE);
        END_OF_DAY_SNAPSHOT_CSV_MAPPER.setMapping("Strike", EndOfDaySnapshot::setStrike, DOUBLE);

        FUNDAMENTAL_SNAPSHOT_CSV_MAPPER = new NamedCSVMapper<>(FundamentalSnapshot::new);
        FUNDAMENTAL_SNAPSHOT_CSV_MAPPER.setMapping("Symbol", FundamentalSnapshot::setSymbol, STRING);
        FUNDAMENTAL_SNAPSHOT_CSV_MAPPER.setMapping("Description", FundamentalSnapshot::setDescription, STRING);
        FUNDAMENTAL_SNAPSHOT_CSV_MAPPER.setMapping("PeRatio", FundamentalSnapshot::setPeRatio, DOUBLE);
        FUNDAMENTAL_SNAPSHOT_CSV_MAPPER.setMapping("AvgVolume", FundamentalSnapshot::setAvgVolume, DOUBLE);
        FUNDAMENTAL_SNAPSHOT_CSV_MAPPER.setMapping("DivYield", FundamentalSnapshot::setDivYield, DOUBLE);
        FUNDAMENTAL_SNAPSHOT_CSV_MAPPER.setMapping("DivAmount", FundamentalSnapshot::setDivAmount, DOUBLE);
        FUNDAMENTAL_SNAPSHOT_CSV_MAPPER.setMapping("DivRate", FundamentalSnapshot::setDivRate, DOUBLE);
        FUNDAMENTAL_SNAPSHOT_CSV_MAPPER.setMapping("PayDate", FundamentalSnapshot::setPayDate, DATE);
        FUNDAMENTAL_SNAPSHOT_CSV_MAPPER.setMapping("ExDivDate", FundamentalSnapshot::setExDivDate, DATE);
        FUNDAMENTAL_SNAPSHOT_CSV_MAPPER.setMapping("CurrentEps", FundamentalSnapshot::setCurrentEps, DOUBLE);
        FUNDAMENTAL_SNAPSHOT_CSV_MAPPER.setMapping("EstEps", FundamentalSnapshot::setEstEps, DOUBLE);
        FUNDAMENTAL_SNAPSHOT_CSV_MAPPER.setMapping("SIC", FundamentalSnapshot::setSic, INTEGER);
        FUNDAMENTAL_SNAPSHOT_CSV_MAPPER.setMapping("Precision", FundamentalSnapshot::setPrecision, INTEGER);
        FUNDAMENTAL_SNAPSHOT_CSV_MAPPER.setMapping("Display", FundamentalSnapshot::setDisplay, DOUBLE);
        FUNDAMENTAL_SNAPSHOT_CSV_MAPPER.setMapping("GrowthPercent", FundamentalSnapshot::setGrowthPercent, DOUBLE);
        FUNDAMENTAL_SNAPSHOT_CSV_MAPPER.setMapping("FiscalYearEnd", FundamentalSnapshot::setFiscalYearEnd, DATE);
        FUNDAMENTAL_SNAPSHOT_CSV_MAPPER.setMapping("Volatility", FundamentalSnapshot::setVolatility, DOUBLE);
        FUNDAMENTAL_SNAPSHOT_CSV_MAPPER.setMapping("ListedMarket", FundamentalSnapshot::setListedMarket, INTEGER);
        FUNDAMENTAL_SNAPSHOT_CSV_MAPPER.setMapping("MaturityDate", FundamentalSnapshot::setMaturityDate, DATE);
        FUNDAMENTAL_SNAPSHOT_CSV_MAPPER.setMapping("OptionRoots", FundamentalSnapshot::setOptionRoots, STRING);
        FUNDAMENTAL_SNAPSHOT_CSV_MAPPER.setMapping("CouponRate", FundamentalSnapshot::setCouponRate, DOUBLE);
        FUNDAMENTAL_SNAPSHOT_CSV_MAPPER.setMapping("InstitutionalPercent",
                FundamentalSnapshot::setInstitutionalPercent, DOUBLE);
        FUNDAMENTAL_SNAPSHOT_CSV_MAPPER.setMapping("YearEndClose", FundamentalSnapshot::setYearEndClose, DOUBLE);
        FUNDAMENTAL_SNAPSHOT_CSV_MAPPER.setMapping("Beta", FundamentalSnapshot::setBeta, DOUBLE);
        FUNDAMENTAL_SNAPSHOT_CSV_MAPPER.setMapping("LEAPs", FundamentalSnapshot::setLEAPs, STRING);
        FUNDAMENTAL_SNAPSHOT_CSV_MAPPER.setMapping("WRAPs", FundamentalSnapshot::setWRAPs, STRING);
        FUNDAMENTAL_SNAPSHOT_CSV_MAPPER.setMapping("Assets", FundamentalSnapshot::setAssets, DOUBLE);
        FUNDAMENTAL_SNAPSHOT_CSV_MAPPER.setMapping("Liabilities", FundamentalSnapshot::setLiabilities, DOUBLE);
        FUNDAMENTAL_SNAPSHOT_CSV_MAPPER.setMapping("BalanceSheetDate", FundamentalSnapshot::setBalanceSheetDate, DATE);
        FUNDAMENTAL_SNAPSHOT_CSV_MAPPER.setMapping("LongTermDebt", FundamentalSnapshot::setLongTermDebt, DOUBLE);
        FUNDAMENTAL_SNAPSHOT_CSV_MAPPER.setMapping("CommonSharesOutstanding",
                FundamentalSnapshot::setCommonSharesOutstanding, DOUBLE);
        FUNDAMENTAL_SNAPSHOT_CSV_MAPPER.setMapping("MarketCap", FundamentalSnapshot::setMarketCap, DOUBLE);
        FUNDAMENTAL_SNAPSHOT_CSV_MAPPER.setMapping("52WeekHigh", FundamentalSnapshot::set52WeekHigh, DOUBLE);
        FUNDAMENTAL_SNAPSHOT_CSV_MAPPER.setMapping("52WeekHighDate", FundamentalSnapshot::set52WeekHighDate, DATE);
        FUNDAMENTAL_SNAPSHOT_CSV_MAPPER.setMapping("52WeekLow", FundamentalSnapshot::set52WeekLow, DOUBLE);
        FUNDAMENTAL_SNAPSHOT_CSV_MAPPER.setMapping("52WeekLowDate", FundamentalSnapshot::set52WeekLowDate, DATE);
        FUNDAMENTAL_SNAPSHOT_CSV_MAPPER.setMapping("CalHigh", FundamentalSnapshot::setCalHigh, DOUBLE);
        FUNDAMENTAL_SNAPSHOT_CSV_MAPPER.setMapping("CalHighDate", FundamentalSnapshot::setCalHighDate, DATE);
        FUNDAMENTAL_SNAPSHOT_CSV_MAPPER.setMapping("CalLow", FundamentalSnapshot::setCalLow, DOUBLE);
        FUNDAMENTAL_SNAPSHOT_CSV_MAPPER.setMapping("CalLowDate", FundamentalSnapshot::setCalLowDate, DATE);
        FUNDAMENTAL_SNAPSHOT_CSV_MAPPER.setMapping("Expiration", FundamentalSnapshot::setExpiration, STRING);
        FUNDAMENTAL_SNAPSHOT_CSV_MAPPER.setMapping("LastSplit", FundamentalSnapshot::setLastSplit, DOUBLE);
        FUNDAMENTAL_SNAPSHOT_CSV_MAPPER.setMapping("LastSplitDate", FundamentalSnapshot::setLastSplitDate, DATE);
        FUNDAMENTAL_SNAPSHOT_CSV_MAPPER.setMapping("PrevSplit", FundamentalSnapshot::setPrevSplit, DOUBLE);
        FUNDAMENTAL_SNAPSHOT_CSV_MAPPER.setMapping("PrevSplitDate", FundamentalSnapshot::setPrevSplitDate, DATE);
        FUNDAMENTAL_SNAPSHOT_CSV_MAPPER.setMapping("NAICS", FundamentalSnapshot::setNaics, INTEGER);
        FUNDAMENTAL_SNAPSHOT_CSV_MAPPER.setMapping("ShortInterest", FundamentalSnapshot::setShortInterest, DOUBLE);

        FIVE_MINUTE_SNAPSHOT_CSV_MAPPER = new NamedCSVMapper<>(FiveMinuteSnapshot::new);
        FIVE_MINUTE_SNAPSHOT_CSV_MAPPER.setMapping("Symbol", FiveMinuteSnapshot::setSymbol, STRING);
        FIVE_MINUTE_SNAPSHOT_CSV_MAPPER.setMapping("Exchange", FiveMinuteSnapshot::setExchange, INTEGER);
        FIVE_MINUTE_SNAPSHOT_CSV_MAPPER.setMapping("Type", FiveMinuteSnapshot::setType, INTEGER);
        FIVE_MINUTE_SNAPSHOT_CSV_MAPPER.setMapping("Last", FiveMinuteSnapshot::setLast, DOUBLE);
        FIVE_MINUTE_SNAPSHOT_CSV_MAPPER.setMapping("TradeSize", FiveMinuteSnapshot::setTradeSize, DOUBLE);
        FIVE_MINUTE_SNAPSHOT_CSV_MAPPER.setMapping("TradedMarket", FiveMinuteSnapshot::setTradedMarket, INTEGER);
        FIVE_MINUTE_SNAPSHOT_CSV_MAPPER.setMapping("TradeDate", FiveMinuteSnapshot::setTradeDate, DATE);
        FIVE_MINUTE_SNAPSHOT_CSV_MAPPER.setMapping("TradeTime",
                FiveMinuteSnapshot::setTradeTime, OPTIONAL_1_OR_2_DIGIT_HOUR_TIME_NULLABLE);
        FIVE_MINUTE_SNAPSHOT_CSV_MAPPER.setMapping("Open", FiveMinuteSnapshot::setOpen, DOUBLE);
        FIVE_MINUTE_SNAPSHOT_CSV_MAPPER.setMapping("High", FiveMinuteSnapshot::setHigh, DOUBLE);
        FIVE_MINUTE_SNAPSHOT_CSV_MAPPER.setMapping("Low", FiveMinuteSnapshot::setLow, DOUBLE);
        FIVE_MINUTE_SNAPSHOT_CSV_MAPPER.setMapping("Close", FiveMinuteSnapshot::setClose, DOUBLE);
        FIVE_MINUTE_SNAPSHOT_CSV_MAPPER.setMapping("Bid", FiveMinuteSnapshot::setBid, DOUBLE);
        FIVE_MINUTE_SNAPSHOT_CSV_MAPPER.setMapping("BidMarket", FiveMinuteSnapshot::setBidMarket, INTEGER);
        FIVE_MINUTE_SNAPSHOT_CSV_MAPPER.setMapping("BidSize", FiveMinuteSnapshot::setBidSize, DOUBLE);
        FIVE_MINUTE_SNAPSHOT_CSV_MAPPER.setMapping("Ask", FiveMinuteSnapshot::setAsk, DOUBLE);
        FIVE_MINUTE_SNAPSHOT_CSV_MAPPER.setMapping("AskMarket", FiveMinuteSnapshot::setAskMarket, INTEGER);
        FIVE_MINUTE_SNAPSHOT_CSV_MAPPER.setMapping("AskSize", FiveMinuteSnapshot::setAskSize, DOUBLE);
        FIVE_MINUTE_SNAPSHOT_CSV_MAPPER.setMapping("Volume", FiveMinuteSnapshot::setVolume, DOUBLE);
        FIVE_MINUTE_SNAPSHOT_CSV_MAPPER.setMapping("PDayVolume", FiveMinuteSnapshot::setPDayVolume, DOUBLE);
        FIVE_MINUTE_SNAPSHOT_CSV_MAPPER.setMapping("UpVolume", FiveMinuteSnapshot::setUpVolume, DOUBLE);
        FIVE_MINUTE_SNAPSHOT_CSV_MAPPER.setMapping("DownVolume", FiveMinuteSnapshot::setDownVolume, DOUBLE);
        FIVE_MINUTE_SNAPSHOT_CSV_MAPPER.setMapping("NeutralVolume", FiveMinuteSnapshot::setNeutralVolume, DOUBLE);
        FIVE_MINUTE_SNAPSHOT_CSV_MAPPER.setMapping("TradeCount", FiveMinuteSnapshot::setTradeCount, DOUBLE);
        FIVE_MINUTE_SNAPSHOT_CSV_MAPPER.setMapping("UpTrades", FiveMinuteSnapshot::setUpTrades, DOUBLE);
        FIVE_MINUTE_SNAPSHOT_CSV_MAPPER.setMapping("DownTrades", FiveMinuteSnapshot::setDownTrades, DOUBLE);
        FIVE_MINUTE_SNAPSHOT_CSV_MAPPER.setMapping("NeutralTrades", FiveMinuteSnapshot::setNeutralTrades, DOUBLE);
        FIVE_MINUTE_SNAPSHOT_CSV_MAPPER.setMapping("VWAP", FiveMinuteSnapshot::setVwap, DOUBLE);
        FIVE_MINUTE_SNAPSHOT_CSV_MAPPER.setMapping("MutualDiv", FiveMinuteSnapshot::setMutualDiv, DOUBLE);
        FIVE_MINUTE_SNAPSHOT_CSV_MAPPER.setMapping("SevenDayYield", FiveMinuteSnapshot::setSevenDayYield, DOUBLE);
        FIVE_MINUTE_SNAPSHOT_CSV_MAPPER.setMapping("OpenInterest", FiveMinuteSnapshot::setOpenInterest, DOUBLE);
        FIVE_MINUTE_SNAPSHOT_CSV_MAPPER.setMapping("Settlement", FiveMinuteSnapshot::setSettlement, DOUBLE);
        FIVE_MINUTE_SNAPSHOT_CSV_MAPPER.setMapping("SettlementDate", FiveMinuteSnapshot::setSettlementDate, DATE);
        FIVE_MINUTE_SNAPSHOT_CSV_MAPPER.setMapping("ExpirationDate", FiveMinuteSnapshot::setExpirationDate, DATE);
        FIVE_MINUTE_SNAPSHOT_CSV_MAPPER.setMapping("Strike", FiveMinuteSnapshot::setStrike, DOUBLE);
    }

    protected final Object messageReceivedLock;
    private final HashMap<String, MultiMessageListener<EndOfDaySnapshot>> endOfDaySnapshotListenersOfRequestIDs;
    private final HashMap<String, MultiMessageListener<FundamentalSnapshot>> fundamentalSnapshotListenersOfRequestIDs;
    private final HashMap<String, MultiMessageListener<FiveMinuteSnapshot>> fiveMinuteSnapshotListenersOfRequestIDs;
    private final HashMap<String, Map<String, Integer>> csvIndicesOfIndexNamesOfRequestIDs;

    /**
     * Instantiates a new {@link MarketSummaryFeed}.
     *
     * @param marketSummaryFeedName the {@link MarketSummaryFeed} name
     * @param hostname              the hostname
     * @param port                  the port
     */
    public MarketSummaryFeed(String marketSummaryFeedName, String hostname, int port) {
        super(LOGGER, marketSummaryFeedName + FEED_NAME_SUFFIX, hostname, port, QUOTE_ESCAPED_COMMA_DELIMITED_SPLITTER);

        messageReceivedLock = new Object();
        endOfDaySnapshotListenersOfRequestIDs = new HashMap<>();
        fundamentalSnapshotListenersOfRequestIDs = new HashMap<>();
        fiveMinuteSnapshotListenersOfRequestIDs = new HashMap<>();
        csvIndicesOfIndexNamesOfRequestIDs = new HashMap<>();
    }

    @Override
    protected void onMessageReceived(String[] csv) {
        if (isErrorOrInvalidMessage(csv)) {
            return;
        }

        String requestID = csv[0];

        synchronized (messageReceivedLock) {
            if (handleMultiMessage(csv, requestID, endOfDaySnapshotListenersOfRequestIDs,
                    END_OF_DAY_SNAPSHOT_CSV_MAPPER)) {
                return;
            }

            if (handleMultiMessage(csv, requestID, fundamentalSnapshotListenersOfRequestIDs,
                    FUNDAMENTAL_SNAPSHOT_CSV_MAPPER)) {
                return;
            }

            if (handleMultiMessage(csv, requestID, fiveMinuteSnapshotListenersOfRequestIDs,
                    FIVE_MINUTE_SNAPSHOT_CSV_MAPPER)) {
                return;
            }
        }
    }

    /**
     * Handles a message for a {@link MultiMessageListener} by: checking for request error messages, handling 'End of
     * Message' messages, populating {@link #csvIndicesOfIndexNamesOfRequestIDs} as needed, and performing
     * {@link NamedCSVMapper#map(String[], int, Map)} on the <code>csv</code> to call
     * {@link MultiMessageListener#onMessageReceived(Object)}.
     *
     * @param <T>                   the type of {@link MultiMessageListener}
     * @param csv                   the CSV
     * @param requestID             the Request ID
     * @param listenersOfRequestIDs the {@link Map} with the keys being the Request IDs and the values being the
     *                              corresponding {@link MultiMessageListener}s
     * @param namedCSVMapper        the {@link NamedCSVMapper} for the message
     *
     * @return true if the <code>requestID</code> was a key inside <code>listenersOfRequestIDs</code>, false otherwise
     */
    private <T> boolean handleMultiMessage(String[] csv, String requestID,
            HashMap<String, MultiMessageListener<T>> listenersOfRequestIDs, NamedCSVMapper<T> namedCSVMapper) {
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
            csvIndicesOfIndexNamesOfRequestIDs.remove(requestID);
            requestIDFeedHelper.removeRequestID(requestID);
            listener.handleEndOfMultiMessage();
        } else {
            Map<String, Integer> csvIndicesOfIndexNames = csvIndicesOfIndexNamesOfRequestIDs.get(requestID);

            // Since the CSV field names are sent first by IQFeed after a request, if 'csvIndicesOfIndexNames' doesn't
            // already exist, then we know this is the first line and so we can now create the 'csvIndicesOfIndexNames'.
            if (csvIndicesOfIndexNames == null) {
                csvIndicesOfIndexNames = new HashMap<>();
                // Start at 2 to exclude Request ID and 'LM' message identifier
                for (int csvIndex = 2; csvIndex < csv.length; csvIndex++) {
                    if (!valuePresent(csv, csvIndex)) {
                        LOGGER.error("CSV Field Names should always contain a String value, but " +
                                "none exists at index {}!", csvIndex);
                        continue;
                    }

                    csvIndicesOfIndexNames.put(csv[csvIndex], csvIndex);
                }
                csvIndicesOfIndexNamesOfRequestIDs.put(requestID, csvIndicesOfIndexNames);
            } else {
                try {
                    // Offset = 0 since 'if' block above accounts for offsets
                    T messageType = namedCSVMapper.map(csv, 0, csvIndicesOfIndexNames);
                    listener.onMessageReceived(messageType);
                } catch (Exception exception) {
                    listener.onMessageException(exception);
                }
            }
        }

        return true;
    }

    @Override
    protected void onFeedSocketException(Exception exception) {
        endOfDaySnapshotListenersOfRequestIDs.values().forEach(listener -> listener.onMessageException(exception));
        fundamentalSnapshotListenersOfRequestIDs.values().forEach(listener -> listener.onMessageException(exception));
        fiveMinuteSnapshotListenersOfRequestIDs.values().forEach(listener -> listener.onMessageException(exception));
    }

    @Override
    protected void onFeedSocketClose() {
        onFeedSocketException(new RuntimeException("Feed socket closed normally while a request was active!"));
    }

    //
    // START Feed commands
    //

    /**
     * Retrieves {@link EndOfDaySnapshot}s for all symbols in a Security Type and Exchange Group. This sends a
     * {@link MarketSummaryCommand#END_OF_DAY_SUMMARY} request.
     *
     * @param securityType             a number representing the desired security type. (Futures, equities, spots,
     *                                 etc... Values can be found using
     *                                 {@link SymbolMarketInfoFeed#requestSecurityTypes(MultiMessageListener)}).
     * @param groupID                  a number representing the desired exchange group.
     * @param date                     the date of data being requested
     * @param endOfDaySnapshotListener the {@link MultiMessageListener} for the requested {@link EndOfDaySnapshot}s
     *
     * @throws IOException thrown for {@link IOException}s
     */
    public void requestEndOfDaySummary(String securityType, String groupID, LocalDate date,
            MultiMessageListener<EndOfDaySnapshot> endOfDaySnapshotListener) throws IOException {
        checkNotNull(securityType);
        checkNotNull(groupID);
        checkNotNull(date);
        checkNotNull(endOfDaySnapshotListener);

        String requestID = requestIDFeedHelper.getNewRequestID();
        StringBuilder requestBuilder = new StringBuilder();

        requestBuilder.append(MarketSummaryCommand.END_OF_DAY_SUMMARY.value()).append(",");
        requestBuilder.append(securityType).append(",");
        requestBuilder.append(groupID).append(",");
        requestBuilder.append(date.format(DateTimeFormatters.DATE)).append(",");
        requestBuilder.append(requestID);
        requestBuilder.append(LineEnding.CR_LF.getASCIIString());

        synchronized (messageReceivedLock) {
            endOfDaySnapshotListenersOfRequestIDs.put(requestID, endOfDaySnapshotListener);
        }

        sendAndLogMessage(requestBuilder.toString());
    }

    /**
     * Calls {@link #requestEndOfDaySummary(String, String, LocalDate, MultiMessageListener)} and will accumulate all
     * requested data into a {@link List} which can be consumed later.
     *
     * @return a {@link List} of {@link EndOfDaySnapshot}s
     *
     * @throws IOException          thrown for {@link IOException}s
     * @throws ExecutionException   thrown for {@link ExecutionException}s
     * @throws InterruptedException thrown for {@link InterruptedException}s
     */
    public List<EndOfDaySnapshot> requestEndOfDaySummary(String securityType, String groupID, LocalDate date)
            throws IOException, ExecutionException, InterruptedException {
        MultiMessageAccumulator<EndOfDaySnapshot> asyncListener = new MultiMessageAccumulator<>();
        requestEndOfDaySummary(securityType, groupID, date, asyncListener);
        return asyncListener.getMessages();
    }

    /**
     * Retrieves {@link FundamentalSnapshot}s for all symbols in a Security Type and Exchange Group. This sends a
     * {@link MarketSummaryCommand#FUNDAMENTAL_SUMMARY} request.
     *
     * @param securityType                a number representing the desired security type. (Futures, equities, spots,
     *                                    etc... Values can be found using
     *                                    {@link SymbolMarketInfoFeed#requestSecurityTypes(MultiMessageListener)}).
     * @param groupID                     a number representing the desired exchange group.
     * @param date                        the date of data being requested
     * @param fundamentalSnapshotListener the {@link MultiMessageListener} for the requested
     *                                    {@link FundamentalSnapshot}s
     *
     * @throws IOException thrown for {@link IOException}s
     */
    public void requestFundamentalSummary(String securityType, String groupID, LocalDate date,
            MultiMessageListener<FundamentalSnapshot> fundamentalSnapshotListener) throws IOException {
        checkNotNull(securityType);
        checkNotNull(groupID);
        checkNotNull(date);
        checkNotNull(fundamentalSnapshotListener);

        String requestID = requestIDFeedHelper.getNewRequestID();
        StringBuilder requestBuilder = new StringBuilder();

        requestBuilder.append(MarketSummaryCommand.FUNDAMENTAL_SUMMARY.value()).append(",");
        requestBuilder.append(securityType).append(",");
        requestBuilder.append(groupID).append(",");
        requestBuilder.append(date.format(DateTimeFormatters.DATE)).append(",");
        requestBuilder.append(requestID);
        requestBuilder.append(LineEnding.CR_LF.getASCIIString());

        synchronized (messageReceivedLock) {
            fundamentalSnapshotListenersOfRequestIDs.put(requestID, fundamentalSnapshotListener);
        }

        sendAndLogMessage(requestBuilder.toString());
    }

    /**
     * Calls {@link #requestFundamentalSummary(String, String, LocalDate, MultiMessageListener)} and will accumulate all
     * requested data into a {@link List} which can be consumed later.
     *
     * @return a {@link List} of {@link FundamentalSnapshot}s
     *
     * @throws IOException          thrown for {@link IOException}s
     * @throws ExecutionException   thrown for {@link ExecutionException}s
     * @throws InterruptedException thrown for {@link InterruptedException}s
     */
    public List<FundamentalSnapshot> requestFundamentalSummary(String securityType, String groupID, LocalDate date)
            throws IOException, ExecutionException, InterruptedException {
        MultiMessageAccumulator<FundamentalSnapshot> asyncListener = new MultiMessageAccumulator<>();
        requestFundamentalSummary(securityType, groupID, date, asyncListener);
        return asyncListener.getMessages();
    }

    /**
     * Retrieves a snapshot of the current market data for all symbols in a Security Type and Exchange Group. NOTE: The
     * timing of the snapshot is not guaranteed, but data will be gathered every 5 minutes. This sends a
     * {@link MarketSummaryCommand#FIVE_MINUTE_SNAPSHOT} request.
     *
     * @param securityType               a number representing the desired security type. (Futures, equities, spots,
     *                                   etc... Values can be found using
     *                                   {@link SymbolMarketInfoFeed#requestSecurityTypes(MultiMessageListener)}).
     * @param groupID                    a number representing the desired exchange group.
     * @param fiveMinuteSnapshotListener the {@link MultiMessageListener} for the requested {@link FiveMinuteSnapshot}s
     *
     * @throws IOException thrown for {@link IOException}s
     */
    public void request5MinuteSummary(String securityType, String groupID,
            MultiMessageListener<FiveMinuteSnapshot> fiveMinuteSnapshotListener) throws IOException {
        checkNotNull(securityType);
        checkNotNull(groupID);
        checkNotNull(fiveMinuteSnapshotListener);

        String requestID = requestIDFeedHelper.getNewRequestID();
        StringBuilder requestBuilder = new StringBuilder();

        requestBuilder.append(MarketSummaryCommand.FIVE_MINUTE_SNAPSHOT.value()).append(",");
        requestBuilder.append(securityType).append(",");
        requestBuilder.append(groupID).append(",");
        requestBuilder.append(requestID);
        requestBuilder.append(LineEnding.CR_LF.getASCIIString());

        synchronized (messageReceivedLock) {
            fiveMinuteSnapshotListenersOfRequestIDs.put(requestID, fiveMinuteSnapshotListener);
        }

        sendAndLogMessage(requestBuilder.toString());
    }

    /**
     * Calls {@link #request5MinuteSummary(String, String, MultiMessageListener)} and will accumulate all requested data
     * into a {@link List} which can be consumed later.
     *
     * @return a {@link List} of {@link FiveMinuteSnapshot}s
     *
     * @throws IOException          thrown for {@link IOException}s
     * @throws ExecutionException   thrown for {@link ExecutionException}s
     * @throws InterruptedException thrown for {@link InterruptedException}s
     */
    public List<FiveMinuteSnapshot> request5MinuteSummary(String securityType, String groupID)
            throws IOException, ExecutionException, InterruptedException {
        MultiMessageAccumulator<FiveMinuteSnapshot> asyncListener = new MultiMessageAccumulator<>();
        request5MinuteSummary(securityType, groupID, asyncListener);
        return asyncListener.getMessages();
    }

    //
    // END Feed commands
    //
}
