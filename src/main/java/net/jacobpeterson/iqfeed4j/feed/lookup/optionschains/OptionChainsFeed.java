package net.jacobpeterson.iqfeed4j.feed.lookup.optionschains;

import com.google.common.base.Splitter;
import net.jacobpeterson.iqfeed4j.feed.SingleMessageFuture;
import net.jacobpeterson.iqfeed4j.feed.lookup.AbstractLookupFeed;
import net.jacobpeterson.iqfeed4j.model.feedenums.FeedMessageType;
import net.jacobpeterson.iqfeed4j.model.feedenums.lookup.optionchains.EquityOptionMonth;
import net.jacobpeterson.iqfeed4j.model.feedenums.lookup.optionchains.FutureMonth;
import net.jacobpeterson.iqfeed4j.model.feedenums.lookup.optionchains.OptionType;
import net.jacobpeterson.iqfeed4j.model.lookup.optionchains.FutureContract;
import net.jacobpeterson.iqfeed4j.model.lookup.optionchains.FutureSpread;
import net.jacobpeterson.iqfeed4j.model.lookup.optionchains.OptionContract;
import net.jacobpeterson.iqfeed4j.util.chars.CharUtil;
import net.jacobpeterson.iqfeed4j.util.csv.mapper.ListCSVMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static net.jacobpeterson.iqfeed4j.util.csv.CSVUtil.valueEquals;
import static net.jacobpeterson.iqfeed4j.util.csv.CSVUtil.valuePresent;

/**
 * {@link OptionChainsFeed} is an {@link AbstractLookupFeed} for Option chains data.
 */
public class OptionChainsFeed extends AbstractLookupFeed {

    /**
     * {@link MonthHelper} contains {@link OptionChainsFeed} {@link Month} related static functions.
     */
    public static class MonthHelper {

        /**
         * Maps {@link FutureMonth} to {@link Month}.
         *
         * @param futureMonth the {@link FutureMonth}
         *
         * @return the {@link Month}
         */
        public static Month mapFutureMonth(FutureMonth futureMonth) {
            switch (futureMonth) {
                case JANUARY:
                    return Month.JANUARY;
                case FEBRUARY:
                    return Month.FEBRUARY;
                case MARCH:
                    return Month.MARCH;
                case APRIL:
                    return Month.APRIL;
                case MAY:
                    return Month.MAY;
                case JUNE:
                    return Month.JUNE;
                case JULY:
                    return Month.JULY;
                case AUGUST:
                    return Month.AUGUST;
                case SEPTEMBER:
                    return Month.SEPTEMBER;
                case OCTOBER:
                    return Month.OCTOBER;
                case NOVEMBER:
                    return Month.NOVEMBER;
                case DECEMBER:
                    return Month.DECEMBER;
                default:
                    throw new UnsupportedOperationException();
            }
        }

        /**
         * Maps {@link EquityOptionMonth} to {@link Month}.
         *
         * @param equityOptionMonth the {@link EquityOptionMonth}
         *
         * @return the {@link Month}
         */
        public static Month mapEquityOptionMonth(EquityOptionMonth equityOptionMonth) {
            switch (equityOptionMonth) {
                case JANUARY_CALL:
                case JANUARY_PUT:
                    return Month.JANUARY;
                case FEBRUARY_CALL:
                case FEBRUARY_PUT:
                    return Month.FEBRUARY;
                case MARCH_CALL:
                case MARCH_PUT:
                    return Month.MARCH;
                case APRIL_CALL:
                case APRIL_PUT:
                    return Month.APRIL;
                case MAY_CALL:
                case MAY_PUT:
                    return Month.MAY;
                case JUNE_CALL:
                case JUNE_PUT:
                    return Month.JUNE;
                case JULY_CALL:
                case JULY_PUT:
                    return Month.JULY;
                case AUGUST_CALL:
                case AUGUST_PUT:
                    return Month.AUGUST;
                case SEPTEMBER_CALL:
                case SEPTEMBER_PUT:
                    return Month.SEPTEMBER;
                case OCTOBER_CALL:
                case OCTOBER_PUT:
                    return Month.OCTOBER;
                case NOVEMBER_CALL:
                case NOVEMBER_PUT:
                    return Month.NOVEMBER;
                case DECEMBER_CALL:
                case DECEMBER_PUT:
                    return Month.DECEMBER;
                default:
                    throw new UnsupportedOperationException();
            }
        }

