package net.jacobpeterson.iqfeed4j.feed.streaming.level1;

import net.jacobpeterson.iqfeed4j.feed.message.FeedMessageListener;
import net.jacobpeterson.iqfeed4j.feed.message.SingleMessageFuture;
import net.jacobpeterson.iqfeed4j.feed.streaming.AbstractServerConnectionFeed;
import net.jacobpeterson.iqfeed4j.feed.streaming.StreamingCSVMappers;
import net.jacobpeterson.iqfeed4j.model.feed.common.enums.FeedCommand;
import net.jacobpeterson.iqfeed4j.model.feed.common.enums.FeedMessageType;
import net.jacobpeterson.iqfeed4j.model.feed.streaming.common.FeedStatistics;
import net.jacobpeterson.iqfeed4j.model.feed.streaming.level1.*;
import net.jacobpeterson.iqfeed4j.model.feed.streaming.level1.CustomerInformation.ServiceType;
import net.jacobpeterson.iqfeed4j.model.feed.streaming.level1.FundamentalData.OptionsMultipleDeliverables;
import net.jacobpeterson.iqfeed4j.model.feed.streaming.level1.SummaryUpdate.MarketOpen;
import net.jacobpeterson.iqfeed4j.model.feed.streaming.level1.SummaryUpdate.MostRecentTradeAggressor;
import net.jacobpeterson.iqfeed4j.model.feed.streaming.level1.SummaryUpdate.RestrictedCode;
import net.jacobpeterson.iqfeed4j.model.feed.streaming.level1.enums.*;
import net.jacobpeterson.iqfeed4j.util.csv.mapper.CSVMapping;
import net.jacobpeterson.iqfeed4j.util.csv.mapper.index.DirectIndexCSVMapper;
import net.jacobpeterson.iqfeed4j.util.csv.mapper.index.IndexCSVMapper;
import net.jacobpeterson.iqfeed4j.util.csv.mapper.list.AbstractListCSVMapper;
import net.jacobpeterson.iqfeed4j.util.csv.mapper.list.DirectListCSVMapper;
import net.jacobpeterson.iqfeed4j.util.csv.mapper.list.ListCSVMapper;
import net.jacobpeterson.iqfeed4j.util.string.LineEnding;
import net.jacobpeterson.iqfeed4j.util.tradecondition.TradeConditionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static net.jacobpeterson.iqfeed4j.feed.streaming.level1.Level1Feed.CSVPOJOPopulators.splitFactorAndDate;
import static net.jacobpeterson.iqfeed4j.model.feed.streaming.level1.enums.SummaryUpdateField.*;
import static net.jacobpeterson.iqfeed4j.util.csv.CSVUtil.valueEquals;
import static net.jacobpeterson.iqfeed4j.util.csv.CSVUtil.valueExists;
import static net.jacobpeterson.iqfeed4j.util.csv.CSVUtil.valuePresent;
import static net.jacobpeterson.iqfeed4j.util.csv.mapper.AbstractCSVMapper.DateTimeConverters.*;
import static net.jacobpeterson.iqfeed4j.util.csv.mapper.AbstractCSVMapper.PrimitiveConvertors.DOUBLE;
import static net.jacobpeterson.iqfeed4j.util.csv.mapper.AbstractCSVMapper.PrimitiveConvertors.INTEGER;
import static net.jacobpeterson.iqfeed4j.util.csv.mapper.AbstractCSVMapper.PrimitiveConvertors.STRING;

/**
 * {@link Level1Feed} is an {@link AbstractServerConnectionFeed} for Level 1 market data.
 */
public class Level1Feed extends AbstractServerConnectionFeed {

    /**
     * {@link CSVPOJOPopulators} contains static functions with two arguments: the first being a POJO instance, and the
     * second being a CSV {@link String} value.
     */
    public static class CSVPOJOPopulators {

        /**
         * This populates {@link FundamentalData} "Split" factor and date from a CSV {@link String} value. Note this
         * could throw a variety of {@link Exception}s.
         *
         * @param FundamentalData the {@link FundamentalData}
         * @param csvValue        the CSV value
         */
        public static void splitFactorAndDate(FundamentalData FundamentalData, String csvValue,
                BiConsumer<FundamentalData, Double> factorConsumer,
                BiConsumer<FundamentalData, LocalDate> dateConsumer) {
            String[] spaceSplit = csvValue.split(" ");
            factorConsumer.accept(FundamentalData, Double.parseDouble(spaceSplit[0]));
            dateConsumer.accept(FundamentalData, SLASHED_DATE.apply(spaceSplit[1]));
        }
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(Level1Feed.class);
    protected static final String FEED_NAME_SUFFIX = " Level 1 Feed";
    protected static final DirectListCSVMapper<String> STRING_LIST_CSV_MAPPER;
    protected static final HashMap<SummaryUpdateField, CSVMapping<SummaryUpdate, ?>>
            CSV_MAPPINGS_OF_SUMMARY_UPDATE_FIELDS;
    protected static final IndexCSVMapper<FundamentalData> FUNDAMENTAL_DATA_CSV_MAPPER;
    protected static final IndexCSVMapper<RegionalQuote> REGIONAL_QUOTE_CSV_MAPPER;
    protected static final IndexCSVMapper<NewsHeadline> NEWS_HEADLINE_CSV_MAPPER;
    protected static final IndexCSVMapper<CustomerInformation> CUSTOMER_INFORMATION_CSV_MAPPER;
    protected static final DirectListCSVMapper<SummaryUpdateField> SUMMARY_UPDATE_FIELDS_CSV_MAPPER;
    protected static final DirectListCSVMapper<LogLevel> LOG_LEVELS_CSV_MAPPER;
    protected static final DirectIndexCSVMapper<LocalDateTime> TIMESTAMP_CSV_MAPPER;

