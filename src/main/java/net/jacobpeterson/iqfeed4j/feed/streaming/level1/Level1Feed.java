package net.jacobpeterson.iqfeed4j.feed.streaming.level1;

import net.jacobpeterson.iqfeed4j.feed.message.FeedMessageListener;
import net.jacobpeterson.iqfeed4j.feed.message.SingleMessageFuture;
import net.jacobpeterson.iqfeed4j.feed.streaming.AbstractServerConnectionFeed;
import net.jacobpeterson.iqfeed4j.model.feed.common.enums.FeedCommand;
import net.jacobpeterson.iqfeed4j.model.feed.common.enums.FeedMessageType;
import net.jacobpeterson.iqfeed4j.model.feed.streaming.admin.enums.AdminSystemCommand;
import net.jacobpeterson.iqfeed4j.model.feed.streaming.level1.CustomerInformation;
import net.jacobpeterson.iqfeed4j.model.feed.streaming.level1.CustomerInformation.ServiceType;
import net.jacobpeterson.iqfeed4j.model.feed.streaming.level1.FundamentalData;
import net.jacobpeterson.iqfeed4j.model.feed.streaming.level1.FundamentalData.OptionsMultipleDeliverables;
import net.jacobpeterson.iqfeed4j.model.feed.streaming.level1.NewsHeadline;
import net.jacobpeterson.iqfeed4j.model.feed.streaming.level1.RegionalQuote;
import net.jacobpeterson.iqfeed4j.model.feed.streaming.level1.SummaryUpdate;
import net.jacobpeterson.iqfeed4j.model.feed.streaming.level1.SummaryUpdate.MarketOpen;
import net.jacobpeterson.iqfeed4j.model.feed.streaming.level1.SummaryUpdate.MostRecentTradeAggressor;
import net.jacobpeterson.iqfeed4j.model.feed.streaming.level1.SummaryUpdate.RestrictedCode;
import net.jacobpeterson.iqfeed4j.model.feed.streaming.level1.Timestamp;
import net.jacobpeterson.iqfeed4j.model.feed.streaming.level1.enums.Level1Command;
import net.jacobpeterson.iqfeed4j.model.feed.streaming.level1.enums.Level1MessageType;
import net.jacobpeterson.iqfeed4j.model.feed.streaming.level1.enums.Level1SystemCommand;
import net.jacobpeterson.iqfeed4j.model.feed.streaming.level1.enums.Level1SystemMessageType;
import net.jacobpeterson.iqfeed4j.model.feed.streaming.level1.enums.SummaryUpdateContent;
import net.jacobpeterson.iqfeed4j.model.feed.streaming.level1.enums.SummaryUpdateField;
import net.jacobpeterson.iqfeed4j.util.csv.mapper.CSVMapping;
import net.jacobpeterson.iqfeed4j.util.csv.mapper.IndexCSVMapper;
import net.jacobpeterson.iqfeed4j.util.string.LineEnding;
import net.jacobpeterson.iqfeed4j.util.tradecondition.TradeConditionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.StringJoiner;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static net.jacobpeterson.iqfeed4j.feed.streaming.level1.Level1Feed.CSVPOJOPopulators.splitFactorAndDate;
import static net.jacobpeterson.iqfeed4j.model.feed.streaming.level1.enums.SummaryUpdateField.ASK;
import static net.jacobpeterson.iqfeed4j.model.feed.streaming.level1.enums.SummaryUpdateField.ASK_CHANGE;
import static net.jacobpeterson.iqfeed4j.model.feed.streaming.level1.enums.SummaryUpdateField.ASK_MARKET_CENTER;
import static net.jacobpeterson.iqfeed4j.model.feed.streaming.level1.enums.SummaryUpdateField.ASK_SIZE;
import static net.jacobpeterson.iqfeed4j.model.feed.streaming.level1.enums.SummaryUpdateField.ASK_TIME;
import static net.jacobpeterson.iqfeed4j.model.feed.streaming.level1.enums.SummaryUpdateField.AVAILABLE_REGIONS;
import static net.jacobpeterson.iqfeed4j.model.feed.streaming.level1.enums.SummaryUpdateField.AVERAGE_MATURITY;
import static net.jacobpeterson.iqfeed4j.model.feed.streaming.level1.enums.SummaryUpdateField.BID;
import static net.jacobpeterson.iqfeed4j.model.feed.streaming.level1.enums.SummaryUpdateField.BID_CHANGE;
import static net.jacobpeterson.iqfeed4j.model.feed.streaming.level1.enums.SummaryUpdateField.BID_MARKET_CENTER;
import static net.jacobpeterson.iqfeed4j.model.feed.streaming.level1.enums.SummaryUpdateField.BID_SIZE;
import static net.jacobpeterson.iqfeed4j.model.feed.streaming.level1.enums.SummaryUpdateField.BID_TIME;
import static net.jacobpeterson.iqfeed4j.model.feed.streaming.level1.enums.SummaryUpdateField.CHANGE;
import static net.jacobpeterson.iqfeed4j.model.feed.streaming.level1.enums.SummaryUpdateField.CHANGE_FROM_OPEN;
import static net.jacobpeterson.iqfeed4j.model.feed.streaming.level1.enums.SummaryUpdateField.CLOSE;
import static net.jacobpeterson.iqfeed4j.model.feed.streaming.level1.enums.SummaryUpdateField.CLOSE_RANGE_1;
import static net.jacobpeterson.iqfeed4j.model.feed.streaming.level1.enums.SummaryUpdateField.CLOSE_RANGE_2;
import static net.jacobpeterson.iqfeed4j.model.feed.streaming.level1.enums.SummaryUpdateField.DAYS_TO_EXPIRATION;
import static net.jacobpeterson.iqfeed4j.model.feed.streaming.level1.enums.SummaryUpdateField.DECIMAL_PRECISION;
import static net.jacobpeterson.iqfeed4j.model.feed.streaming.level1.enums.SummaryUpdateField.DELAY;
import static net.jacobpeterson.iqfeed4j.model.feed.streaming.level1.enums.SummaryUpdateField.EXCHANGE_ID;
import static net.jacobpeterson.iqfeed4j.model.feed.streaming.level1.enums.SummaryUpdateField.EXTENDED_TRADE;
import static net.jacobpeterson.iqfeed4j.model.feed.streaming.level1.enums.SummaryUpdateField.EXTENDED_TRADE_DATE;
import static net.jacobpeterson.iqfeed4j.model.feed.streaming.level1.enums.SummaryUpdateField.EXTENDED_TRADE_MARKET_CENTER;
import static net.jacobpeterson.iqfeed4j.model.feed.streaming.level1.enums.SummaryUpdateField.EXTENDED_TRADE_SIZE;
import static net.jacobpeterson.iqfeed4j.model.feed.streaming.level1.enums.SummaryUpdateField.EXTENDED_TRADE_TIME;
import static net.jacobpeterson.iqfeed4j.model.feed.streaming.level1.enums.SummaryUpdateField.EXTENDED_TRADING_CHANGE;
import static net.jacobpeterson.iqfeed4j.model.feed.streaming.level1.enums.SummaryUpdateField.EXTENDED_TRADING_DIFFERENCE;
import static net.jacobpeterson.iqfeed4j.model.feed.streaming.level1.enums.SummaryUpdateField.FINANCIAL_STATUS_INDICATOR;
import static net.jacobpeterson.iqfeed4j.model.feed.streaming.level1.enums.SummaryUpdateField.FRACTION_DISPLAY_CODE;
import static net.jacobpeterson.iqfeed4j.model.feed.streaming.level1.enums.SummaryUpdateField.HIGH;
import static net.jacobpeterson.iqfeed4j.model.feed.streaming.level1.enums.SummaryUpdateField.LAST;
import static net.jacobpeterson.iqfeed4j.model.feed.streaming.level1.enums.SummaryUpdateField.LAST_DATE;
import static net.jacobpeterson.iqfeed4j.model.feed.streaming.level1.enums.SummaryUpdateField.LAST_MARKET_CENTER;
import static net.jacobpeterson.iqfeed4j.model.feed.streaming.level1.enums.SummaryUpdateField.LAST_SIZE;
import static net.jacobpeterson.iqfeed4j.model.feed.streaming.level1.enums.SummaryUpdateField.LAST_TIME;
import static net.jacobpeterson.iqfeed4j.model.feed.streaming.level1.enums.SummaryUpdateField.LOW;
import static net.jacobpeterson.iqfeed4j.model.feed.streaming.level1.enums.SummaryUpdateField.MARKET_CAPITALIZATION;
import static net.jacobpeterson.iqfeed4j.model.feed.streaming.level1.enums.SummaryUpdateField.MARKET_OPEN;
import static net.jacobpeterson.iqfeed4j.model.feed.streaming.level1.enums.SummaryUpdateField.MESSAGE_CONTENTS;
import static net.jacobpeterson.iqfeed4j.model.feed.streaming.level1.enums.SummaryUpdateField.MOST_RECENT_TRADE;
import static net.jacobpeterson.iqfeed4j.model.feed.streaming.level1.enums.SummaryUpdateField.MOST_RECENT_TRADE_AGGRESSOR;
import static net.jacobpeterson.iqfeed4j.model.feed.streaming.level1.enums.SummaryUpdateField.MOST_RECENT_TRADE_CONDITIONS;
import static net.jacobpeterson.iqfeed4j.model.feed.streaming.level1.enums.SummaryUpdateField.MOST_RECENT_TRADE_DATE;
import static net.jacobpeterson.iqfeed4j.model.feed.streaming.level1.enums.SummaryUpdateField.MOST_RECENT_TRADE_DAY_CODE;
import static net.jacobpeterson.iqfeed4j.model.feed.streaming.level1.enums.SummaryUpdateField.MOST_RECENT_TRADE_MARKET_CENTER;
import static net.jacobpeterson.iqfeed4j.model.feed.streaming.level1.enums.SummaryUpdateField.MOST_RECENT_TRADE_SIZE;
import static net.jacobpeterson.iqfeed4j.model.feed.streaming.level1.enums.SummaryUpdateField.MOST_RECENT_TRADE_TIME;
import static net.jacobpeterson.iqfeed4j.model.feed.streaming.level1.enums.SummaryUpdateField.NET_ASSET_VALUE;
import static net.jacobpeterson.iqfeed4j.model.feed.streaming.level1.enums.SummaryUpdateField.NUMBER_OF_TRADES_TODAY;
import static net.jacobpeterson.iqfeed4j.model.feed.streaming.level1.enums.SummaryUpdateField.OPEN;
import static net.jacobpeterson.iqfeed4j.model.feed.streaming.level1.enums.SummaryUpdateField.OPEN_INTEREST;
import static net.jacobpeterson.iqfeed4j.model.feed.streaming.level1.enums.SummaryUpdateField.OPEN_RANGE_1;
import static net.jacobpeterson.iqfeed4j.model.feed.streaming.level1.enums.SummaryUpdateField.OPEN_RANGE_2;
import static net.jacobpeterson.iqfeed4j.model.feed.streaming.level1.enums.SummaryUpdateField.PERCENT_CHANGE;
import static net.jacobpeterson.iqfeed4j.model.feed.streaming.level1.enums.SummaryUpdateField.PERCENT_OFF_AVERAGE_VOLUME;
import static net.jacobpeterson.iqfeed4j.model.feed.streaming.level1.enums.SummaryUpdateField.PREVIOUS_DAY_VOLUME;
import static net.jacobpeterson.iqfeed4j.model.feed.streaming.level1.enums.SummaryUpdateField.PRICE_EARNINGS_RATIO;
import static net.jacobpeterson.iqfeed4j.model.feed.streaming.level1.enums.SummaryUpdateField.RANGE;
import static net.jacobpeterson.iqfeed4j.model.feed.streaming.level1.enums.SummaryUpdateField.RESTRICTED_CODE;
import static net.jacobpeterson.iqfeed4j.model.feed.streaming.level1.enums.SummaryUpdateField.SETTLE;
import static net.jacobpeterson.iqfeed4j.model.feed.streaming.level1.enums.SummaryUpdateField.SETTLEMENT_DATE;
import static net.jacobpeterson.iqfeed4j.model.feed.streaming.level1.enums.SummaryUpdateField.SEVEN_DAY_YIELD;
import static net.jacobpeterson.iqfeed4j.model.feed.streaming.level1.enums.SummaryUpdateField.SPREAD;
import static net.jacobpeterson.iqfeed4j.model.feed.streaming.level1.enums.SummaryUpdateField.SYMBOL;
import static net.jacobpeterson.iqfeed4j.model.feed.streaming.level1.enums.SummaryUpdateField.TICK;
import static net.jacobpeterson.iqfeed4j.model.feed.streaming.level1.enums.SummaryUpdateField.TICK_ID;
import static net.jacobpeterson.iqfeed4j.model.feed.streaming.level1.enums.SummaryUpdateField.TOTAL_VOLUME;
import static net.jacobpeterson.iqfeed4j.model.feed.streaming.level1.enums.SummaryUpdateField.VOLATILITY;
import static net.jacobpeterson.iqfeed4j.model.feed.streaming.level1.enums.SummaryUpdateField.VWAP;
import static net.jacobpeterson.iqfeed4j.util.csv.CSVUtil.valueEquals;
import static net.jacobpeterson.iqfeed4j.util.csv.CSVUtil.valuePresent;
import static net.jacobpeterson.iqfeed4j.util.csv.mapper.CSVMapper.DateTimeConverters.COLON_TIME;
import static net.jacobpeterson.iqfeed4j.util.csv.mapper.CSVMapper.DateTimeConverters.DATE;
import static net.jacobpeterson.iqfeed4j.util.csv.mapper.CSVMapper.DateTimeConverters.DATE_SPACE_TIME;
import static net.jacobpeterson.iqfeed4j.util.csv.mapper.CSVMapper.DateTimeConverters.SLASHED_DATE;
import static net.jacobpeterson.iqfeed4j.util.csv.mapper.CSVMapper.PrimitiveConvertors.DOUBLE;
import static net.jacobpeterson.iqfeed4j.util.csv.mapper.CSVMapper.PrimitiveConvertors.INTEGER;
import static net.jacobpeterson.iqfeed4j.util.csv.mapper.CSVMapper.PrimitiveConvertors.STRING;

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
    protected static final HashMap<SummaryUpdateField,
            CSVMapping<SummaryUpdate, ?>> CSV_MAPPINGS_OF_SUMMARY_UPDATE_FIELDS;
    protected static final IndexCSVMapper<FundamentalData> FUNDAMENTAL_DATA_CSV_MAPPER;
    protected static final IndexCSVMapper<RegionalQuote> REGIONAL_QUOTE_CSV_MAPPER;
    protected static final IndexCSVMapper<NewsHeadline> NEWS_HEADLINE_CSV_MAPPER;
    protected static final IndexCSVMapper<CustomerInformation> CUSTOMER_INFORMATION_CSV_MAPPER;

    static {
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
                (value) -> Arrays.asList(value.split(" ")));
        CUSTOMER_INFORMATION_CSV_MAPPER.addMapping(CustomerInformation::setMaxSymbols, INTEGER);
        CUSTOMER_INFORMATION_CSV_MAPPER.addMapping(CustomerInformation::setFlags, STRING);
        CUSTOMER_INFORMATION_CSV_MAPPER.addMapping(CustomerInformation::setAccountExpirationDate, DATE);

    }

    protected final Object messageReceivedLock;
    protected final HashMap<String, FeedMessageListener<FundamentalData>> fundamentalDataListenersOfSymbols;
    protected final HashMap<String, FeedMessageListener<SummaryUpdate>> summaryUpdateListenersOfSymbols;
    protected final HashMap<String, FeedMessageListener<RegionalQuote>> regionalQuoteListenersOfSymbols;
    protected final HashMap<String, FeedMessageListener<NewsHeadline>> newsHeadlineListenersOfSymbols;
    protected final HashMap<String, FeedMessageListener<CustomerInformation>> customerInformationListenersOfSymbols;

    protected IndexCSVMapper<SummaryUpdate> summaryUpdateCSVMapper;

    protected SingleMessageFuture<Void> serverReconnectFailedFuture;
    protected SingleMessageFuture<List<String>> ipAddressesFuture;
    protected List<String> ipAddresses;
    protected SingleMessageFuture<Timestamp> timestampFuture;
    protected Timestamp latestTimestamp;

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
        newsHeadlineListenersOfSymbols = new HashMap<>();
        customerInformationListenersOfSymbols = new HashMap<>();
    }

    @Override
    protected void onMessageReceived(String[] csv) {
        super.onMessageReceived(csv);

        // Confirm message format
        if (!valuePresent(csv, 0)) {
            LOGGER.error("Received unknown message format: {}", (Object) csv);
            return;
        }

        if (valueEquals(csv, 0, FeedMessageType.ERROR.value())) {
            LOGGER.error("Received error message! {}", (Object) csv);
            return;
        }

        // TODO

        if (valueEquals(csv, 0, FeedMessageType.SYSTEM.value())) {
            if (!valuePresent(csv, 1)) {
                LOGGER.error("Received unknown System message: {}", (Object) csv);
                return;
            }

            try {
                Level1SystemMessageType systemMessageType = Level1SystemMessageType.fromValue(csv[1]);

                switch (systemMessageType) {
                    case KEY:
                    case KEYOK:
                        LOGGER.debug("Received unused KEY message: {}", (Object) csv);
                        break;
                    case SERVER_RECONNECT_FAILED:
                        break;
                    case SYMBOL_LIMIT_REACHED:
                        break;
                    case IP:
                        break;
                    case CUST:
                        break;
                    case STATS:
                        break;
                    case FUNDAMENTAL_FIELDNAMES:
                        break;
                    case UPDATE_FIELDNAMES:
                        break;
                    case CURRENT_UPDATE_FIELDNAMES:
                        break;
                    case CURRENT_LOG_LEVELS:
                        break;
                    case WATCHES:
                        break;
                    default:
                        LOGGER.error("Unhandled message type: {}", systemMessageType);
                }
            } catch (Exception exception) {
                LOGGER.error("Received unknown message type: {}", csv[1], exception);
            }
        } else {
            try {
                Level1MessageType messageType = Level1MessageType.fromValue(csv[0]);
            } catch (Exception exception) {
                LOGGER.error("Received unknown message type: {}", csv[1], exception);
            }
        }
    }

    //
    // START Feed commands
    //

    /**
     * Begins watching a symbol for Level 1 updates.
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

        String requestString = requestBuilder.toString();
        LOGGER.debug("Sending request: {}", requestString);
        // If symbol is already being watched, nothing happens
        sendMessage(requestString);
    }

    /**
     * Begins a trades only watch on a symbol for Level 1 updates.
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

        String requestString = requestBuilder.toString();
        LOGGER.debug("Sending request: {}", requestString);
        sendMessage(requestString);
    }

    /**
     * Terminates Level 1 updates for the symbol specified (including regionals).
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

        String requestString = requestBuilder.toString();
        LOGGER.debug("Sending request: {}", requestString);
        sendMessage(requestString);
    }

    /**
     * Forces a refresh for {@link FundamentalData} and a summary {@link SummaryUpdate} message from the server for the
     * symbol specified.
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

        String requestString = requestBuilder.toString();
        LOGGER.debug("Sending request: {}", requestString);
        sendMessage(requestString);
    }

    /**
     * Requests a {@link Timestamp} be sent.
     *
     * @return the {@link SingleMessageFuture} of the {@link Timestamp}
     *
     * @throws IOException thrown for {@link IOException}s
     */
    public SingleMessageFuture<Timestamp> requestTimestamp() throws IOException {
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

        String requestString = requestBuilder.toString();
        LOGGER.debug("Sending request: {}", requestString);
        sendMessage(requestString);

        return timestampFuture;
    }

    /**
     * Sends a {@link FeedCommand#SYSTEM} {@link Level1SystemCommand}.
     *
     * @param level1SystemCommand the {@link AdminSystemCommand}
     * @param arguments           the arguments. Null for no arguments.
     *
     * @throws IOException thrown for {@link IOException}s
     */
    private void sendLevel1SystemCommand(Level1SystemCommand level1SystemCommand, String... arguments)
            throws IOException {
        StringJoiner joiner = new StringJoiner(",", "", LineEnding.CR_LF.getASCIIString());
        joiner.add(FeedCommand.SYSTEM.value());
        joiner.add(level1SystemCommand.value());
        if (arguments != null && arguments.length != 0) {
            for (String argument : arguments) {
                joiner.add(argument);
            }
        }

        String command = joiner.toString();
        LOGGER.debug("Sending Level 1 command message: {}", command);
        sendMessage(command);
    }

    /**
     * Enables or disables the once-per-second {@link Timestamp}s.
     *
     * @param toggle true to enable, false to disable
     *
     * @throws IOException thrown for {@link IOException}s
     */
    public void enableTimestamps(boolean toggle) throws IOException {
        sendLevel1SystemCommand(toggle ? Level1SystemCommand.TIMESTAMPSON : Level1SystemCommand.TIMESTAMPSOFF);
    }

    // TODO

    //
    // END Feed commands
    //
}