        /**
         * Gets an {@link OptionType} from an {@link EquityOptionMonth}.
         *
         * @param equityOptionMonth the {@link EquityOptionMonth}
         *
         * @return the {@link OptionType}
         */
        public static OptionType optionTypeFromEquityOptionMonth(EquityOptionMonth equityOptionMonth) {
            switch (equityOptionMonth) {
                case JANUARY_CALL:
                case FEBRUARY_CALL:
                case MARCH_CALL:
                case APRIL_CALL:
                case MAY_CALL:
                case JUNE_CALL:
                case JULY_CALL:
                case AUGUST_CALL:
                case SEPTEMBER_CALL:
                case OCTOBER_CALL:
                case NOVEMBER_CALL:
                case DECEMBER_CALL:
                    return OptionType.CALL;
                case JANUARY_PUT:
                case FEBRUARY_PUT:
                case MARCH_PUT:
                case APRIL_PUT:
                case MAY_PUT:
                case JUNE_PUT:
                case JULY_PUT:
                case AUGUST_PUT:
                case SEPTEMBER_PUT:
                case OCTOBER_PUT:
                case NOVEMBER_PUT:
                case DECEMBER_PUT:
                    return OptionType.PUT;
                default:
                    throw new UnsupportedOperationException();
            }
        }
    }

    /**
     * {@link CSVPOJOPopulators} contains static functions with two arguments: the first being a POJO instance, and the
     * second being a CSV list {@link String} value.
     */
    public static class CSVPOJOPopulators {

        /**
         * Specifically for a CFU request, this populates a {@link FutureContract} from a CSV list {@link String} value.
         * Note this could throw a variety of {@link Exception}s.
         *
         * @param futureContract the {@link FutureContract} instance
         * @param csvValue       the CSV value
         */
        public static void futureContract(FutureContract futureContract, String csvValue) {
            // Format of Future contract symbol is: <symbol name><month code char><2 digit year>

            int length = csvValue.length();
            final int symbolBeingIndex = 0;
            final int symbolEndIndex = length - 3;
            final int monthCodeCharIndex = length - 3;
            final int yearBeginIndex = length - 2;
            final int yearEndIndex = length;

            futureContract.setSymbol(csvValue.substring(symbolBeingIndex, symbolEndIndex));
            futureContract.setDate(LocalDate.of(
                    // Year (future contract dates are in 21st century)
                    Integer.parseInt("20" + csvValue.substring(yearBeginIndex, yearEndIndex)),
                    MonthHelper.mapFutureMonth(
                            FutureMonth.fromValue(String.valueOf(csvValue.charAt(monthCodeCharIndex)))), // Month
                    0)); // Day
        }

        /**
         * Specifically for a CFS request, this populates a {@link FutureSpread} from a CSV list {@link String} value.
         * Note this could throw a variety of {@link Exception}s.
         *
         * @param futureSpread the {@link FutureSpread} instance
         * @param csvValue     the CSV value
         */
        @SuppressWarnings("UnstableApiUsage")
        public static void futureSpread(FutureSpread futureSpread, String csvValue) {
            // Format of Future spread is two future contract symbols separated by a '-'

            List<String> futureSpreadStrings = Splitter.on('-').splitToList(csvValue);
            if (futureSpreadStrings.size() != 2) {
                throw new IllegalArgumentException("Two Future spreads must be delimited by a '-' character!");
            }

            FutureContract from = new FutureContract();
            futureContract(from, futureSpreadStrings.get(0));
            FutureContract to = new FutureContract();
            futureContract(to, futureSpreadStrings.get(1));

            futureSpread.setFrom(from);
            futureSpread.setTo(to);
        }

        /**
         * Specifically for a CFO request, this populates a Future {@link OptionContract} from a CSV list {@link String}
         * value. Note this could throw a variety of {@link Exception}s.
         *
         * @param futureOptionContract the Future {@link OptionContract} instance
         * @param csvValue             the CSV value
         */
        public static void futureOptionContract(OptionContract futureOptionContract, String csvValue) {
            // Get 'OptionType' from symbol
            final int putOptionTypeIndex = csvValue.lastIndexOf(OptionType.PUT.value());
            final int callOptionTypeIndex = csvValue.lastIndexOf(OptionType.CALL.value());
            if (putOptionTypeIndex == -1 && callOptionTypeIndex == -1) {
                throw new IllegalArgumentException("Option contract symbol must contain a 'P' or a 'C'!");
            }
            final int optionTypeIndex = putOptionTypeIndex != -1 ? putOptionTypeIndex : callOptionTypeIndex;

            int length = csvValue.length();

            final int symbolBeingIndex = 0;
            final int symbolEndIndex = optionTypeIndex - 3;
            final int monthCodeCharIndex = optionTypeIndex - 3;
            final int yearBeginIndex = optionTypeIndex - 2;
            final int yearEndIndex = optionTypeIndex;
            final int strikePriceBeginIndex = optionTypeIndex + 1;
            final int strikePriceEndIndex = length;

            futureOptionContract.setSymbol(csvValue.substring(symbolBeingIndex, symbolEndIndex));
            futureOptionContract.setExpirationDate(LocalDate.of(
                    // Year (future option contract dates are in 21st century)
                    Integer.parseInt("20" + csvValue.substring(yearBeginIndex, yearEndIndex)),
                    MonthHelper.mapFutureMonth(
                            FutureMonth.fromValue(String.valueOf(csvValue.charAt(monthCodeCharIndex)))), // Month
                    0)); // Day
            futureOptionContract.setOptionType(OptionType.fromValue(String.valueOf(csvValue.charAt(optionTypeIndex))));
            futureOptionContract.setStrikePrice(
                    Double.parseDouble(csvValue.substring(strikePriceBeginIndex, strikePriceEndIndex)));
        }