    static {
        STRING_LIST_CSV_MAPPER = new DirectListCSVMapper<>(ArrayList::new, STRING);

        // We aren't using 'NamedCSVMapper' since it's slower due to traversing two 'Maps' so instead, we are
        // storing 'CSVMapping's based on 'SummaryUpdateField's due to "Dynamic Fieldsets" that can be set.
        CSV_MAPPINGS_OF_SUMMARY_UPDATE_FIELDS = new HashMap<>();
        CSV_MAPPINGS_OF_SUMMARY_UPDATE_FIELDS.put(SEVEN_DAY_YIELD,
                new CSVMapping<>(SummaryUpdate::set7DayYield, DOUBLE));
        CSV_MAPPINGS_OF_SUMMARY_UPDATE_FIELDS.put(ASK,
                new CSVMapping<>(SummaryUpdate::setAsk, DOUBLE));
        CSV_MAPPINGS_OF_SUMMARY_UPDATE_FIELDS.put(ASK_CHANGE,
                new CSVMapping<>(SummaryUpdate::setAskChange, DOUBLE));
        CSV_MAPPINGS_OF_SUMMARY_UPDATE_FIELDS.put(ASK_MARKET_CENTER,
                new CSVMapping<>(SummaryUpdate::setAskMarketCenter, INTEGER));
        CSV_MAPPINGS_OF_SUMMARY_UPDATE_FIELDS.put(ASK_SIZE,
                new CSVMapping<>(SummaryUpdate::setAskSize, INTEGER));
        CSV_MAPPINGS_OF_SUMMARY_UPDATE_FIELDS.put(ASK_TIME,
                new CSVMapping<>(SummaryUpdate::setAskTime, COLON_TIME));
        CSV_MAPPINGS_OF_SUMMARY_UPDATE_FIELDS.put(AVAILABLE_REGIONS,
                new CSVMapping<>(SummaryUpdate::setAvailableRegions, STRING));
        CSV_MAPPINGS_OF_SUMMARY_UPDATE_FIELDS.put(AVERAGE_MATURITY,
                new CSVMapping<>(SummaryUpdate::setAverageMaturity, DOUBLE));
        CSV_MAPPINGS_OF_SUMMARY_UPDATE_FIELDS.put(BID,
                new CSVMapping<>(SummaryUpdate::setBid, DOUBLE));
        CSV_MAPPINGS_OF_SUMMARY_UPDATE_FIELDS.put(BID_CHANGE,
                new CSVMapping<>(SummaryUpdate::setBidChange, DOUBLE));
        CSV_MAPPINGS_OF_SUMMARY_UPDATE_FIELDS.put(BID_MARKET_CENTER,
                new CSVMapping<>(SummaryUpdate::setBidMarketCenter, INTEGER));
        CSV_MAPPINGS_OF_SUMMARY_UPDATE_FIELDS.put(BID_SIZE,
                new CSVMapping<>(SummaryUpdate::setBidSize, INTEGER));
        CSV_MAPPINGS_OF_SUMMARY_UPDATE_FIELDS.put(BID_TIME,
                new CSVMapping<>(SummaryUpdate::setBidTime, COLON_TIME));
        CSV_MAPPINGS_OF_SUMMARY_UPDATE_FIELDS.put(CHANGE,
                new CSVMapping<>(SummaryUpdate::setChange, DOUBLE));
        CSV_MAPPINGS_OF_SUMMARY_UPDATE_FIELDS.put(CHANGE_FROM_OPEN,
                new CSVMapping<>(SummaryUpdate::setChangeFromOpen, DOUBLE));
        CSV_MAPPINGS_OF_SUMMARY_UPDATE_FIELDS.put(CLOSE,
                new CSVMapping<>(SummaryUpdate::setClose, DOUBLE));
        CSV_MAPPINGS_OF_SUMMARY_UPDATE_FIELDS.put(CLOSE_RANGE_1,
                new CSVMapping<>(SummaryUpdate::setCloseRange1, DOUBLE));
        CSV_MAPPINGS_OF_SUMMARY_UPDATE_FIELDS.put(CLOSE_RANGE_2,
                new CSVMapping<>(SummaryUpdate::setCloseRange2, DOUBLE));
        CSV_MAPPINGS_OF_SUMMARY_UPDATE_FIELDS.put(DAYS_TO_EXPIRATION,
                new CSVMapping<>(SummaryUpdate::setDaysToExpiration, STRING));
        CSV_MAPPINGS_OF_SUMMARY_UPDATE_FIELDS.put(DECIMAL_PRECISION,
                new CSVMapping<>(SummaryUpdate::setDecimalPrecision, STRING));
        CSV_MAPPINGS_OF_SUMMARY_UPDATE_FIELDS.put(DELAY,
                new CSVMapping<>(SummaryUpdate::setDelay, INTEGER));
        CSV_MAPPINGS_OF_SUMMARY_UPDATE_FIELDS.put(EXCHANGE_ID,
                new CSVMapping<>(SummaryUpdate::setExchangeID, (value) -> Integer.parseInt(value, 16)));
        CSV_MAPPINGS_OF_SUMMARY_UPDATE_FIELDS.put(EXTENDED_TRADE,
                new CSVMapping<>(SummaryUpdate::setExtendedTrade, DOUBLE));
        CSV_MAPPINGS_OF_SUMMARY_UPDATE_FIELDS.put(EXTENDED_TRADE_DATE,
                new CSVMapping<>(SummaryUpdate::setExtendedTradeDate, SLASHED_DATE));
        CSV_MAPPINGS_OF_SUMMARY_UPDATE_FIELDS.put(EXTENDED_TRADE_MARKET_CENTER,
                new CSVMapping<>(SummaryUpdate::setExtendedTradeMarketCenter, INTEGER));
        CSV_MAPPINGS_OF_SUMMARY_UPDATE_FIELDS.put(EXTENDED_TRADE_SIZE,
                new CSVMapping<>(SummaryUpdate::setExtendedTradeSize, INTEGER));
        CSV_MAPPINGS_OF_SUMMARY_UPDATE_FIELDS.put(EXTENDED_TRADE_TIME,
                new CSVMapping<>(SummaryUpdate::setExtendedTradeTime, COLON_TIME));
        CSV_MAPPINGS_OF_SUMMARY_UPDATE_FIELDS.put(EXTENDED_TRADING_CHANGE,
                new CSVMapping<>(SummaryUpdate::setExtendedTradingChange, DOUBLE));
        CSV_MAPPINGS_OF_SUMMARY_UPDATE_FIELDS.put(EXTENDED_TRADING_DIFFERENCE,
                new CSVMapping<>(SummaryUpdate::setExtendedTradingDifference, DOUBLE));
        CSV_MAPPINGS_OF_SUMMARY_UPDATE_FIELDS.put(FINANCIAL_STATUS_INDICATOR,
                new CSVMapping<>(SummaryUpdate::setFinancialStatusIndicator, STRING));
        CSV_MAPPINGS_OF_SUMMARY_UPDATE_FIELDS.put(FRACTION_DISPLAY_CODE,
                new CSVMapping<>(SummaryUpdate::setFractionDisplayCode, STRING));
        CSV_MAPPINGS_OF_SUMMARY_UPDATE_FIELDS.put(HIGH,
                new CSVMapping<>(SummaryUpdate::setHigh, DOUBLE));
        CSV_MAPPINGS_OF_SUMMARY_UPDATE_FIELDS.put(LAST,
                new CSVMapping<>(SummaryUpdate::setLast, DOUBLE));
        CSV_MAPPINGS_OF_SUMMARY_UPDATE_FIELDS.put(LAST_DATE,
                new CSVMapping<>(SummaryUpdate::setLastDate, SLASHED_DATE));
        CSV_MAPPINGS_OF_SUMMARY_UPDATE_FIELDS.put(LAST_MARKET_CENTER,
                new CSVMapping<>(SummaryUpdate::setLastMarketCenter, INTEGER));
        CSV_MAPPINGS_OF_SUMMARY_UPDATE_FIELDS.put(LAST_SIZE,
                new CSVMapping<>(SummaryUpdate::setLastSize, INTEGER));
        CSV_MAPPINGS_OF_SUMMARY_UPDATE_FIELDS.put(LAST_TIME,
                new CSVMapping<>(SummaryUpdate::setLastTime, COLON_TIME));
        CSV_MAPPINGS_OF_SUMMARY_UPDATE_FIELDS.put(LOW,
                new CSVMapping<>(SummaryUpdate::setLow, DOUBLE));
        CSV_MAPPINGS_OF_SUMMARY_UPDATE_FIELDS.put(MARKET_CAPITALIZATION,
                new CSVMapping<>(SummaryUpdate::setMarketCapitalization, DOUBLE));
        CSV_MAPPINGS_OF_SUMMARY_UPDATE_FIELDS.put(MARKET_OPEN,
                new CSVMapping<>(SummaryUpdate::setMarketOpen, MarketOpen::fromValue));
        CSV_MAPPINGS_OF_SUMMARY_UPDATE_FIELDS.put(MESSAGE_CONTENTS,
                new CSVMapping<>(SummaryUpdate::setMessageContents,
                        (value) -> IntStream.range(0, value.length())
                                .mapToObj(value::charAt)
                                .map(String::valueOf)
                                .map(SummaryUpdateContent::fromValue)
                                .collect(Collectors.toList())));
        CSV_MAPPINGS_OF_SUMMARY_UPDATE_FIELDS.put(MOST_RECENT_TRADE,
                new CSVMapping<>(SummaryUpdate::setMostRecentTrade, DOUBLE));
        CSV_MAPPINGS_OF_SUMMARY_UPDATE_FIELDS.put(MOST_RECENT_TRADE_AGGRESSOR,
                new CSVMapping<>(SummaryUpdate::setMostRecentTradeAggressor, MostRecentTradeAggressor::fromValue));
        CSV_MAPPINGS_OF_SUMMARY_UPDATE_FIELDS.put(MOST_RECENT_TRADE_CONDITIONS,
                new CSVMapping<>(SummaryUpdate::setMostRecentTradeConditions,
                        TradeConditionUtil::listFromTradeConditionString));
        CSV_MAPPINGS_OF_SUMMARY_UPDATE_FIELDS.put(MOST_RECENT_TRADE_DATE,
                new CSVMapping<>(SummaryUpdate::setMostRecentTradeDate, SLASHED_DATE));
        CSV_MAPPINGS_OF_SUMMARY_UPDATE_FIELDS.put(MOST_RECENT_TRADE_DAY_CODE,
                new CSVMapping<>(SummaryUpdate::setMostRecentTradeDayCode, INTEGER));
        CSV_MAPPINGS_OF_SUMMARY_UPDATE_FIELDS.put(MOST_RECENT_TRADE_MARKET_CENTER,
                new CSVMapping<>(SummaryUpdate::setMostRecentTradeMarketCenter, INTEGER));
        CSV_MAPPINGS_OF_SUMMARY_UPDATE_FIELDS.put(MOST_RECENT_TRADE_SIZE,
                new CSVMapping<>(SummaryUpdate::setMostRecentTradeSize, INTEGER));
        CSV_MAPPINGS_OF_SUMMARY_UPDATE_FIELDS.put(MOST_RECENT_TRADE_TIME,
                new CSVMapping<>(SummaryUpdate::setMostRecentTradeTime, COLON_TIME));
        CSV_MAPPINGS_OF_SUMMARY_UPDATE_FIELDS.put(NET_ASSET_VALUE,
                new CSVMapping<>(SummaryUpdate::setNetAssetValue, DOUBLE));
        CSV_MAPPINGS_OF_SUMMARY_UPDATE_FIELDS.put(NUMBER_OF_TRADES_TODAY,
                new CSVMapping<>(SummaryUpdate::setNumberOfTradesToday, INTEGER));
        CSV_MAPPINGS_OF_SUMMARY_UPDATE_FIELDS.put(OPEN,
                new CSVMapping<>(SummaryUpdate::setOpen, DOUBLE));
        CSV_MAPPINGS_OF_SUMMARY_UPDATE_FIELDS.put(OPEN_INTEREST,
                new CSVMapping<>(SummaryUpdate::setOpenInterest, INTEGER));
        CSV_MAPPINGS_OF_SUMMARY_UPDATE_FIELDS.put(OPEN_RANGE_1,
                new CSVMapping<>(SummaryUpdate::setOpenRange1, DOUBLE));
        CSV_MAPPINGS_OF_SUMMARY_UPDATE_FIELDS.put(OPEN_RANGE_2,
                new CSVMapping<>(SummaryUpdate::setOpenRange2, DOUBLE));
        CSV_MAPPINGS_OF_SUMMARY_UPDATE_FIELDS.put(PERCENT_CHANGE,
                new CSVMapping<>(SummaryUpdate::setPercentChange, DOUBLE));
        CSV_MAPPINGS_OF_SUMMARY_UPDATE_FIELDS.put(PERCENT_OFF_AVERAGE_VOLUME,
                new CSVMapping<>(SummaryUpdate::setPercentOffAverageVolume, DOUBLE));
        CSV_MAPPINGS_OF_SUMMARY_UPDATE_FIELDS.put(PREVIOUS_DAY_VOLUME,
                new CSVMapping<>(SummaryUpdate::setPreviousDayVolume, INTEGER));
        CSV_MAPPINGS_OF_SUMMARY_UPDATE_FIELDS.put(PRICE_EARNINGS_RATIO,
                new CSVMapping<>(SummaryUpdate::setPriceEarningsRatio, DOUBLE));
        CSV_MAPPINGS_OF_SUMMARY_UPDATE_FIELDS.put(RANGE,
                new CSVMapping<>(SummaryUpdate::setRange, DOUBLE));
        CSV_MAPPINGS_OF_SUMMARY_UPDATE_FIELDS.put(RESTRICTED_CODE,
                new CSVMapping<>(SummaryUpdate::setRestrictedCode, RestrictedCode::fromValue));
        CSV_MAPPINGS_OF_SUMMARY_UPDATE_FIELDS.put(SETTLE,
                new CSVMapping<>(SummaryUpdate::setSettle, DOUBLE));
        CSV_MAPPINGS_OF_SUMMARY_UPDATE_FIELDS.put(SETTLEMENT_DATE,
                new CSVMapping<>(SummaryUpdate::setSettlementDate, SLASHED_DATE));
        CSV_MAPPINGS_OF_SUMMARY_UPDATE_FIELDS.put(SPREAD,
                new CSVMapping<>(SummaryUpdate::setSpread, DOUBLE));
        CSV_MAPPINGS_OF_SUMMARY_UPDATE_FIELDS.put(SYMBOL,
                new CSVMapping<>(SummaryUpdate::setSymbol, STRING));
        CSV_MAPPINGS_OF_SUMMARY_UPDATE_FIELDS.put(TICK,
                new CSVMapping<>(SummaryUpdate::setTick, INTEGER));
        CSV_MAPPINGS_OF_SUMMARY_UPDATE_FIELDS.put(TICK_ID,
                new CSVMapping<>(SummaryUpdate::setTickID, INTEGER));
        CSV_MAPPINGS_OF_SUMMARY_UPDATE_FIELDS.put(TOTAL_VOLUME,
                new CSVMapping<>(SummaryUpdate::setTotalVolume, INTEGER));
        CSV_MAPPINGS_OF_SUMMARY_UPDATE_FIELDS.put(VOLATILITY,
                new CSVMapping<>(SummaryUpdate::setVolatility, DOUBLE));
        CSV_MAPPINGS_OF_SUMMARY_UPDATE_FIELDS.put(VWAP,
                new CSVMapping<>(SummaryUpdate::setVwap, DOUBLE));

        // Add mappings with CSV indices analogous to line of execution

        FUNDAMENTAL_DATA_CSV_MAPPER = new IndexCSVMapper<>(FundamentalData::new);
        FUNDAMENTAL_DATA_CSV_MAPPER.addMapping(FundamentalData::setSymbol, STRING);
        FUNDAMENTAL_DATA_CSV_MAPPER.addMapping(FundamentalData::setExchangeID, (value) -> Integer.parseInt(value, 16));
        FUNDAMENTAL_DATA_CSV_MAPPER.addMapping(FundamentalData::setPERatio, DOUBLE);
        FUNDAMENTAL_DATA_CSV_MAPPER.addMapping(FundamentalData::setAverageVolume, DOUBLE);
        FUNDAMENTAL_DATA_CSV_MAPPER.addMapping(FundamentalData::set52WeekHigh, DOUBLE);
        FUNDAMENTAL_DATA_CSV_MAPPER.addMapping(FundamentalData::set52WeekLow, DOUBLE);
        FUNDAMENTAL_DATA_CSV_MAPPER.addMapping(FundamentalData::setCalendarYearHigh, DOUBLE);
        FUNDAMENTAL_DATA_CSV_MAPPER.addMapping(FundamentalData::setCalendarYearLow, DOUBLE);
        FUNDAMENTAL_DATA_CSV_MAPPER.addMapping(FundamentalData::setDividendYield, DOUBLE);
        FUNDAMENTAL_DATA_CSV_MAPPER.addMapping(FundamentalData::setDividendAmount, DOUBLE);
        FUNDAMENTAL_DATA_CSV_MAPPER.addMapping(FundamentalData::setDividendRate, DOUBLE);
        FUNDAMENTAL_DATA_CSV_MAPPER.addMapping(FundamentalData::setPayDate, SLASHED_DATE);
        FUNDAMENTAL_DATA_CSV_MAPPER.addMapping(FundamentalData::setExDividendDate, SLASHED_DATE);
        FUNDAMENTAL_DATA_CSV_MAPPER.addMapping(FundamentalData::setCurrentYearEarningsPerShare, DOUBLE);
        FUNDAMENTAL_DATA_CSV_MAPPER.addMapping(FundamentalData::setNextYearEarningsPerShare, DOUBLE);
        FUNDAMENTAL_DATA_CSV_MAPPER.addMapping(FundamentalData::setFiveYearGrowthPercentage, DOUBLE);
        FUNDAMENTAL_DATA_CSV_MAPPER.addMapping(FundamentalData::setFiscalYearEnd, INTEGER);
        FUNDAMENTAL_DATA_CSV_MAPPER.addMapping(FundamentalData::setCompanyName, STRING);
        FUNDAMENTAL_DATA_CSV_MAPPER.addMapping(FundamentalData::setRootOptionSymbol,
                (value) -> Arrays.asList(value.split(" ")));
        FUNDAMENTAL_DATA_CSV_MAPPER.addMapping(FundamentalData::setPercentHeldByInstitutions, DOUBLE);
        FUNDAMENTAL_DATA_CSV_MAPPER.addMapping(FundamentalData::setBeta, DOUBLE);
        FUNDAMENTAL_DATA_CSV_MAPPER.addMapping(FundamentalData::setLeaps, STRING);
        FUNDAMENTAL_DATA_CSV_MAPPER.addMapping(FundamentalData::setCurrentAssets, DOUBLE);
        FUNDAMENTAL_DATA_CSV_MAPPER.addMapping(FundamentalData::setCurrentLiabilities, DOUBLE);
        FUNDAMENTAL_DATA_CSV_MAPPER.addMapping(FundamentalData::setBalanceSheetDate, SLASHED_DATE);
        FUNDAMENTAL_DATA_CSV_MAPPER.addMapping(FundamentalData::setLongTermDebt, DOUBLE);
        FUNDAMENTAL_DATA_CSV_MAPPER.addMapping(FundamentalData::setCommonSharesOutstanding, DOUBLE);
        // For "Split factor 1"
        FUNDAMENTAL_DATA_CSV_MAPPER.addMapping((fundamentalData, csvValue) ->
                splitFactorAndDate(fundamentalData, csvValue,
                        FundamentalData::setSplitFactor1, FundamentalData::setSplitFactor1Date));
        // For "Split factor 2"
        FUNDAMENTAL_DATA_CSV_MAPPER.addMapping((fundamentalData, csvValue) ->
                splitFactorAndDate(fundamentalData, csvValue,
                        FundamentalData::setSplitFactor2, FundamentalData::setSplitFactor2Date));
        FUNDAMENTAL_DATA_CSV_MAPPER.addMapping(FundamentalData::setFormatCode, STRING);
        FUNDAMENTAL_DATA_CSV_MAPPER.addMapping(FundamentalData::setPrecision, INTEGER);
        FUNDAMENTAL_DATA_CSV_MAPPER.addMapping(FundamentalData::setSic, INTEGER);
        FUNDAMENTAL_DATA_CSV_MAPPER.addMapping(FundamentalData::setHistoricalVolatility, DOUBLE);
        FUNDAMENTAL_DATA_CSV_MAPPER.addMapping(FundamentalData::setSecurityType, STRING);
        FUNDAMENTAL_DATA_CSV_MAPPER.addMapping(FundamentalData::setListedMarket, STRING);
        FUNDAMENTAL_DATA_CSV_MAPPER.addMapping(FundamentalData::set52WeekHighDate, SLASHED_DATE);
        FUNDAMENTAL_DATA_CSV_MAPPER.addMapping(FundamentalData::set52WeekLowDate, SLASHED_DATE);
        FUNDAMENTAL_DATA_CSV_MAPPER.addMapping(FundamentalData::setCalendarYearHighDate, SLASHED_DATE);
        FUNDAMENTAL_DATA_CSV_MAPPER.addMapping(FundamentalData::setCalendarYearLowDate, SLASHED_DATE);
        FUNDAMENTAL_DATA_CSV_MAPPER.addMapping(FundamentalData::setYearEndClose, DOUBLE);
        FUNDAMENTAL_DATA_CSV_MAPPER.addMapping(FundamentalData::setMaturityDate, SLASHED_DATE);
        FUNDAMENTAL_DATA_CSV_MAPPER.addMapping(FundamentalData::setCouponRate, DOUBLE);
        FUNDAMENTAL_DATA_CSV_MAPPER.addMapping(FundamentalData::setExpirationDate, SLASHED_DATE);
        FUNDAMENTAL_DATA_CSV_MAPPER.addMapping(FundamentalData::setStrikePrice, DOUBLE);
        FUNDAMENTAL_DATA_CSV_MAPPER.addMapping(FundamentalData::setNaics, INTEGER);
        FUNDAMENTAL_DATA_CSV_MAPPER.addMapping(FundamentalData::setExchangeRoot, STRING);
        FUNDAMENTAL_DATA_CSV_MAPPER.addMapping(FundamentalData::setOptionsPremiumMultiplier, DOUBLE);
        FUNDAMENTAL_DATA_CSV_MAPPER.addMapping(FundamentalData::setOptionsMultipleDeliverables,
                OptionsMultipleDeliverables::fromValue);
        FUNDAMENTAL_DATA_CSV_MAPPER.addMapping(FundamentalData::setSessionOpenTime, COLON_TIME);
        FUNDAMENTAL_DATA_CSV_MAPPER.addMapping(FundamentalData::setSessionCloseTime, COLON_TIME);
        FUNDAMENTAL_DATA_CSV_MAPPER.addMapping(FundamentalData::setBaseCurrency, STRING);
        FUNDAMENTAL_DATA_CSV_MAPPER.addMapping(FundamentalData::setContractSize, STRING);
        FUNDAMENTAL_DATA_CSV_MAPPER.addMapping(FundamentalData::setContractMonths, STRING);
        FUNDAMENTAL_DATA_CSV_MAPPER.addMapping(FundamentalData::setMinimumTickSize, DOUBLE);
        FUNDAMENTAL_DATA_CSV_MAPPER.addMapping(FundamentalData::setFirstDeliveryDate, SLASHED_DATE);
        FUNDAMENTAL_DATA_CSV_MAPPER.addMapping(FundamentalData::setFigi, STRING);
        FUNDAMENTAL_DATA_CSV_MAPPER.addMapping(FundamentalData::setSecuritySubType, INTEGER);

        REGIONAL_QUOTE_CSV_MAPPER = new IndexCSVMapper<>(RegionalQuote::new);
        REGIONAL_QUOTE_CSV_MAPPER.addMapping(RegionalQuote::setSymbol, STRING);
        REGIONAL_QUOTE_CSV_MAPPER.addMapping(RegionalQuote::setExchange, STRING);
        REGIONAL_QUOTE_CSV_MAPPER.addMapping(RegionalQuote::setRegionalBid, DOUBLE);
        REGIONAL_QUOTE_CSV_MAPPER.addMapping(RegionalQuote::setRegionalBidSize, INTEGER);
        REGIONAL_QUOTE_CSV_MAPPER.addMapping(RegionalQuote::setRegionalBidTime, COLON_TIME);
        REGIONAL_QUOTE_CSV_MAPPER.addMapping(RegionalQuote::setRegionalAsk, DOUBLE);
        REGIONAL_QUOTE_CSV_MAPPER.addMapping(RegionalQuote::setRegionalAskSize, INTEGER);
        REGIONAL_QUOTE_CSV_MAPPER.addMapping(RegionalQuote::setRegionalAskTime, COLON_TIME);
        REGIONAL_QUOTE_CSV_MAPPER.addMapping(RegionalQuote::setFractionDisplayCode, INTEGER);
        REGIONAL_QUOTE_CSV_MAPPER.addMapping(RegionalQuote::setDecimalPrecision, INTEGER);
        REGIONAL_QUOTE_CSV_MAPPER.addMapping(RegionalQuote::setMarketCenter, INTEGER);

        NEWS_HEADLINE_CSV_MAPPER = new IndexCSVMapper<>(NewsHeadline::new);
        NEWS_HEADLINE_CSV_MAPPER.addMapping(NewsHeadline::setDistributorCode, STRING);
        NEWS_HEADLINE_CSV_MAPPER.addMapping(NewsHeadline::setStoryID, INTEGER);
        NEWS_HEADLINE_CSV_MAPPER.addMapping(NewsHeadline::setSymbolList, (value) -> Arrays.asList(value.split(":")));
        NEWS_HEADLINE_CSV_MAPPER.addMapping(NewsHeadline::setTimestamp, DATE_SPACE_TIME);
        NEWS_HEADLINE_CSV_MAPPER.addMapping(NewsHeadline::setHeadline, STRING);

        CUSTOMER_INFORMATION_CSV_MAPPER = new IndexCSVMapper<>(CustomerInformation::new);
        CUSTOMER_INFORMATION_CSV_MAPPER.addMapping(CustomerInformation::setServiceType, ServiceType::fromValue);
        CUSTOMER_INFORMATION_CSV_MAPPER.addMapping(CustomerInformation::setIp, STRING);
        CUSTOMER_INFORMATION_CSV_MAPPER.addMapping(CustomerInformation::setPort, INTEGER);
        CUSTOMER_INFORMATION_CSV_MAPPER.setMapping(4, CustomerInformation::setVersion, STRING);
        CUSTOMER_INFORMATION_CSV_MAPPER.setMapping(6, CustomerInformation::setVerboseExchanges,
                (value) -> Arrays.asList(value.trim().split(" ")));
        CUSTOMER_INFORMATION_CSV_MAPPER.setMapping(8, CustomerInformation::setMaxSymbols, INTEGER);
        CUSTOMER_INFORMATION_CSV_MAPPER.addMapping(CustomerInformation::setFlags, STRING);
        CUSTOMER_INFORMATION_CSV_MAPPER.addMapping(CustomerInformation::setAccountExpirationDate, DATE);

        SUMMARY_UPDATE_FIELDS_CSV_MAPPER = new DirectListCSVMapper<>(ArrayList::new, SummaryUpdateField::fromValue);

        LOG_LEVELS_CSV_MAPPER = new DirectListCSVMapper<>(ArrayList::new, LogLevel::fromValue);

        TIMESTAMP_CSV_MAPPER = new DirectIndexCSVMapper<>(0, DATE_SPACE_COLON_TIME);
    }

