package net.jacobpeterson.iqfeed4j.util.csv;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static net.jacobpeterson.iqfeed4j.util.csv.CSVUtil.valueExists;

/**
 * {@link CSVMapper} maps CSV {@link String} values to POJO fields.
 *
 * @param <T> the type of the POJO
 */
public final class CSVMapper<T> {

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
     * {@link DateTimeConverters} contains common {@link Function}s with the argument being the CSV {@link String} value
     * and the return value being the converted CSV date/time value.
     */
    public static class DateTimeConverters {

        private static final DateTimeFormatter DATE_SPACE_TIME_FORMATTER =
                DateTimeFormatter.ofPattern("yyyyMMdd HHmmss");
        public static final Function<String, LocalDateTime> DATE_SPACE_TIME =
                (value) -> LocalDateTime.parse(value, DATE_SPACE_TIME_FORMATTER);

        private static final DateTimeFormatter MONTH3_DAY_TIME_AM_PM_FORMATTER =
                new DateTimeFormatterBuilder()
                        .appendPattern("MMM dd h:mma")
                        .parseDefaulting(ChronoField.YEAR, LocalDate.now().getYear())
                        .toFormatter(Locale.ENGLISH);
        public static final Function<String, LocalDateTime> MONTH3_DAY_TIME_AM_PM =
                (value) -> LocalDateTime.parse(value, MONTH3_DAY_TIME_AM_PM_FORMATTER);
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(CSVMapper.class);

    private final Callable<T> pojoInstantiator;
    private final HashMap<Integer, MappingFunctions<?>> mappingFunctionsOfCSVIndices;

    /**
     * Instantiates a new {@link CSVMapper}.
     *
     * @param pojoInstantiator a {@link Callable} to instantiate a new POJO
     */
    public CSVMapper(Callable<T> pojoInstantiator) {
        this.pojoInstantiator = pojoInstantiator;

        mappingFunctionsOfCSVIndices = new HashMap<>();
    }

    /**
     * Adds a CSV to POJO field mapping as the CSV index being the largest {@link #setMapping(int, BiConsumer,
     * Function)} CSV index + 1.
     *
     * @param <P>                    the type of the POJO field
     * @param fieldSetter            see {@link MappingFunctions} constructor doc
     * @param stringToFieldConverter see {@link MappingFunctions} constructor doc
     */
    public <P> void addMapping(BiConsumer<T, P> fieldSetter, Function<String, P> stringToFieldConverter) {
        int nextCSVIndex = mappingFunctionsOfCSVIndices.keySet().stream().max(Integer::compareTo).orElse(-1) + 1;
        setMapping(nextCSVIndex, fieldSetter, stringToFieldConverter);
    }

    /**
     * Sets a CSV to POJO field mapping.
     *
     * @param <P>                    the type of the POJO field
     * @param csvIndex               the CSV index
     * @param fieldSetter            see {@link MappingFunctions} constructor doc
     * @param stringToFieldConverter see {@link MappingFunctions} constructor doc
     */
    public <P> void setMapping(int csvIndex, BiConsumer<T, P> fieldSetter, Function<String, P> stringToFieldConverter) {
        mappingFunctionsOfCSVIndices.put(csvIndex, new MappingFunctions<>(fieldSetter, stringToFieldConverter));
    }

    /**
     * Removes a CSV mapping, if it exists.
     *
     * @param csvIndex the CSV index
     */
    public void removeMapping(int csvIndex) {
        mappingFunctionsOfCSVIndices.remove(csvIndex);
    }

    /**
     * Maps the given CSV to a POJO, or null if unsuccessful. Note this will attempt to map all mappings added via
     * {@link #addMapping(BiConsumer, Function)}.
     *
     * @param csv    the CSV
     * @param offset offset to add to CSV indices when applying {@link MappingFunctions}
     *
     * @return the POJO, or null if unsuccessful
     */
    public T map(String[] csv, int offset) {
        try {
            T instance = pojoInstantiator.call();

            // Loop through all added 'MappingFunctions' and apply them
            for (int csvIndex : mappingFunctionsOfCSVIndices.keySet()) {
                if (!valueExists(csv, csvIndex + offset)) {
                    LOGGER.debug("Mapping at index {} with added offset {} doesn't exist in the given CSV: {}",
                            csvIndex, offset, csv);
                    continue;
                }

                try {
                    mappingFunctionsOfCSVIndices.get(csvIndex).apply(instance, csv[csvIndex + offset]);
                } catch (Exception exception) {
                    LOGGER.error("Could not map at index {} with added offset {}!", csvIndex, offset, exception);
                }
            }
            return instance;
        } catch (Exception exception) {
            return null;
        }
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
