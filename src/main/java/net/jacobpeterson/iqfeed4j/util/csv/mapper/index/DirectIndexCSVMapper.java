package net.jacobpeterson.iqfeed4j.util.csv.mapper.index;

import java.util.function.Function;

import static net.jacobpeterson.iqfeed4j.util.csv.CSVUtil.valueNotWhitespace;

/**
 * {@inheritDoc}
 * <br>
 * {@link DirectIndexCSVMapper} mappings are based off of predefined CSV indices, but without any POJO instantiation and
 * rather uses direct type conversion.
 */
public class DirectIndexCSVMapper<T> extends AbstractIndexCSVMapper<T> {

    protected final int csvIndex;
    protected final Function<String, T> stringToTypeConverter;

    /**
     * Instantiates a new {@link DirectIndexCSVMapper}.
     *
     * @param csvIndex              the CSV index
     * @param stringToTypeConverter a {@link Function} that will takes a CSV value {@link String} as the argument, and
     *                              returns the converted CSV value type.
     */
    public DirectIndexCSVMapper(int csvIndex, Function<String, T> stringToTypeConverter) {
        super(null);

        this.csvIndex = csvIndex;
        this.stringToTypeConverter = stringToTypeConverter;
    }

    /**
     * {@inheritDoc}
     * <br>
     * Note: this will map to a type using the {@link #stringToTypeConverter}.
     */
    @Override
    public T map(String[] csv, int offset) throws Exception {
        if (!valueNotWhitespace(csv, csvIndex + offset)) { // Don't map empty CSV values
            return null;
        }

        // apply() could throw a variety of exceptions
        try {
            return stringToTypeConverter.apply(csv[csvIndex + offset]);
        } catch (Exception exception) {
            throw new Exception("Error mapping at index " + csvIndex + " with offset " + offset, exception);
        }
    }
}
