package net.jacobpeterson.iqfeed4j.feed.lookup.optionschains;

import com.google.common.base.Splitter;
import net.jacobpeterson.iqfeed4j.feed.exception.IQFeedRuntimeException;
import net.jacobpeterson.iqfeed4j.feed.exception.NoDataException;
import net.jacobpeterson.iqfeed4j.feed.exception.SyntaxException;
import net.jacobpeterson.iqfeed4j.feed.lookup.AbstractLookupFeed;
import net.jacobpeterson.iqfeed4j.feed.message.SingleMessageFuture;
import net.jacobpeterson.iqfeed4j.model.feed.lookup.optionchains.FutureContract;
import net.jacobpeterson.iqfeed4j.model.feed.lookup.optionchains.FutureSpread;
import net.jacobpeterson.iqfeed4j.model.feed.lookup.optionchains.OptionContract;
import net.jacobpeterson.iqfeed4j.model.feed.lookup.optionchains.enums.*;
import net.jacobpeterson.iqfeed4j.util.chars.CharUtil;
import net.jacobpeterson.iqfeed4j.util.csv.mapper.list.ListCSVMapper;
import net.jacobpeterson.iqfeed4j.util.string.LineEnding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDate;
import java.time.Month;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
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
            final int monthCodeCharIndex = CharUtil.lastIndexOfNonNumber(csvValue, true, false);
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
    protected static final String FEED_NAME_SUFFIX = " Option Chains";
    protected static final Pattern COLON_REGEX = Pattern.compile(Pattern.quote(":"));
    protected static final ListCSVMapper<FutureContract> FUTURE_CONTRACT_CSV_MAPPER;
    protected static final ListCSVMapper<FutureSpread> FUTURE_SPREAD_CSV_MAPPER;
    protected static final ListCSVMapper<OptionContract> FUTURE_OPTION_CSV_MAPPER;
    protected static final ListCSVMapper<OptionContract> EQUITY_OPTION_CSV_MAPPER;

    static {
        FUTURE_CONTRACT_CSV_MAPPER = new ListCSVMapper<>(() -> new ArrayList<>(30),
                FutureContract::new, CSVPOJOPopulators::futureContract, COLON_REGEX);
        FUTURE_SPREAD_CSV_MAPPER = new ListCSVMapper<>(() -> new ArrayList<>(30),
                FutureSpread::new, CSVPOJOPopulators::futureSpread, COLON_REGEX);
        FUTURE_OPTION_CSV_MAPPER = new ListCSVMapper<>(() -> new ArrayList<>(30),
                OptionContract::new, CSVPOJOPopulators::futureOptionContract, COLON_REGEX);
        EQUITY_OPTION_CSV_MAPPER = new ListCSVMapper<>(() -> new ArrayList<>(30),
                OptionContract::new, CSVPOJOPopulators::equityOptionContract, COLON_REGEX);
    }

    protected final Object messageReceivedLock;
    protected final HashMap<String, SingleMessageFuture<List<FutureContract>>> futureContractListFuturesOfRequestIDs;
    protected final HashMap<String, SingleMessageFuture<List<FutureSpread>>> futureSpreadListFuturesOfRequestIDs;
    protected final HashMap<String, SingleMessageFuture<List<OptionContract>>> futureOptionListFuturesOfRequestIDs;
    protected final HashMap<String, SingleMessageFuture<List<OptionContract>>> equityOptionListFuturesOfRequestIDs;

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
        futureSpreadListFuturesOfRequestIDs = new HashMap<>();
        futureOptionListFuturesOfRequestIDs = new HashMap<>();
        equityOptionListFuturesOfRequestIDs = new HashMap<>();
    }

    @Override
    protected void onMessageReceived(String[] csv) {
        if (isErrorOrInvalidMessage(csv)) {
            return;
        }

        String requestID = csv[0];

        synchronized (messageReceivedLock) {
            if (handleRequestIDSingleMessageList(csv, requestID, futureContractListFuturesOfRequestIDs,
                    FUTURE_CONTRACT_CSV_MAPPER)) {
                return;
            }

            if (handleRequestIDSingleMessageList(csv, requestID, futureSpreadListFuturesOfRequestIDs,
                    FUTURE_SPREAD_CSV_MAPPER)) {
                return;
            }

            if (handleRequestIDSingleMessageList(csv, requestID, futureOptionListFuturesOfRequestIDs,
                    FUTURE_OPTION_CSV_MAPPER)) {
                return;
            }

            if (handleRequestIDSingleMessageList(csv, requestID, equityOptionListFuturesOfRequestIDs,
                    EQUITY_OPTION_CSV_MAPPER)) {
                return;
            }
        }
    }

    /**
     * Handles a message for a {@link SingleMessageFuture} {@link List} by: checking for request error messages, and
     * performing {@link ListCSVMapper#mapToList(String[], int)} on the 'CSV' to complete the {@link
     * SingleMessageFuture}.
     *
     * @param <T>                   the type of {@link SingleMessageFuture} {@link List}
     * @param csv                   the CSV
     * @param requestID             the Request ID
     * @param listenersOfRequestIDs the {@link Map} with the keys being the Request IDs and the values being the
     *                              corresponding {@link SingleMessageFuture}'s {@link List}
     * @param listCSVMapper         the {@link ListCSVMapper} for the message
     *
     * @return true if the 'requestID' was a key inside 'listenersOfRequestIDs', false otherwise
     */
    protected <T> boolean handleRequestIDSingleMessageList(String[] csv, String requestID,
            HashMap<String, SingleMessageFuture<List<T>>> listenersOfRequestIDs, ListCSVMapper<T> listCSVMapper) {
        SingleMessageFuture<List<T>> future = listenersOfRequestIDs.get(requestID);

        if (future == null) {
            return false;
        }

        if (requestIDFeedHelper.isRequestErrorMessage(csv, requestID)) {
            if (requestIDFeedHelper.isRequestNoDataError(csv)) {
                future.completeExceptionally(new NoDataException());
            } else if (requestIDFeedHelper.isRequestSyntaxError(csv)) {
                future.completeExceptionally(new SyntaxException());
            } else {
                future.completeExceptionally(new IQFeedRuntimeException(
                        valuePresent(csv, 2) ?
                                String.join(",", Arrays.copyOfRange(csv, 2, csv.length)) :
                                "Error message not present."));
            }
        } else if (requestIDFeedHelper.isRequestEndOfMessage(csv, requestID)) {
            listenersOfRequestIDs.remove(requestID);
            requestIDFeedHelper.removeRequestID(requestID);
        } else {
            try {
                List<T> message = listCSVMapper.mapToList(csv, 2);
                future.complete(message);
            } catch (Exception exception) {
                future.completeExceptionally(exception);
            }
        }

        return true;
    }

    //
    // START Feed commands
    //

    /**
     * Gets a {@link FutureContract} chain. This sends a {@link OptionChainsCommand#FUTURE_CHAIN} request.
     *
     * @param symbol     the symbol. Max Length 30 characters.
     * @param months     the {@link FutureMonth}s of the chain
     * @param years      the years of the chain
     * @param nearMonths the number of near contracts to display: values 0 through 4
     *
     * @return a {@link SingleMessageFuture} of {@link FutureContract}s
     *
     * @throws IOException thrown for {@link IOException}s
     */
    public SingleMessageFuture<List<FutureContract>> getFutureChain(String symbol, List<FutureMonth> months,
            List<Integer> years, Integer nearMonths) throws IOException {
        checkNotNull(symbol);
        checkArgument(months == null || !months.isEmpty());
        checkNotNull(years);
        checkArgument(years.size() > 0);

        String requestID = requestIDFeedHelper.getNewRequestID();
        StringBuilder requestBuilder = new StringBuilder();

        requestBuilder.append(OptionChainsCommand.FUTURE_CHAIN.value()).append(",");
        requestBuilder.append(symbol).append(",");

        if (months != null) {
            requestBuilder.append(months.stream().map(FutureMonth::value).collect(Collectors.joining()));
        }
        requestBuilder.append(",");

        // The years argument the last digit of all the years without delimiters.
        // e.g.: 2005 - 2014 would be "5678901234"
        requestBuilder.append(years.stream()
                .map(Object::toString) // Convert ints to strings
                .map(s -> s.substring(s.length() - 1)) // Get last digit of year string
                .distinct() // Remove duplicates
                .collect(Collectors.joining())) // Collect digits back-to-back
                .append(",");

        if (nearMonths != null) {
            requestBuilder.append(nearMonths);
        }
        requestBuilder.append(",");

        requestBuilder.append(requestID);
        requestBuilder.append(LineEnding.CR_LF.getASCIIString());

        SingleMessageFuture<List<FutureContract>> future = new SingleMessageFuture<>();
        synchronized (messageReceivedLock) {
            futureContractListFuturesOfRequestIDs.put(requestID, future);
        }

        sendAndLogMessage(requestBuilder.toString());

        return future;
    }

    /**
     * Gets a {@link FutureSpread} chain. This sends a {@link OptionChainsCommand#FUTURE_SPREAD_CHAIN} request.
     *
     * @param symbol     the symbol. Max Length 30 characters.
     * @param months     the {@link FutureMonth}s of the chain
     * @param years      the years of the chain
     * @param nearMonths the number of near contracts to display: values 0 through 4
     *
     * @return a {@link SingleMessageFuture} of {@link FutureSpread}s
     *
     * @throws IOException thrown for {@link IOException}s
     */
    public SingleMessageFuture<List<FutureSpread>> getFutureSpreadChain(String symbol, List<FutureMonth> months,
            List<Integer> years, Integer nearMonths) throws IOException {
        checkNotNull(symbol);
        checkArgument(months == null || !months.isEmpty());
        checkNotNull(years);
        checkArgument(years.size() > 0);

        String requestID = requestIDFeedHelper.getNewRequestID();
        StringBuilder requestBuilder = new StringBuilder();

        requestBuilder.append(OptionChainsCommand.FUTURE_SPREAD_CHAIN.value()).append(",");
        requestBuilder.append(symbol).append(",");

        if (months != null) {
            requestBuilder.append(months.stream().map(FutureMonth::value).collect(Collectors.joining()));
        }
        requestBuilder.append(",");

        // The years argument the last digit of all the years without delimiters.
        // e.g.: 2005 - 2014 would be "5678901234"
        requestBuilder.append(years.stream()
                .map(Object::toString) // Convert ints to strings
                .map(s -> s.substring(s.length() - 1)) // Get last digit of year string
                .distinct() // Remove duplicates
                .collect(Collectors.joining())) // Collect digits back-to-back
                .append(",");

        if (nearMonths != null) {
            requestBuilder.append(nearMonths);
        }
        requestBuilder.append(",");

        requestBuilder.append(requestID);
        requestBuilder.append(LineEnding.CR_LF.getASCIIString());

        SingleMessageFuture<List<FutureSpread>> future = new SingleMessageFuture<>();
        synchronized (messageReceivedLock) {
            futureSpreadListFuturesOfRequestIDs.put(requestID, future);
        }

        sendAndLogMessage(requestBuilder.toString());

        return future;
    }

    /**
     * Gets a Future {@link OptionContract} chain. This sends a {@link OptionChainsCommand#FUTURE_OPTION_CHAIN}
     * request.
     *
     * @param symbol          the symbol. Max Length 30 characters.
     * @param putsCallsOption the {@link PutsCallsOption}
     * @param months          the {@link FutureMonth}s of the chain
     * @param years           the years of the chain
     * @param nearMonths      the number of near contracts to display: values 0 through 4
     *
     * @return a {@link SingleMessageFuture} of {@link OptionContract}s
     *
     * @throws IOException thrown for {@link IOException}s
     */
    public SingleMessageFuture<List<OptionContract>> getFutureOptionChain(String symbol,
            PutsCallsOption putsCallsOption, List<FutureMonth> months, List<Integer> years, Integer nearMonths)
            throws IOException {
        checkNotNull(symbol);
        checkNotNull(putsCallsOption);
        checkArgument(months == null || !months.isEmpty());
        checkNotNull(years);
        checkArgument(years.size() > 0);

        String requestID = requestIDFeedHelper.getNewRequestID();
        StringBuilder requestBuilder = new StringBuilder();

        requestBuilder.append(OptionChainsCommand.FUTURE_OPTION_CHAIN.value()).append(",");
        requestBuilder.append(symbol).append(",");
        requestBuilder.append(putsCallsOption.value()).append(",");

        if (months != null) {
            requestBuilder.append(months.stream().map(FutureMonth::value).collect(Collectors.joining()));
        }
        requestBuilder.append(",");

        // The years argument the last digit of all the years without delimiters.
        // e.g.: 2005 - 2014 would be "5678901234"
        requestBuilder.append(years.stream()
                .map(Object::toString) // Convert ints to strings
                .map(s -> s.substring(s.length() - 1)) // Get last digit of year string
                .distinct() // Remove duplicates
                .collect(Collectors.joining())) // Collect digits back-to-back
                .append(",");

        if (nearMonths != null) {
            requestBuilder.append(nearMonths);
        }
        requestBuilder.append(",");

        requestBuilder.append(requestID);
        requestBuilder.append(LineEnding.CR_LF.getASCIIString());

        SingleMessageFuture<List<OptionContract>> future = new SingleMessageFuture<>();
        synchronized (messageReceivedLock) {
            futureOptionListFuturesOfRequestIDs.put(requestID, future);
        }

        sendAndLogMessage(requestBuilder.toString());

        return future;
    }

    /**
     * Gets an Equity {@link OptionContract} chain. This sends a {@link OptionChainsCommand#EQUITY_OPTION_CHAIN}
     * request.
     *
     * @param symbol                 the symbol. Max Length 30 characters.
     * @param putsCallsOption        the {@link PutsCallsOption}
     * @param months                 the {@link EquityOptionMonth}s of the chain
     * @param nearMonths             the number of near contracts to display: values 0 through 4
     * @param nonStandardOptionTypes {@link NonStandardOptionTypes}
     *
     * @return a {@link SingleMessageFuture} of {@link OptionContract}s
     *
     * @throws IOException thrown for {@link IOException}s
     */
    public SingleMessageFuture<List<OptionContract>> getEquityOptionChain(String symbol,
            PutsCallsOption putsCallsOption, List<EquityOptionMonth> months, Integer nearMonths,
            NonStandardOptionTypes nonStandardOptionTypes) throws IOException {
        return getEquityOptionChainWithFilter(symbol, putsCallsOption, months, nearMonths, OptionFilterType.NONE, null,
                null, nonStandardOptionTypes);
    }

    /**
     * Gets an Equity {@link OptionContract} chain using a Strike price filter. This sends a {@link
     * OptionChainsCommand#EQUITY_OPTION_CHAIN} request.
     *
     * @param symbol                 the symbol. Max Length 30 characters.
     * @param putsCallsOption        the {@link PutsCallsOption}
     * @param months                 the {@link EquityOptionMonth}s of the chain
     * @param nearMonths             the number of near contracts to display: values 0 through 4
     * @param beginningStrikePrice   the beginning strike price
     * @param endingStrikePrice      the ending strike price
     * @param nonStandardOptionTypes {@link NonStandardOptionTypes}
     *
     * @return a {@link SingleMessageFuture} of {@link OptionContract}s
     *
     * @throws IOException thrown for {@link IOException}s
     */
    public SingleMessageFuture<List<OptionContract>> getEquityOptionChainWithStrikeFilter(String symbol,
            PutsCallsOption putsCallsOption, List<EquityOptionMonth> months, Integer nearMonths,
            Double beginningStrikePrice, Double endingStrikePrice, NonStandardOptionTypes nonStandardOptionTypes)
            throws IOException {
        return getEquityOptionChainWithFilter(symbol, putsCallsOption, months, nearMonths,
                OptionFilterType.STRIKE_RANGE, beginningStrikePrice.toString(), endingStrikePrice.toString(),
                nonStandardOptionTypes);
    }

    /**
     * Gets an Equity {@link OptionContract} chain using the number of contracts In-The-Money or Out-Of-The-Money as a
     * filter. This sends a {@link OptionChainsCommand#EQUITY_OPTION_CHAIN} request.
     *
     * @param symbol                 the symbol. Max Length 30 characters.
     * @param putsCallsOption        the {@link PutsCallsOption}
     * @param months                 the {@link EquityOptionMonth}s of the chain
     * @param nearMonths             the number of near contracts to display: values 0 through 4
     * @param itmCount               the number of contracts In-The-Money
     * @param otmCount               the number of contracts Out-Of-The-Money
     * @param nonStandardOptionTypes {@link NonStandardOptionTypes}
     *
     * @return a {@link SingleMessageFuture} of {@link OptionContract}s
     *
     * @throws IOException thrown for {@link IOException}s
     */
    public SingleMessageFuture<List<OptionContract>> getEquityOptionChainWithITMOTMFilter(String symbol,
            PutsCallsOption putsCallsOption, List<EquityOptionMonth> months, Integer nearMonths, Integer itmCount,
            Integer otmCount, NonStandardOptionTypes nonStandardOptionTypes) throws IOException {
        return getEquityOptionChainWithFilter(symbol, putsCallsOption, months, nearMonths,
                OptionFilterType.IN_OR_OUT_OF_THE_MONEY, itmCount.toString(), otmCount.toString(),
                nonStandardOptionTypes);
    }

    /**
     * Gets an Equity {@link OptionContract} chain. This sends a {@link OptionChainsCommand#EQUITY_OPTION_CHAIN}
     * request.
     *
     * @param symbol                 the symbol. Max Length 30 characters.
     * @param putsCallsOption        the {@link PutsCallsOption}
     * @param months                 the {@link EquityOptionMonth}s of the chain
     * @param nearMonths             the number of near contracts to display: values 0 through 4
     * @param optionFilterType       the {@link OptionFilterType}
     * @param filter1                ignored if [Filter Type] is "0". If [Filter Type] = "1" then beginning strike price
     *                               or if [Filter Type] = "2" then the number of contracts in the money
     * @param filter2                ignored if [Filter Type] is "0". If [Filter Type] = "1" then ending strike price or
     *                               if [Filter Type] = "2" then the number of contracts out of the money
     * @param nonStandardOptionTypes {@link NonStandardOptionTypes}
     *
     * @return a {@link SingleMessageFuture} of {@link OptionContract}s
     *
     * @throws IOException thrown for {@link IOException}s
     */
    private SingleMessageFuture<List<OptionContract>> getEquityOptionChainWithFilter(String symbol,
            PutsCallsOption putsCallsOption, List<EquityOptionMonth> months, Integer nearMonths,
            OptionFilterType optionFilterType, String filter1, String filter2,
            NonStandardOptionTypes nonStandardOptionTypes) throws IOException {
        checkNotNull(symbol);
        checkNotNull(putsCallsOption);
        checkArgument(months == null || !months.isEmpty());
        checkNotNull(optionFilterType);
        checkArgument(optionFilterType == OptionFilterType.NONE || (filter1 != null && filter2 != null));
        checkNotNull(nonStandardOptionTypes);

        String requestID = requestIDFeedHelper.getNewRequestID();
        StringBuilder requestBuilder = new StringBuilder();

        requestBuilder.append(OptionChainsCommand.EQUITY_OPTION_CHAIN.value()).append(",");
        requestBuilder.append(symbol).append(",");
        requestBuilder.append(putsCallsOption.value()).append(",");

        if (months != null) {
            requestBuilder.append(months.stream().map(EquityOptionMonth::value).collect(Collectors.joining()));
        }
        requestBuilder.append(",");

        if (nearMonths != null) {
            requestBuilder.append(nearMonths);
        }
        requestBuilder.append(",");

        requestBuilder.append(optionFilterType.value()).append(",");
        if (filter1 != null) {
            requestBuilder.append(filter1);
        }
        requestBuilder.append(",");
        if (filter2 != null) {
            requestBuilder.append(filter2);
        }
        requestBuilder.append(",");

        requestBuilder.append(requestID).append(",");
        requestBuilder.append(nonStandardOptionTypes.value());
        requestBuilder.append(LineEnding.CR_LF.getASCIIString());

        SingleMessageFuture<List<OptionContract>> future = new SingleMessageFuture<>();
        synchronized (messageReceivedLock) {
            equityOptionListFuturesOfRequestIDs.put(requestID, future);
        }

        sendAndLogMessage(requestBuilder.toString());

        return future;
    }

    //
    // END Feed commands
    //
}