        /**
         * Specifically for a CEO request, this populates an Equity {@link OptionContract} from a CSV list {@link
         * String} value. Note this could throw a variety of {@link Exception}s.
         *
         * @param equityOptionContract the Equity {@link OptionContract} instance
         * @param csvValue             the CSV value
         */
        public static void equityOptionContract(OptionContract equityOptionContract, String csvValue) {
            final int monthCodeCharIndex = CharUtil.lastIndexOfNonNumber(csvValue);
            if (monthCodeCharIndex == -1) {
                throw new IllegalArgumentException("Option contract symbol must contain month code!");
            }

            int length = csvValue.length();

            final int symbolBeingIndex = 0;
            final int symbolEndIndex = monthCodeCharIndex - 4;
            final int yearBeginIndex = monthCodeCharIndex - 4;
            final int yearEndIndex = monthCodeCharIndex - 2;
            final int dayBeginIndex = monthCodeCharIndex - 2;
            final int dayEndIndex = monthCodeCharIndex;
            final int strikePriceBeginIndex = monthCodeCharIndex + 1;
            final int strikePriceEndIndex = length;

            final EquityOptionMonth equityOptionMonth = EquityOptionMonth.fromValue(
                    String.valueOf(csvValue.charAt(monthCodeCharIndex)));

            equityOptionContract.setSymbol(csvValue.substring(symbolBeingIndex, symbolEndIndex));
            equityOptionContract.setExpirationDate(LocalDate.of(
                    // Year (future option contract dates are in 21st century)
                    Integer.parseInt("20" + csvValue.substring(yearBeginIndex, yearEndIndex)),
                    MonthHelper.mapEquityOptionMonth(equityOptionMonth), // Month
                    Integer.parseInt(csvValue.substring(dayBeginIndex, dayEndIndex)))); // Day
            equityOptionContract.setOptionType(MonthHelper.optionTypeFromEquityOptionMonth(equityOptionMonth));
            equityOptionContract.setStrikePrice(
                    Double.parseDouble(csvValue.substring(strikePriceBeginIndex, strikePriceEndIndex)));
        }
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(OptionChainsFeed.class);
    private static final String FEED_NAME_SUFFIX = " Option Chains";
    private static final ListCSVMapper<FutureContract> FUTURE_CONTRACT_CSV_MAPPER;
    private static final ListCSVMapper<FutureSpread> FUTURE_SPREAD_CSV_MAPPER;
    private static final ListCSVMapper<OptionContract> FUTURE_OPTION_CSV_MAPPER;
    private static final ListCSVMapper<OptionContract> EQUITY_OPTION_CSV_MAPPER;

    static {
        FUTURE_CONTRACT_CSV_MAPPER = new ListCSVMapper<>(() -> new ArrayList<>(30),
                FutureContract::new, CSVPOJOPopulators::futureContract);
        FUTURE_SPREAD_CSV_MAPPER = new ListCSVMapper<>(() -> new ArrayList<>(30),
                FutureSpread::new, CSVPOJOPopulators::futureSpread);
        FUTURE_OPTION_CSV_MAPPER = new ListCSVMapper<>(() -> new ArrayList<>(30),
                OptionContract::new, CSVPOJOPopulators::futureOptionContract);
        EQUITY_OPTION_CSV_MAPPER = new ListCSVMapper<>(() -> new ArrayList<>(30),
                OptionContract::new, CSVPOJOPopulators::equityOptionContract);
    }

    protected final Object messageReceivedLock;
    protected final HashMap<String, SingleMessageFuture<List<FutureContract>>> futureContractListFuturesOfRequestIDs;

    /**
     * Instantiates a new {@link OptionChainsFeed}.
     *
     * @param optionChainsFeedName the {@link OptionChainsFeed} name
     * @param hostname             the hostname
     * @param port                 the port
     */
    public OptionChainsFeed(String optionChainsFeedName, String hostname, int port) {
        super(optionChainsFeedName + FEED_NAME_SUFFIX, hostname, port, COMMA_DELIMITED_SPLITTER);

        messageReceivedLock = new Object();
        futureContractListFuturesOfRequestIDs = new HashMap<>();
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

    // TODO

    //
    // END Feed commands
    //
}
