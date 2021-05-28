package net.jacobpeterson.iqfeed4j.feed.lookup.marketsummary;

import com.google.common.base.Preconditions;
import net.jacobpeterson.iqfeed4j.feed.MultiMessageListener;
import net.jacobpeterson.iqfeed4j.feed.lookup.AbstractLookupFeed;
import net.jacobpeterson.iqfeed4j.feed.lookup.symbolmarketinfo.SymbolMarketInfoFeed;
import net.jacobpeterson.iqfeed4j.model.lookup.marketsummary.EndOfDaySnapshot;
import net.jacobpeterson.iqfeed4j.model.lookup.marketsummary.FiveMinuteSnapshot;
import net.jacobpeterson.iqfeed4j.model.lookup.marketsummary.FundamentalSnapshot;
import net.jacobpeterson.iqfeed4j.util.csv.mapper.CSVMapper.DateTimeFormatters;
import net.jacobpeterson.iqfeed4j.util.csv.mapper.NamedCSVMapper;
import net.jacobpeterson.iqfeed4j.util.exception.IQFeedException;
import net.jacobpeterson.iqfeed4j.util.exception.NoDataException;
import net.jacobpeterson.iqfeed4j.util.exception.SyntaxException;
import net.jacobpeterson.iqfeed4j.util.string.LineEnding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static net.jacobpeterson.iqfeed4j.util.csv.CSVUtil.valuePresent;
import static net.jacobpeterson.iqfeed4j.util.csv.mapper.CSVMapper.DateTimeConverters.DATE;
import static net.jacobpeterson.iqfeed4j.util.csv.mapper.CSVMapper.DateTimeConverters.TIME;
import static net.jacobpeterson.iqfeed4j.util.csv.mapper.CSVMapper.PrimitiveConvertors.DOUBLE;
import static net.jacobpeterson.iqfeed4j.util.csv.mapper.CSVMapper.PrimitiveConvertors.INT;
import static net.jacobpeterson.iqfeed4j.util.csv.mapper.CSVMapper.PrimitiveConvertors.STRING;

/**
 * {@link MarketSummaryFeed} is an {@link AbstractLookupFeed} for market summary (snapshot) data.
 */
public class MarketSummaryFeed extends AbstractLookupFeed {

    private static final Logger LOGGER = LoggerFactory.getLogger(MarketSummaryFeed.class);
    private static final String FEED_NAME_SUFFIX = " Market Summary";
    private static final NamedCSVMapper<EndOfDaySnapshot> END_OF_DAY_SNAPSHOT_CSV_MAPPER;
    private static final NamedCSVMapper<FundamentalSnapshot> FUNDAMENTAL_SNAPSHOT_CSV_MAPPER;
    private static final NamedCSVMapper<FiveMinuteSnapshot> FIVE_MINUTE_SNAPSHOT_CSV_MAPPER;

