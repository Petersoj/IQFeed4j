package net.jacobpeterson.iqfeed4j.util.csv.mapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * {@link CSVMapper} maps CSV {@link String} values to POJO fields.
 *
 * @param <T> the type of the POJO
 */
public abstract class CSVMapper<T> {

    /**
     * {@link PrimitiveConvertors} contains common {@link Function}s with the argument being the CSV {@link String}
     * value and the return value being the converted CSV primitive value.
     */
    public static class PrimitiveConvertors {

        public static final Function<String, String> STRING = (value) -> value;
        public static final Function<String, Character> CHAR = (value) -> value.length() == 0 ? '\0' : value.charAt(0);
        public static final Function<String, Boolean> BOOLEAN = Boolean::valueOf;
        public static final Function<String, Byte> BYTE = Byte::valueOf;
        public static final Function<String, Short> SHORT = Short::valueOf;
        public static final Function<String, Integer> INT = Integer::valueOf;
        public static final Function<String, Long> LONG = Long::valueOf;
        public static final Function<String, Float> FLOAT = Float::valueOf;
        public static final Function<String, Double> DOUBLE = Double::valueOf;
    }

    /**
     * {@link DateTimeFormatters} contains various {@link DateTimeFormatters} for formatting/converting from/to CSV.
     */
    public static class DateTimeFormatters {

        /** Format of: <code>HHmmss</code> */
        public static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HHmmss");

        /** Format of: <code>yyyyMMdd</code> */
        public static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

        /** Format of: <code>yyyyMMdd HHmmss</code>. */
        public static final DateTimeFormatter DATE_SPACE_TIME =
                DateTimeFormatter.ofPattern("yyyyMMdd HHmmss");

        /** Format of: <code>MMM dd h:mma</code> */
        public static final DateTimeFormatter MONTH3_DAY_TIME_AM_PM =
                new DateTimeFormatterBuilder()
                        .appendPattern("MMM dd h:mma")
                        .parseDefaulting(ChronoField.YEAR, LocalDate.now().getYear())
                        .toFormatter(Locale.ENGLISH);

        /** Format of: <code>yyyy-MM-dd HH:mm:ss.nnnnnn</code>. */
        public static final DateTimeFormatter DASHED_DATE_SPACE_TIME_FRACTIONAL =
                new DateTimeFormatterBuilder()
                        .parseCaseInsensitive()
                        .append(DateTimeFormatter.ISO_LOCAL_DATE)
                        .appendLiteral(' ')
                        .append(DateTimeFormatter.ISO_LOCAL_TIME) // Optionally includes micro/nanoseconds
                        .toFormatter(Locale.ENGLISH);

        /** Format of: <code>yyyy-MM-dd</code>. */
        public static final DateTimeFormatter DASHED_DATE = DateTimeFormatter.ISO_LOCAL_DATE;
    }

    /**
     * {@link DateTimeConverters} contains common {@link Function}s with the argument being the CSV {@link String} value
     * and the return value being the converted CSV date/time value.
     */
    public static class DateTimeConverters {

        /** Convertor using {@link DateTimeFormatters#TIME} */
        public static final Function<String, LocalTime> TIME =
                (value) -> LocalTime.parse(value, DateTimeFormatters.TIME);

        /** Convertor using {@link DateTimeFormatters#DATE} */
        public static final Function<String, LocalDate> DATE =
                (value) -> LocalDate.parse(value, DateTimeFormatters.DATE);

        /** Convertor using {@link DateTimeFormatters#DATE_SPACE_TIME} */
        public static final Function<String, LocalDateTime> DATE_SPACE_TIME =
                (value) -> LocalDateTime.parse(value, DateTimeFormatters.DATE_SPACE_TIME);

        /** Convertor using {@link DateTimeFormatters#MONTH3_DAY_TIME_AM_PM} */
        public static final Function<String, LocalDateTime> MONTH3_DAY_TIME_AM_PM =
                (value) -> LocalDateTime.parse(value, DateTimeFormatters.MONTH3_DAY_TIME_AM_PM);

        /** Convertor using {@link DateTimeFormatters#DASHED_DATE_SPACE_TIME_FRACTIONAL} */
        public static final Function<String, LocalDateTime> DASHED_DATE_SPACE_TIME_FRACTIONAL =
                (value) -> LocalDateTime.parse(value, DateTimeFormatters.DASHED_DATE_SPACE_TIME_FRACTIONAL);

        /** Convertor using {@link DateTimeFormatters#DASHED_DATE_SPACE_TIME_FRACTIONAL} */
        public static final Function<String, LocalDateTime> DASHED_DATE_SPACE_TIME = DASHED_DATE_SPACE_TIME_FRACTIONAL;

        /** Convertor using {@link DateTimeFormatters#DASHED_DATE} */
        public static final Function<String, LocalDate> DASHED_DATE =
                (value) -> LocalDate.parse(value, DateTimeFormatters.DASHED_DATE);
    }

    protected final Callable<T> pojoInstantiator;

    /**
     * Instantiates a new {@link CSVMapper}.
     *
     * @param pojoInstantiator a {@link Callable} to instantiate a new POJO
     */
    public CSVMapper(Callable<T> pojoInstantiator) {
        this.pojoInstantiator = pojoInstantiator;
    }

    /**
     * {@link MappingFunctions} holds functions for CSV to field mapping.
     *
     * @param <P> the type of the POJO field
     */
    protected final class MappingFunctions<P> {

        private final BiConsumer<T, P> fieldSetter;
        private final Function<String, P> stringToFieldConverter;

        /**
         * Instantiates a new {@link MappingFunctions}.
         *
         * @param fieldSetter            the field setter {@link BiConsumer}. The first argument is the POJO instance
         *                               and the second argument is the converted field to be set in the instance.
         * @param stringToFieldConverter the string to field converter {@link Function}. The return type is the
         *                               converted POJO field instance and the argument is the CSV value {@link
         *                               String}.
         */
        public MappingFunctions(BiConsumer<T, P> fieldSetter, Function<String, P> stringToFieldConverter) {
            this.fieldSetter = fieldSetter;
            this.stringToFieldConverter = stringToFieldConverter;
        }

        /**
         * Applies the mapping functions. Note this could throw a variety of {@link Exception}s.
         *
         * @param instance the POJO instance
         * @param value    the CSV value
         */
        public void apply(T instance, String value) {
            fieldSetter.accept(instance, stringToFieldConverter.apply(value));
        }

        /**
         * Gets {@link #fieldSetter}.
         *
         * @return a {@link BiConsumer}
         */
        public BiConsumer<T, P> getFieldSetter() {
            return fieldSetter;
        }

        /**
         * Gets {@link #stringToFieldConverter}.
         *
         * @return a {@link Function}
         */
        public Function<String, P> getStringToFieldConverter() {
            return stringToFieldConverter;
        }
    }
}