    protected final Object messageReceivedLock;
    protected final HashMap<String, FeedMessageListener<FundamentalData>> fundamentalDataListenersOfSymbols;
    protected final HashMap<String, FeedMessageListener<SummaryUpdate>> summaryUpdateListenersOfSymbols;
    protected final HashMap<String, FeedMessageListener<RegionalQuote>> regionalQuoteListenersOfSymbols;

    protected Level1FeedEventListener level1FeedEventListener;
    protected IndexCSVMapper<SummaryUpdate> summaryUpdateCSVMapper;

    protected FeedMessageListener<NewsHeadline> newsHeadlineListener;
    protected SingleMessageFuture<LocalDateTime> timestampFuture;
    protected LocalDateTime latestTimestamp;
    protected CustomerInformation customerInformation;
    protected SingleMessageFuture<FeedStatistics> feedStatisticsFuture;
    protected FeedStatistics latestFeedStatistics;
    protected SingleMessageFuture<List<String>> fundamentalFieldNamesFuture;
    protected SingleMessageFuture<List<SummaryUpdateField>> allUpdateFieldNamesFuture;
    protected SingleMessageFuture<List<SummaryUpdateField>> currentUpdateFieldNamesFuture;
    protected SingleMessageFuture<List<LogLevel>> logLevelsFuture;
    protected SingleMessageFuture<List<String>> watchedSymbolsFuture;

