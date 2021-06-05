package net.jacobpeterson.iqfeed4j.util.csv.mapper.list;

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
public class ListCSVMapper<T> extends AbstractListCSVMapper<T> {

    protected final Callable<? extends List<T>> listInstantiator;
    protected final BiConsumer<T, String> csvValueConsumer;
    protected final Pattern skipPattern;

    /**
     * Instantiates a new {@link ListCSVMapper}.
     *
     * @param listInstantiator a {@link Callable} to instantiate a new {@link List}
     * @param pojoInstantiator a {@link Callable} to instantiate a new POJO
     * @param csvValueConsumer a {@link BiConsumer} that takes a new POJO instance as the first argument, and the CSV
     *                         list {@link String} value as the second argument. This is so that fields inside the POJO
     *                         can be set according to the passed in CSV list {@link String} value.
     * @param skipPattern      when a CSV value matches this given {@link Pattern} in {@link #mapToList(String[], int)},
     *                         then it is skipped (<code>null</code> to not check)
     */
    public ListCSVMapper(Callable<? extends List<T>> listInstantiator, Callable<T> pojoInstantiator,
            BiConsumer<T, String> csvValueConsumer, Pattern skipPattern) {
        super(pojoInstantiator);

        this.listInstantiator = listInstantiator;
        this.csvValueConsumer = csvValueConsumer;
        this.skipPattern = skipPattern;
    }

    /**
     * {@inheritDoc}
     * <br>
     * Note: this will map to a list of POJOs with the {@link #csvValueConsumer} applied.
     */
    @Override
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
