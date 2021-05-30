package net.jacobpeterson.iqfeed4j.util.csv.mapper;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;

import static net.jacobpeterson.iqfeed4j.util.csv.CSVUtil.valueNotWhitespace;

/**
 * {@inheritDoc}
 * <br>
 * {@link ListCSVMapper} maps a CSV list to a POJO {@link List}.
 */
public class ListCSVMapper<T> extends CSVMapper<T> {

    protected final Callable<? extends List<T>> listInstantiator;
    protected final BiConsumer<T, String> csvValueConsumer;
    protected final Pattern skipPattern;

    /**
     * Instantiates a new {@link ListCSVMapper}.
     *
     * @param listInstantiator a {@link Callable} to instantiate a new {@link List}
     * @param pojoInstantiator a {@link Callable} to instantiate a new POJO
     * @param csvValueConsumer a {@link BiConsumer} that will takes a new POJO instance as the first argument, and the
     *                         CSV list {@link String} value as the second argument. This is so that fields inside the
     *                         POJO can be set according to the passed in CSV list {@link String} value.
     * @param skipPattern      when a CSV value matches this given {@link Pattern} in {@link #mapToList(String[], int)},
     *                         then it is skipped (null to not check)
     */
    public ListCSVMapper(Callable<? extends List<T>> listInstantiator, Callable<T> pojoInstantiator,
            BiConsumer<T, String> csvValueConsumer, Pattern skipPattern) {
        super(pojoInstantiator);

        this.listInstantiator = listInstantiator;
        this.csvValueConsumer = csvValueConsumer;
        this.skipPattern = skipPattern;
    }

    /**
     * Use {@link #mapToList(String[], int)}.
     */
    @Override
    public T map(String[] csv, int offset) throws Exception {
        throw new UnsupportedOperationException();
    }

    /**
     * Maps the given 'csv' list to a {@link List} of POJOs with the {@link #csvValueConsumer} applied.
     *
     * @param csv    the CSV
     * @param offset offset to add to CSV indices when applying {@link CSVMapping}
     *
     * @return a new {@link List} of mapped POJOs
     *
     * @throws Exception thrown for a variety of {@link Exception}s
     */
    public List<T> mapToList(String[] csv, int offset) throws Exception {
        List<T> mappedList = listInstantiator.call();

        for (int csvIndex = offset; csvIndex < csv.length; csvIndex++) {
            if (!valueNotWhitespace(csv, csvIndex)) { // Add null for empty CSV values
                mappedList.add(null);
                continue;
            }

            // Skip over matched 'skipPattern's
            if (skipPattern != null && skipPattern.matcher(csv[csvIndex]).matches()) {
                continue;
            }

            T instance = pojoInstantiator.call();
            // accept() could throw a variety of exceptions
            try {
                csvValueConsumer.accept(instance, csv[csvIndex]);
                mappedList.add(instance);
            } catch (Exception exception) {
                throw new Exception("Error mapping at index " + (csvIndex - offset) + " with offset " + offset,
                        exception);
            }
        }

        return mappedList;
    }
}