    /**
     * Instantiates a new {@link Level1Feed}.
     *
     * @param level1FeedName the {@link Level1Feed} feed name
     * @param hostname       the hostname
     * @param port           the port
     */
    public Level1Feed(String level1FeedName, String hostname, int port) {
        super(level1FeedName + FEED_NAME_SUFFIX, hostname, port, COMMA_DELIMITED_SPLITTER, true, true);

        messageReceivedLock = new Object();
        fundamentalDataListenersOfSymbols = new HashMap<>();
        summaryUpdateListenersOfSymbols = new HashMap<>();
        regionalQuoteListenersOfSymbols = new HashMap<>();

        level1FeedEventListener = new Level1FeedEventListener() {
            @Override
            public void onServerReconnectFailed() {
                LOGGER.warn("Server reconnection has failed!");
            }

            @Override
            public void onSymbolLimitReached(String symbol) {
                LOGGER.warn("Symbol limit reached with symbol: {}!", symbol);
            }

            @Override
            public void onSymbolNotWatched(String symbol) {
                LOGGER.warn("{} symbol not watched!", symbol);
            }
        };
        currentUpdateFieldNamesFuture = new SingleMessageFuture<>(); // For initial 'CURRENT_UPDATE_FIELDNAMES' message
    }

    @Override
    protected void onMessageReceived(String[] csv) {
        // Confirm message format
        if (!valuePresent(csv, 0)) {
            LOGGER.error("Received unknown message format: {}", (Object) csv);
            return;
        }

        if (valueEquals(csv, 0, FeedMessageType.ERROR.value())) {
            LOGGER.error("Received error message! {}", (Object) csv);
            return;
        }

        synchronized (messageReceivedLock) {
            if (valueEquals(csv, 0, FeedMessageType.SYSTEM.value())) {
                if (!valuePresent(csv, 1)) {
                    LOGGER.error("Received unknown System message: {}", (Object) csv);
                    return;
                }

                String systemMessageTypeString = csv[1];

                if (checkServerConnectionStatusMessage(systemMessageTypeString)) {
                    return;
                }

                try {
                    Level1SystemMessageType systemMessageType = Level1SystemMessageType.fromValue(
                            systemMessageTypeString);

                    switch (systemMessageType) {
                        case KEY:
                        case KEYOK:
                        case IP:
                            LOGGER.debug("Received unused {} message: {}", systemMessageType, csv);
                            break;
                        case SERVER_RECONNECT_FAILED:
                            handleServerReconnectFailed();
                            break;
                        case SYMBOL_LIMIT_REACHED:
                            handleSymbolLimitReachedMessage(csv);
                            break;
                        case CUST:
                            handleCustomerInformationMessage(csv);
                            break;
                        case STATS:
                            handleFeedStatisticsMessage(csv);
                            break;
                        case FUNDAMENTAL_FIELDNAMES:
                            handleListCSVSystemMessageFuture(systemMessageType, fundamentalFieldNamesFuture,
                                    STRING_LIST_CSV_MAPPER, csv);
                            fundamentalFieldNamesFuture = null;
                            break;
                        case UPDATE_FIELDNAMES:
                            handleListCSVSystemMessageFuture(systemMessageType, allUpdateFieldNamesFuture,
                                    SUMMARY_UPDATE_FIELDS_CSV_MAPPER, csv);
                            allUpdateFieldNamesFuture = null;
                            break;
                        case CURRENT_UPDATE_FIELDNAMES:
                            handleListCSVSystemMessageFuture(systemMessageType, currentUpdateFieldNamesFuture,
                                    SUMMARY_UPDATE_FIELDS_CSV_MAPPER, csv);
                            if (currentUpdateFieldNamesFuture != null &&
                                    !currentUpdateFieldNamesFuture.isCompletedExceptionally()) {
                                setupSummaryUpdateCSVMapper();
                            } else {
                                LOGGER.error("Cannot setup mappings for summary/update messages!");
                            }
                            currentUpdateFieldNamesFuture = null;
                            break;
                        case CURRENT_LOG_LEVELS:
                            handleListCSVSystemMessageFuture(systemMessageType, logLevelsFuture,
                                    LOG_LEVELS_CSV_MAPPER, csv);
                            logLevelsFuture = null;
                            break;
                        case WATCHES:
                            handleListCSVSystemMessageFuture(systemMessageType, watchedSymbolsFuture,
                                    STRING_LIST_CSV_MAPPER, csv);
                            watchedSymbolsFuture = null;
                            break;
                        default:
                            LOGGER.error("Unhandled message type: {}", systemMessageType);
                    }
                } catch (IllegalArgumentException illegalArgumentException) {
                    LOGGER.error("Received unknown message type: {}", csv[1], illegalArgumentException);
                }
            } else {
                try {
                    Level1MessageType messageType = Level1MessageType.fromValue(csv[0]);

                    switch (messageType) {
                        case FUNDAMENTAL_MESSAGE:
                            handleFundamentalMessage(csv);
                            break;
                        case SUMMARY_MESSAGE:
                        case UPDATE_MESSAGE:
                            handleSummaryUpdateMessage(csv, messageType);
                            break;
                        case REGIONAL_UPDATE:
                            handleRegionalUpdateMessage(csv);
                            break;
                        case NEWS_HEADLINE_MESSAGE:
                            handleNewsHeadlineMessage(csv);
                            break;
                        case TIMESTAMP_MESSAGE:
                            handleTimestampMessage(csv);
                            break;
                        case SYMBOL_NOT_WATCHED:
                            handleSymbolNotWatched(csv);
                            break;
                        default:
                            LOGGER.error("Unhandled message type: {}", messageType);
                    }
                } catch (IllegalArgumentException illegalArgumentException) {
                    LOGGER.error("Received unknown message type: {}", csv[1], illegalArgumentException);
                }
            }
        }
    }

