package net.jacobpeterson.iqfeed4j.util.csv.mapper.list;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Function;

import static net.jacobpeterson.iqfeed4j.util.csv.CSVUtil.valueNotWhitespace;

/**
 * {@inheritDoc}
 * <br>
 * {@link DirectListCSVMapper} maps a CSV list to a {@link List} using a direct type conversion.
 */
public class DirectListCSVMapper<T> extends AbstractListCSVMapper<T> {

    protected final Callable<? extends List<T>> listInstantiator;
    protected final Function<String, T> stringToTypeConverter;

    /**
     * Instantiates a new {@link DirectListCSVMapper}.
     *
     * @param listInstantiator      a {@link Callable} to instantiate a new {@link List}
     * @param stringToTypeConverter a {@link Function} that will takes a CSV value {@link String} as the argument, and
     *                              returns the converted CSV value type.
     */
    public DirectListCSVMapper(Callable<? extends List<T>> listInstantiator,
            Function<String, T> stringToTypeConverter) {
        super(null);

        this.listInstantiator = listInstantiator;
        this.stringToTypeConverter = stringToTypeConverter;
    }

    /**
     * {@inheritDoc}
     * <br>
     * Note: this will map to a list with the {@link #stringToTypeConverter} applied.
     */
    @Override
    public List<T> mapToList(String[] csv, int offset) throws Exception {
        List<T> mappedList = listInstantiator.call();

        for (int csvIndex = offset; csvIndex < csv.length; csvIndex++) {
            if (!valueNotWhitespace(csv, csvIndex)) { // Add null for empty CSV values
                mappedList.add(null);
                continue;
            }

            // accept() could throw a variety of exceptions
            try {
                mappedList.add(stringToTypeConverter.apply(csv[csvIndex]));
            } catch (Exception exception) {
                throw new Exception("Error mapping at index " + (csvIndex - offset) + " with offset " + offset,
                        exception);
            }
        }

        return mappedList;
    }
}