    static {
        END_OF_DAY_SNAPSHOT_CSV_MAPPER = new NamedCSVMapper<>(EndOfDaySnapshot::new);
        END_OF_DAY_SNAPSHOT_CSV_MAPPER.setMapping("Symbol", EndOfDaySnapshot::setSymbol, STRING);
        END_OF_DAY_SNAPSHOT_CSV_MAPPER.setMapping("Exchange", EndOfDaySnapshot::setExchange, INT);
        END_OF_DAY_SNAPSHOT_CSV_MAPPER.setMapping("Type", EndOfDaySnapshot::setType, INT);
        END_OF_DAY_SNAPSHOT_CSV_MAPPER.setMapping("Last", EndOfDaySnapshot::setLast, DOUBLE);
        END_OF_DAY_SNAPSHOT_CSV_MAPPER.setMapping("TradeSize", EndOfDaySnapshot::setTradeSize, DOUBLE);
        END_OF_DAY_SNAPSHOT_CSV_MAPPER.setMapping("TradedMarket", EndOfDaySnapshot::setTradedMarket, INT);
        END_OF_DAY_SNAPSHOT_CSV_MAPPER.setMapping("TradeDate", EndOfDaySnapshot::setTradeDate, DATE);
        END_OF_DAY_SNAPSHOT_CSV_MAPPER.setMapping("TradeTime", EndOfDaySnapshot::setTradeTime, TIME);
        END_OF_DAY_SNAPSHOT_CSV_MAPPER.setMapping("Open", EndOfDaySnapshot::setOpen, DOUBLE);
        END_OF_DAY_SNAPSHOT_CSV_MAPPER.setMapping("High", EndOfDaySnapshot::setHigh, DOUBLE);
        END_OF_DAY_SNAPSHOT_CSV_MAPPER.setMapping("Low", EndOfDaySnapshot::setLow, DOUBLE);
        END_OF_DAY_SNAPSHOT_CSV_MAPPER.setMapping("Close", EndOfDaySnapshot::setClose, DOUBLE);
        END_OF_DAY_SNAPSHOT_CSV_MAPPER.setMapping("Bid", EndOfDaySnapshot::setBid, DOUBLE);
        END_OF_DAY_SNAPSHOT_CSV_MAPPER.setMapping("BidMarket", EndOfDaySnapshot::setBidMarket, INT);
        END_OF_DAY_SNAPSHOT_CSV_MAPPER.setMapping("BidSize", EndOfDaySnapshot::setBidSize, DOUBLE);
        END_OF_DAY_SNAPSHOT_CSV_MAPPER.setMapping("Ask", EndOfDaySnapshot::setAsk, DOUBLE);
        END_OF_DAY_SNAPSHOT_CSV_MAPPER.setMapping("AskMarket", EndOfDaySnapshot::setAskMarket, INT);
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
        FUNDAMENTAL_SNAPSHOT_CSV_MAPPER.setMapping("SIC", FundamentalSnapshot::setSic, INT);
        FUNDAMENTAL_SNAPSHOT_CSV_MAPPER.setMapping("Precision", FundamentalSnapshot::setPrecision, INT);
        FUNDAMENTAL_SNAPSHOT_CSV_MAPPER.setMapping("Display", FundamentalSnapshot::setDisplay, DOUBLE);
        FUNDAMENTAL_SNAPSHOT_CSV_MAPPER.setMapping("GrowthPercent", FundamentalSnapshot::setGrowthPercent, DOUBLE);
        FUNDAMENTAL_SNAPSHOT_CSV_MAPPER.setMapping("FiscalYearEnd", FundamentalSnapshot::setFiscalYearEnd, DATE);
        FUNDAMENTAL_SNAPSHOT_CSV_MAPPER.setMapping("Volatility", FundamentalSnapshot::setVolatility, DOUBLE);
        FUNDAMENTAL_SNAPSHOT_CSV_MAPPER.setMapping("ListedMarket", FundamentalSnapshot::setListedMarket, INT);
        FUNDAMENTAL_SNAPSHOT_CSV_MAPPER.setMapping("MaturityDate", FundamentalSnapshot::setMaturityDate, DATE);
        FUNDAMENTAL_SNAPSHOT_CSV_MAPPER.setMapping("OptionRoots", FundamentalSnapshot::setOptionRoots, STRING);
        FUNDAMENTAL_SNAPSHOT_CSV_MAPPER.setMapping("CouponRate", FundamentalSnapshot::setCouponRate, DOUBLE);
        FUNDAMENTAL_SNAPSHOT_CSV_MAPPER
                .setMapping("InstitutionalPercent", FundamentalSnapshot::setInstitutionalPercent, DOUBLE);
        FUNDAMENTAL_SNAPSHOT_CSV_MAPPER.setMapping("YearEndClose", FundamentalSnapshot::setYearEndClose, DOUBLE);
        FUNDAMENTAL_SNAPSHOT_CSV_MAPPER.setMapping("Beta", FundamentalSnapshot::setBeta, DOUBLE);
        FUNDAMENTAL_SNAPSHOT_CSV_MAPPER.setMapping("LEAPs", FundamentalSnapshot::setLEAPs, STRING);
        FUNDAMENTAL_SNAPSHOT_CSV_MAPPER.setMapping("WRAPs", FundamentalSnapshot::setWRAPs, STRING);
        FUNDAMENTAL_SNAPSHOT_CSV_MAPPER.setMapping("Assets", FundamentalSnapshot::setAssets, DOUBLE);
        FUNDAMENTAL_SNAPSHOT_CSV_MAPPER.setMapping("Liabilities", FundamentalSnapshot::setLiabilities, DOUBLE);
        FUNDAMENTAL_SNAPSHOT_CSV_MAPPER.setMapping("BalanceSheetDate", FundamentalSnapshot::setBalanceSheetDate, DATE);
        FUNDAMENTAL_SNAPSHOT_CSV_MAPPER.setMapping("LongTermDebt", FundamentalSnapshot::setLongTermDebt, DOUBLE);
        FUNDAMENTAL_SNAPSHOT_CSV_MAPPER
                .setMapping("CommonSharesOutstanding", FundamentalSnapshot::setCommonSharesOutstanding, DOUBLE);
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
        FUNDAMENTAL_SNAPSHOT_CSV_MAPPER.setMapping("NAICS", FundamentalSnapshot::setNaics, INT);
        FUNDAMENTAL_SNAPSHOT_CSV_MAPPER.setMapping("ShortInterest", FundamentalSnapshot::setShortInterest, DOUBLE);

        FIVE_MINUTE_SNAPSHOT_CSV_MAPPER = new NamedCSVMapper<>(FiveMinuteSnapshot::new);
        FIVE_MINUTE_SNAPSHOT_CSV_MAPPER.setMapping("Symbol", FiveMinuteSnapshot::setSymbol, STRING);
        FIVE_MINUTE_SNAPSHOT_CSV_MAPPER.setMapping("Exchange", FiveMinuteSnapshot::setExchange, INT);
        FIVE_MINUTE_SNAPSHOT_CSV_MAPPER.setMapping("Type", FiveMinuteSnapshot::setType, INT);
        FIVE_MINUTE_SNAPSHOT_CSV_MAPPER.setMapping("Last", FiveMinuteSnapshot::setLast, DOUBLE);
        FIVE_MINUTE_SNAPSHOT_CSV_MAPPER.setMapping("TradeSize", FiveMinuteSnapshot::setTradeSize, DOUBLE);
        FIVE_MINUTE_SNAPSHOT_CSV_MAPPER.setMapping("TradedMarket", FiveMinuteSnapshot::setTradedMarket, INT);
        FIVE_MINUTE_SNAPSHOT_CSV_MAPPER.setMapping("TradeDate", FiveMinuteSnapshot::setTradeDate, DATE);
        FIVE_MINUTE_SNAPSHOT_CSV_MAPPER.setMapping("TradeTime", FiveMinuteSnapshot::setTradeTime, TIME);
        FIVE_MINUTE_SNAPSHOT_CSV_MAPPER.setMapping("Open", FiveMinuteSnapshot::setOpen, DOUBLE);
        FIVE_MINUTE_SNAPSHOT_CSV_MAPPER.setMapping("High", FiveMinuteSnapshot::setHigh, DOUBLE);
        FIVE_MINUTE_SNAPSHOT_CSV_MAPPER.setMapping("Low", FiveMinuteSnapshot::setLow, DOUBLE);
        FIVE_MINUTE_SNAPSHOT_CSV_MAPPER.setMapping("Close", FiveMinuteSnapshot::setClose, DOUBLE);
        FIVE_MINUTE_SNAPSHOT_CSV_MAPPER.setMapping("Bid", FiveMinuteSnapshot::setBid, DOUBLE);
        FIVE_MINUTE_SNAPSHOT_CSV_MAPPER.setMapping("BidMarket", FiveMinuteSnapshot::setBidMarket, INT);
        FIVE_MINUTE_SNAPSHOT_CSV_MAPPER.setMapping("BidSize", FiveMinuteSnapshot::setBidSize, DOUBLE);
        FIVE_MINUTE_SNAPSHOT_CSV_MAPPER.setMapping("Ask", FiveMinuteSnapshot::setAsk, DOUBLE);
        FIVE_MINUTE_SNAPSHOT_CSV_MAPPER.setMapping("AskMarket", FiveMinuteSnapshot::setAskMarket, INT);
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
        super(marketSummaryFeedName + FEED_NAME_SUFFIX, hostname, port, QUOTE_ESCAPED_COMMA_DELIMITED_SPLITTER);

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
     * Message' messages, populating {@link #csvIndicesOfIndexNamesOfRequestIDs} as needed, and performing {@link
     * NamedCSVMapper#map(String[], int, Map)} on the 'CSV' to call
     * {@link MultiMessageListener#onMessageReceived(Object)}.
     *
     * @param <T>                   the type of {@link MultiMessageListener}
     * @param csv                   the CSV
     * @param requestID             the Request ID
     * @param listenersOfRequestIDs the {@link Map} with the keys being the Request IDs and the values being the
     *                              corresponding {@link MultiMessageListener}s
     * @param namedCSVMapper        the {@link NamedCSVMapper} for the message
     *
     * @return true if the 'requestID' was a key inside 'listenersOfRequestIDs', false otherwise
     */
    private <T> boolean handleMultiMessage(String[] csv, String requestID,
            HashMap<String, MultiMessageListener<T>> listenersOfRequestIDs, NamedCSVMapper<T> namedCSVMapper) {
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
            csvIndicesOfIndexNamesOfRequestIDs.remove(requestID);
            removeRequestID(requestID);
            listener.onEndOfMultiMessage();
        } else {
            Map<String, Integer> csvIndicesOfIndexNames = csvIndicesOfIndexNamesOfRequestIDs.get(requestID);

            // Since the CSV field names are sent first by IQFeed after a request, if 'csvIndicesOfIndexNames' doesn't
            // already exist, then we know this is the first line and so we can now create the 'csvIndicesOfIndexNames'.
            if (csvIndicesOfIndexNames == null) {
                csvIndicesOfIndexNames = new HashMap<>();
                // Start at 1 to exclude Request ID
                for (int csvIndex = 1; csvIndex < csv.length; csvIndex++) {
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
                    // Offset = 0 since above 'if' block accounts for Request ID offset
                    T messageType = namedCSVMapper.map(csv, 0, csvIndicesOfIndexNames);
                    listener.onMessageReceived(messageType);
                } catch (Exception exception) {
                    listener.onMessageException(exception);
                }
            }
        }

        return true;
    }

    //
    // START Feed commands
    //

    /**
     * Retrieves {@link EndOfDaySnapshot}s for all symbols in a Security Type and Exchange Group. This sends an EDS
     * request. This method is thread-safe.
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
        Preconditions.checkNotNull(securityType);
        Preconditions.checkNotNull(groupID);
        Preconditions.checkNotNull(date);
        Preconditions.checkNotNull(endOfDaySnapshotListener);

        String requestID = getNewRequestID();
        StringBuilder requestBuilder = new StringBuilder();

        requestBuilder.append("EDS").append(",");
        requestBuilder.append(securityType).append(",");
        requestBuilder.append(groupID).append(",");
        requestBuilder.append(date.format(DateTimeFormatters.DATE)).append(",");
        requestBuilder.append(requestID);
        requestBuilder.append(LineEnding.CR_LF.getASCIIString());

        synchronized (messageReceivedLock) {
            endOfDaySnapshotListenersOfRequestIDs.put(requestID, endOfDaySnapshotListener);
        }
        String requestString = requestBuilder.toString();
        LOGGER.debug("Sending request: {}", requestString);
        sendMessage(requestString);
    }

    /**
     * Retrieves {@link FundamentalSnapshot}s for all symbols in a Security Type and Exchange Group. This sends an FDS
     * request. This method is thread-safe.
     *
     * @param securityType                a number representing the desired security type. (Futures, equities, spots,
     *                                    etc... Values can be found using
     *                                    {@link SymbolMarketInfoFeed#requestSecurityTypes(MultiMessageListener)}).
     * @param groupID                     a number representing the desired exchange group.
     * @param date                        the date of data being requested
     * @param fundamentalSnapshotListener the {@link MultiMessageListener} for the requested {@link
     *                                    FundamentalSnapshot}s
     *
     * @throws IOException thrown for {@link IOException}s
     */
    public void requestFundamentalSummary(String securityType, String groupID, LocalDate date,
            MultiMessageListener<FundamentalSnapshot> fundamentalSnapshotListener) throws IOException {
        Preconditions.checkNotNull(securityType);
        Preconditions.checkNotNull(groupID);
        Preconditions.checkNotNull(date);
        Preconditions.checkNotNull(fundamentalSnapshotListener);

        String requestID = getNewRequestID();
        StringBuilder requestBuilder = new StringBuilder();

        requestBuilder.append("FDS").append(",");
        requestBuilder.append(securityType).append(",");
        requestBuilder.append(groupID).append(",");
        requestBuilder.append(date.format(DateTimeFormatters.DATE)).append(",");
        requestBuilder.append(requestID);
        requestBuilder.append(LineEnding.CR_LF.getASCIIString());

        synchronized (messageReceivedLock) {
            fundamentalSnapshotListenersOfRequestIDs.put(requestID, fundamentalSnapshotListener);
        }
        String requestString = requestBuilder.toString();
        LOGGER.debug("Sending request: {}", requestString);
        sendMessage(requestString);
    }

    /**
     * Retrieves a snapshot of the current market data for all symbols in a Security Type and Exchange Group. NOTE: The
     * timing of the snapshot is not guaranteed, but data will be gathered every 5 minutes. This sends an 5MS request.
     * This method is thread-safe.
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
        Preconditions.checkNotNull(securityType);
        Preconditions.checkNotNull(groupID);
        Preconditions.checkNotNull(fiveMinuteSnapshotListener);

        String requestID = getNewRequestID();
        StringBuilder requestBuilder = new StringBuilder();

        requestBuilder.append("5MS").append(",");
        requestBuilder.append(securityType).append(",");
        requestBuilder.append(groupID).append(",");
        requestBuilder.append(requestID);
        requestBuilder.append(LineEnding.CR_LF.getASCIIString());

        synchronized (messageReceivedLock) {
            fiveMinuteSnapshotListenersOfRequestIDs.put(requestID, fiveMinuteSnapshotListener);
        }
        String requestString = requestBuilder.toString();
        LOGGER.debug("Sending request: {}", requestString);
        sendMessage(requestString);
    }

    //
    // END Feed commands
    //
}