    private void handleServerReconnectFailed() {
        if (level1FeedEventListener != null) {
            level1FeedEventListener.onServerReconnectFailed();
        }
    }

    private void handleSymbolLimitReachedMessage(String[] csv) {
        if (level1FeedEventListener != null) {
            if (!valueExists(csv, 2)) {
                LOGGER.error("System message needs more arguments!");
            } else {
                String symbol = csv[2];
                level1FeedEventListener.onSymbolLimitReached(symbol);
            }
        }
    }

    private void handleCustomerInformationMessage(String[] csv) {
        try {
            customerInformation = CUSTOMER_INFORMATION_CSV_MAPPER.map(csv, 2);
        } catch (Exception exception) {
            LOGGER.error("Could not map CustomerInformation!", exception);
        }
    }

    private void handleFeedStatisticsMessage(String[] csv) {
        try {
            FeedStatistics feedStatistics = StreamingCSVMappers.FEED_STATISTICS_CSV_MAPPER.map(csv, 2);
            latestFeedStatistics = feedStatistics;

            if (feedStatisticsFuture != null) {
                feedStatisticsFuture.complete(feedStatistics);
                feedStatisticsFuture = null;
            } else {
                LOGGER.error("Could not complete {} future!", Level1SystemMessageType.STATS);
            }
        } catch (Exception exception) {
            if (feedStatisticsFuture != null) {
                feedStatisticsFuture.completeExceptionally(exception);
                feedStatisticsFuture = null;
            } else {
                LOGGER.error("Could not complete {} future!", Level1SystemMessageType.STATS);
            }
        }
    }

    private void setupSummaryUpdateCSVMapper() {
        try {
            List<SummaryUpdateField> currentSummaryUpdateFields = currentUpdateFieldNamesFuture.get();
            summaryUpdateCSVMapper = new IndexCSVMapper<>(SummaryUpdate::new);

            for (int index = 0; index < currentSummaryUpdateFields.size(); index++) {
                SummaryUpdateField summaryUpdateField = currentSummaryUpdateFields.get(index);
                if (summaryUpdateField == null) {
                    continue;
                }

                CSVMapping<SummaryUpdate, ?> summaryUpdateFieldMapping =
                        CSV_MAPPINGS_OF_SUMMARY_UPDATE_FIELDS.get(summaryUpdateField);
                if (summaryUpdateFieldMapping == null) {
                    LOGGER.error("No mapping found for {} SummaryUpdateField!", summaryUpdateField);
                    continue;
                }

                summaryUpdateCSVMapper.setMapping(index, summaryUpdateFieldMapping);
            }

            LOGGER.debug("Successfully mapped summary/update CSV messages to: {}", currentSummaryUpdateFields);
        } catch (Exception exception) {
            LOGGER.error("Cannot setup mappings for summary/update messages!", exception);
        }
    }

    /**
     * Handles a {@link SingleMessageFuture} for a CSV with a {@link ListCSVMapper}.
     *
     * @param <T>               the type of {@link SingleMessageFuture}
     * @param systemMessageType the {@link Level1SystemMessageType}
     * @param future            the {@link SingleMessageFuture}
     * @param listCSVMapper     the {@link ListCSVMapper}
     * @param csv               the CSV
     */
    private <T> void handleListCSVSystemMessageFuture(Level1SystemMessageType systemMessageType,
            SingleMessageFuture<List<T>> future, AbstractListCSVMapper<T> listCSVMapper, String[] csv) {
        if (future != null) {
            try {
                future.complete(listCSVMapper.mapToList(csv, 2));
            } catch (Exception exception) {
                future.completeExceptionally(exception);
            }
        } else {
            LOGGER.error("Received {} System message, but with no Future to handle it!", systemMessageType);
        }
    }

    private void handleFundamentalMessage(String[] csv) {
        try {
            FundamentalData fundamentalData = FUNDAMENTAL_DATA_CSV_MAPPER.map(csv, 1);
            FeedMessageListener<FundamentalData> listener =
                    fundamentalDataListenersOfSymbols.get(fundamentalData.getSymbol());
            if (listener == null) {
                LOGGER.error("Received FundamentalData, but no listener for symbol {} exists!",
                        fundamentalData.getSymbol());
            } else {
                listener.onMessageReceived(fundamentalData);
            }
        } catch (Exception exception) {
            LOGGER.error("Could not handle FundamentalData message!", exception);
        }
    }

    private void handleSummaryUpdateMessage(String[] csv, Level1MessageType messageType) {
        try {
            SummaryUpdate summaryUpdate = summaryUpdateCSVMapper.map(csv, 1);
            FeedMessageListener<SummaryUpdate> listener =
                    summaryUpdateListenersOfSymbols.get(summaryUpdate.getSymbol());
            if (listener == null) {
                LOGGER.error("Received SummaryUpdate, but no listener for symbol {} exists!",
                        summaryUpdate.getSymbol());
            } else {
                switch (messageType) {
                    case SUMMARY_MESSAGE:
                        summaryUpdate.setType(SummaryUpdate.Type.SUMMARY);
                        break;
                    case UPDATE_MESSAGE:
                        summaryUpdate.setType(SummaryUpdate.Type.UPDATE);
                        break;
                }

                listener.onMessageReceived(summaryUpdate);
            }
        } catch (Exception exception) {
            LOGGER.error("Could not handle SummaryUpdate message!", exception);
        }
    }

    private void handleRegionalUpdateMessage(String[] csv) {
        try {
            RegionalQuote regionalQuote = REGIONAL_QUOTE_CSV_MAPPER.map(csv, 1);
            FeedMessageListener<RegionalQuote> listener =
                    regionalQuoteListenersOfSymbols.get(regionalQuote.getSymbol());
            if (listener == null) {
                LOGGER.error("Received RegionalQuote, but no listener for symbol {} exists!",
                        regionalQuote.getSymbol());
            } else {
                listener.onMessageReceived(regionalQuote);
            }
        } catch (Exception exception) {
            LOGGER.error("Could not handle RegionalQuote message!", exception);
        }
    }

    private void handleNewsHeadlineMessage(String[] csv) {
        if (newsHeadlineListener == null) {
            LOGGER.error("Received NewsHeadline, but no listener exists!");
        } else {
            try {
                NewsHeadline newsHeadline = NEWS_HEADLINE_CSV_MAPPER.map(csv, 1);
                newsHeadlineListener.onMessageReceived(newsHeadline);
            } catch (Exception exception) {
                newsHeadlineListener.onMessageException(exception);
            }
        }
    }

    private void handleTimestampMessage(String[] csv) {
        try {
            LocalDateTime timestamp = TIMESTAMP_CSV_MAPPER.map(csv, 1);
            latestTimestamp = timestamp;

            if (timestampFuture != null) {
                timestampFuture.complete(timestamp);
                timestampFuture = null;
            }
        } catch (Exception exception) {
            if (timestampFuture != null) {
                timestampFuture.completeExceptionally(exception);
                timestampFuture = null;
            } else {
                LOGGER.error("Could not handle Timestamp message!", exception);
            }
        }
    }

    private void handleSymbolNotWatched(String[] csv) {
        if (level1FeedEventListener != null) {
            if (!valueExists(csv, 2)) {
                LOGGER.error("System message needs more arguments!");
            } else {
                String symbol = csv[2];
                level1FeedEventListener.onSymbolNotWatched(symbol);
            }
        }
    }

    //
    // START Feed commands
    //

    /**
     * Begins watching a symbol for Level 1 updates. This sends a {@link Level1Command#WATCH} request.
     *
     * @param symbol                  the symbol that you wish to receive updates on
     * @param fundamentalDataListener the {@link FeedMessageListener} of {@link FundamentalData} Note if a {@link
     *                                FeedMessageListener} already exists for the given 'symbol', then it is overwritten
     *                                with this one.
     * @param summaryUpdateListener   the {@link FeedMessageListener} of {@link SummaryUpdate}s. Note if a {@link
     *                                FeedMessageListener} already exists for the given 'symbol', then it is overwritten
     *                                with this one.
     *
     * @throws IOException thrown for {@link IOException}s
     */
    public void requestWatch(String symbol, FeedMessageListener<FundamentalData> fundamentalDataListener,
            FeedMessageListener<SummaryUpdate> summaryUpdateListener) throws IOException {
        checkNotNull(symbol);
        checkArgument(fundamentalDataListener != null || summaryUpdateListener != null,
                "There must be at least one FundamentalData listener or SummaryUpdate listener!");

        StringBuilder requestBuilder = new StringBuilder();
        requestBuilder.append(Level1Command.WATCH.value());
        requestBuilder.append(symbol);
        requestBuilder.append(LineEnding.CR_LF.getASCIIString());

        synchronized (messageReceivedLock) {
            if (fundamentalDataListener != null) {
                fundamentalDataListenersOfSymbols.put(symbol, fundamentalDataListener);
            }
            if (summaryUpdateListener != null) {
                summaryUpdateListenersOfSymbols.put(symbol, summaryUpdateListener);
            }
        }

        // If symbol is already being watched, nothing happens
        sendAndLogMessage(requestBuilder.toString());
    }

    /**
     * Begins a trades only watch on a symbol for Level 1 updates. This sends a {@link Level1Command#WATCH_TRADES}
     * request.
     *
     * @param symbol                  the symbol that you wish to receive updates on
     * @param fundamentalDataListener the {@link FeedMessageListener} of {@link FundamentalData} Note if a {@link
     *                                FeedMessageListener} already exists for the given 'symbol', then it is overwritten
     *                                with this one.
     * @param summaryUpdateListener   the {@link FeedMessageListener} of {@link SummaryUpdate}s. Note if a {@link
     *                                FeedMessageListener} already exists for the given 'symbol', then it is overwritten
     *                                with this one.
     *
     * @throws IOException thrown for {@link IOException}s
     */
    public void requestWatchTrades(String symbol, FeedMessageListener<FundamentalData> fundamentalDataListener,
            FeedMessageListener<SummaryUpdate> summaryUpdateListener) throws IOException {
        checkNotNull(symbol);
        checkArgument(fundamentalDataListener != null || summaryUpdateListener != null,
                "There must be at least one FundamentalData listener or SummaryUpdate listener!");

        StringBuilder requestBuilder = new StringBuilder();
        requestBuilder.append(Level1Command.WATCH_TRADES.value());
        requestBuilder.append(symbol);
        requestBuilder.append(LineEnding.CR_LF.getASCIIString());

        synchronized (messageReceivedLock) {
            if (fundamentalDataListener != null) {
                fundamentalDataListenersOfSymbols.put(symbol, fundamentalDataListener);
            }
            if (summaryUpdateListener != null) {
                summaryUpdateListenersOfSymbols.put(symbol, summaryUpdateListener);
            }
        }

        // If symbol is already being watched, nothing happens
        sendAndLogMessage(requestBuilder.toString());
    }

    /**
     * Terminates Level 1 updates for the symbol specified (including regionals). This sends a {@link
     * Level1Command#UNWATCH} request.
     *
     * @param symbol the symbol that you wish to terminate updates on
     *
     * @throws IOException thrown for {@link IOException}s
     */
    public void requestUnwatch(String symbol) throws IOException {
        checkNotNull(symbol);

        StringBuilder requestBuilder = new StringBuilder();
        requestBuilder.append(Level1Command.UNWATCH.value());
        requestBuilder.append(symbol);
        requestBuilder.append(LineEnding.CR_LF.getASCIIString());

        synchronized (messageReceivedLock) {
            fundamentalDataListenersOfSymbols.remove(symbol);
            summaryUpdateListenersOfSymbols.remove(symbol);
            regionalQuoteListenersOfSymbols.remove(symbol);
        }

        sendAndLogMessage(requestBuilder.toString());
    }

    /**
     * Forces a refresh for {@link FundamentalData} and a summary {@link SummaryUpdate} message from the server for the
     * symbol specified. This sends a {@link Level1Command#FORCE_WATCH_REFRESH} request.
     * <br>
     * NOTE: This can not be used as a method to get a snapshot of data from the feed. You must already be watching the
     * symbol or the server ignores this request.
     *
     * @param symbol the symbol that you wish to receive the update on
     *
     * @throws IOException thrown for {@link IOException}s
     */
    public void requestForceRefresh(String symbol) throws IOException {
        checkNotNull(symbol);

        StringBuilder requestBuilder = new StringBuilder();
        requestBuilder.append(Level1Command.FORCE_WATCH_REFRESH.value());
        requestBuilder.append(symbol);
        requestBuilder.append(LineEnding.CR_LF.getASCIIString());

        sendAndLogMessage(requestBuilder.toString());
    }

    /**
     * Requests a {@link LocalDateTime} timestamp to be sent. This sends a {@link Level1Command#TIMESTAMP} request.
     *
     * @return the {@link SingleMessageFuture} of the {@link LocalDateTime} timestamp
     *
     * @throws IOException thrown for {@link IOException}s
     */
    public SingleMessageFuture<LocalDateTime> requestTimestamp() throws IOException {
        synchronized (messageReceivedLock) {
            if (timestampFuture != null) {
                return timestampFuture;
            }
        }

        StringBuilder requestBuilder = new StringBuilder();
        requestBuilder.append(Level1Command.TIMESTAMP.value());
        requestBuilder.append(LineEnding.CR_LF.getASCIIString());

        synchronized (messageReceivedLock) {
            timestampFuture = new SingleMessageFuture<>();
        }

        sendAndLogMessage(requestBuilder.toString());

        return timestampFuture;
    }

    /**
     * Sends a {@link FeedCommand#SYSTEM} {@link Level1SystemCommand}.
     *
     * @param level1SystemCommand the {@link Level1SystemCommand}
     * @param arguments           the arguments. Null for no arguments.
     *
     * @throws IOException thrown for {@link IOException}s
     */
    private void sendLevel1SystemCommand(Level1SystemCommand level1SystemCommand, String... arguments)
            throws IOException {
        StringJoiner commandJoiner = new StringJoiner(",", "", LineEnding.CR_LF.getASCIIString());
        commandJoiner.add(FeedCommand.SYSTEM.value());
        commandJoiner.add(level1SystemCommand.value());
        if (arguments != null && arguments.length != 0) {
            for (String argument : arguments) {
                commandJoiner.add(argument);
            }
        }

        sendAndLogMessage(commandJoiner.toString());
    }

    /**
     * Enables or disables the once-per-second {@link LocalDateTime} timestamp. This sends a {@link
     * Level1SystemCommand#TIMESTAMPSON} or {@link Level1SystemCommand#TIMESTAMPSOFF} request.
     *
     * @param toggle true to enable, false to disable
     *
     * @throws IOException thrown for {@link IOException}s
     */
    public void enableTimestamps(boolean toggle) throws IOException {
        sendLevel1SystemCommand(toggle ? Level1SystemCommand.TIMESTAMPSON : Level1SystemCommand.TIMESTAMPSOFF);
    }

    /**
     * Begins watching a symbol for Level 1 {@link RegionalQuote} updates. This sends a {@link
     * Level1SystemCommand#REGON} request.
     *
     * @param symbol                the symbol that you wish to receive updates on
     * @param regionalQuoteListener the {@link FeedMessageListener} of {@link RegionalQuote}s.Note if a {@link
     *                              FeedMessageListener} already exists for the given 'symbol', then it is overwritten
     *                              with this one.
     *
     * @throws IOException thrown for {@link IOException}s
     */
    public void requestRegionalQuoteWatch(String symbol, FeedMessageListener<RegionalQuote> regionalQuoteListener)
            throws IOException {
        checkNotNull(symbol);
        checkNotNull(regionalQuoteListener);

        synchronized (messageReceivedLock) {
            regionalQuoteListenersOfSymbols.put(symbol, regionalQuoteListener);
        }

        sendLevel1SystemCommand(Level1SystemCommand.REGON, symbol);
    }

    /**
     * Stops watching a symbol for Level 1 {@link RegionalQuote} updates. This sends a {@link
     * Level1SystemCommand#REGOFF} request.
     *
     * @param symbol the symbol that you wish to stop receiving updates on
     *
     * @throws IOException thrown for {@link IOException}s
     */
    public void requestRegionalQuoteUnwatch(String symbol) throws IOException {
        checkNotNull(symbol);

        synchronized (messageReceivedLock) {
            regionalQuoteListenersOfSymbols.remove(symbol);
        }

        sendLevel1SystemCommand(Level1SystemCommand.REGOFF, symbol);
    }

    /**
     * Enables or disables streaming {@link NewsHeadline}s. This sends a {@link Level1SystemCommand#NEWSON} or {@link
     * Level1SystemCommand#NEWSOFF} request.
     *
     * @param toggle true to enable, false to disable
     *
     * @throws IOException thrown for {@link IOException}s
     */
    public void enableNews(boolean toggle) throws IOException {
        sendLevel1SystemCommand(toggle ? Level1SystemCommand.NEWSON : Level1SystemCommand.NEWSOFF);
    }

    /**
     * Requests {@link FeedStatistics} be sent. This sends a {@link Level1SystemCommand#REQUEST_STATS} request.
     *
     * @return the {@link SingleMessageFuture} of the {@link FeedStatistics}
     *
     * @throws IOException thrown for {@link IOException}s
     */
    public SingleMessageFuture<FeedStatistics> requestFeedStatistics() throws IOException {
        synchronized (messageReceivedLock) {
            if (feedStatisticsFuture != null) {
                return feedStatisticsFuture;
            }

            feedStatisticsFuture = new SingleMessageFuture<>();
        }

        sendLevel1SystemCommand(Level1SystemCommand.REQUEST_STATS);

        return feedStatisticsFuture;
    }

    /**
     * Request a list of all available {@link SummaryUpdateField}s for fundamental messages. This sends a {@link
     * Level1SystemCommand#REQUEST_FUNDAMENTAL_FIELDNAMES} request.
     *
     * @return the {@link SingleMessageFuture} of the {@link String} field names
     *
     * @throws IOException thrown for {@link IOException}s
     */
    public SingleMessageFuture<List<String>> requestFundamentalFieldNames() throws IOException {
        synchronized (messageReceivedLock) {
            if (fundamentalFieldNamesFuture != null) {
                return fundamentalFieldNamesFuture;
            }

            fundamentalFieldNamesFuture = new SingleMessageFuture<>();
        }

        sendLevel1SystemCommand(Level1SystemCommand.REQUEST_FUNDAMENTAL_FIELDNAMES);

        return fundamentalFieldNamesFuture;
    }

    /**
     * Request a list of all available {@link SummaryUpdateField}s for summary/update messages for the currently set
     * IQFeed protocol. This sends a {@link Level1SystemCommand#REQUEST_ALL_UPDATE_FIELDNAMES} request.
     *
     * @return the {@link SingleMessageFuture} of the {@link SummaryUpdateField}s
     *
     * @throws IOException thrown for {@link IOException}s
     */
    public SingleMessageFuture<List<SummaryUpdateField>> requestAllUpdateFieldNames() throws IOException {
        synchronized (messageReceivedLock) {
            if (allUpdateFieldNamesFuture != null) {
                return allUpdateFieldNamesFuture;
            }

            allUpdateFieldNamesFuture = new SingleMessageFuture<>();
        }

        sendLevel1SystemCommand(Level1SystemCommand.REQUEST_ALL_UPDATE_FIELDNAMES);

        return allUpdateFieldNamesFuture;
    }

    /**
     * Request a list of all available {@link SummaryUpdateField}s for summary/update messages for this connection. This
     * sends a {@link Level1SystemCommand#REQUEST_CURRENT_UPDATE_FIELDNAMES} request.
     *
     * @return the {@link SingleMessageFuture} of the {@link SummaryUpdateField}s
     *
     * @throws IOException thrown for {@link IOException}s
     */
    public SingleMessageFuture<List<SummaryUpdateField>> requestCurrentUpdateFieldNames() throws IOException {
        synchronized (messageReceivedLock) {
            if (currentUpdateFieldNamesFuture != null) {
                return currentUpdateFieldNamesFuture;
            }

            currentUpdateFieldNamesFuture = new SingleMessageFuture<>();
        }

        sendLevel1SystemCommand(Level1SystemCommand.REQUEST_CURRENT_UPDATE_FIELDNAMES);

        return currentUpdateFieldNamesFuture;
    }

    /**
     * Change your fieldset for this connection. This fieldset applies to all {@link SummaryUpdate} messages you receive
     * on this connection. This sends a {@link Level1SystemCommand#SELECT_UPDATE_FIELDS} request.
     * <br>
     * NOTE: The {@link SummaryUpdateField#SYMBOL} is not selectable and will always be the first field of a {@link
     * SummaryUpdate}.
     *
     * @param summaryUpdateFields the {@link SummaryUpdateField}s
     *
     * @return the {@link SingleMessageFuture} of the {@link SummaryUpdateField}s for summary/update messages for this
     * connection
     *
     * @throws IOException thrown for {@link IOException}s
     */
    public SingleMessageFuture<List<SummaryUpdateField>> selectUpdateFieldNames(
            SummaryUpdateField... summaryUpdateFields) throws IOException {
        synchronized (messageReceivedLock) {
            if (currentUpdateFieldNamesFuture == null) {
                currentUpdateFieldNamesFuture = new SingleMessageFuture<>();
            }
        }

        sendLevel1SystemCommand(Level1SystemCommand.SELECT_UPDATE_FIELDS,
                Arrays.stream(summaryUpdateFields).map(SummaryUpdateField::value).distinct().toArray(String[]::new));

        return currentUpdateFieldNamesFuture;
    }

    /**
     * Change the logging levels for IQFeed. This sends a {@link Level1SystemCommand#SET_LOG_LEVELS} request.
     *
     * @param logLevels the {@link LogLevel}s or <code>null</code> for no logging
     *
     * @throws IOException thrown for {@link IOException}s
     */
    public SingleMessageFuture<List<LogLevel>> setLogLevels(LogLevel... logLevels) throws IOException {
        synchronized (messageReceivedLock) {
            if (logLevelsFuture != null) {
                return logLevelsFuture;
            }

            logLevelsFuture = new SingleMessageFuture<>();
        }

        sendLevel1SystemCommand(Level1SystemCommand.SET_LOG_LEVELS,
                Arrays.stream(logLevels).map(LogLevel::value).distinct().toArray(String[]::new));

        return logLevelsFuture;
    }

    /**
     * Request a list of all symbols currently watched on this connection. This sends a {@link
     * Level1SystemCommand#REQUEST_WATCHES} request.
     *
     * @return the {@link SingleMessageFuture} of the {@link String} symbols
     *
     * @throws IOException thrown for {@link IOException}s
     */
    public SingleMessageFuture<List<String>> requestWatchedSymbols() throws IOException {
        synchronized (messageReceivedLock) {
            if (currentUpdateFieldNamesFuture != null) {
                return watchedSymbolsFuture;
            }

            watchedSymbolsFuture = new SingleMessageFuture<>();
        }

        sendLevel1SystemCommand(Level1SystemCommand.REQUEST_WATCHES);

        return watchedSymbolsFuture;
    }

    /**
     * Unwatch all currently watched symbols. This sends a {@link Level1SystemCommand#UNWATCH_ALL} request.
     *
     * @throws IOException thrown for {@link IOException}s
     */
    public void requestUnwatchAll() throws IOException {
        synchronized (messageReceivedLock) {
            fundamentalDataListenersOfSymbols.clear();
            summaryUpdateListenersOfSymbols.clear();
            regionalQuoteListenersOfSymbols.clear();
        }

        sendLevel1SystemCommand(Level1SystemCommand.UNWATCH_ALL);
    }

    /**
     * Tells IQFeed to initiate a connection to the Level 1 server. This happens automatically upon launching the feed
     * unless the ProductID and/or Product version have not been set. This message is ignored if the feed is already
     * connected. This sends a {@link Level1SystemCommand#CONNECT} request.
     *
     * @throws IOException thrown for {@link IOException}s
     */
    public void connect() throws IOException {
        sendLevel1SystemCommand(Level1SystemCommand.CONNECT);
    }

    /**
     * Tells IQFeed to disconnect from the Level 1 server. This happens automatically as soon as the last client
     * connection to IQConnect is terminated and the ClientsConnected value in the S,STATS message returns to zero
     * (after having incremented above zero). This message is ignored if the feed is already disconnected. This sends a
     * {@link Level1SystemCommand#DISCONNECT} request.
     * <br>
     * NOTE: This will terminate all Level 1 updates for ALL apps connected to IQConnect on this Computer and should
     * only be used if you are certain no other applications are receiving data.
     *
     * @throws IOException thrown for {@link IOException}s
     */
    public void disconnect() throws IOException {
        sendLevel1SystemCommand(Level1SystemCommand.DISCONNECT);
    }

    //
    // END Feed commands
    //

    /**
     * Gets {@link #level1FeedEventListener}.
     *
     * @return the {@link Level1FeedEventListener}
     */
    public Level1FeedEventListener getLevel1FeedEventListener() {
        return level1FeedEventListener;
    }

    /**
     * Sets {@link #level1FeedEventListener}.
     *
     * @param level1FeedEventListener the {@link Level1FeedEventListener}
     */
    public void setLevel1FeedEventListener(Level1FeedEventListener level1FeedEventListener) {
        synchronized (messageReceivedLock) {
            this.level1FeedEventListener = level1FeedEventListener;
        }
    }

    /**
     * Gets {@link #newsHeadlineListener}.
     *
     * @return the {@link FeedMessageListener} of {@link NewsHeadline}s
     */
    public FeedMessageListener<NewsHeadline> getNewsHeadlineListener() {
        return newsHeadlineListener;
    }

    /**
     * Sets {@link #newsHeadlineListener}.
     *
     * @param newsHeadlineListener the {@link FeedMessageListener} of {@link NewsHeadline}s
     */
    public void setNewsHeadlineListener(FeedMessageListener<NewsHeadline> newsHeadlineListener) {
        synchronized (messageReceivedLock) {
            this.newsHeadlineListener = newsHeadlineListener;
        }
    }

    /**
     * Gets {@link #latestTimestamp}.
     *
     * @return the latest {@link LocalDateTime} timestamp
     */
    public LocalDateTime getLatestTimestamp() {
        return latestTimestamp;
    }

    /**
     * Gets {@link #customerInformation}.
     *
     * @return the {@link CustomerInformation}
     */
    public CustomerInformation getCustomerInformation() {
        return customerInformation;
    }

    /**
     * Gets {@link #latestFeedStatistics}.
     *
     * @return the latest {@link FeedStatistics}
     */
    public FeedStatistics getLatestFeedStatistics() {
        return latestFeedStatistics;
    }
}
