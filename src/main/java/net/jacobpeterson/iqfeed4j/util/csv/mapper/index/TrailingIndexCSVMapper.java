package net.jacobpeterson.iqfeed4j.util.csv.mapper.index;

import net.jacobpeterson.iqfeed4j.util.csv.mapper.CSVMapping;
import net.jacobpeterson.iqfeed4j.util.csv.mapper.exception.CSVMappingException;

import java.util.HashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static net.jacobpeterson.iqfeed4j.util.csv.CSVUtil.valueExists;
import static net.jacobpeterson.iqfeed4j.util.csv.CSVUtil.valueNotWhitespace;

/**
 * {@inheritDoc}
 * <br>
 * {@link TrailingIndexCSVMapper} mappings are based off of predefined CSV indices, but with the ability to add a CSV
 * mapping that accumulates all CSV values after a certain index into one mapping.
 */
public class TrailingIndexCSVMapper<T> extends AbstractIndexCSVMapper<T> {

    protected final HashMap<Integer, CSVMapping<T, ?>> csvMappingsOfCSVIndices;
    protected int trailingCSVIndex;
    protected CSVMapping<T, ?> trailingCSVMapping;

    /**
     * Instantiates a new {@link TrailingIndexCSVMapper}.
     *
     * @param pojoInstantiator a {@link Supplier} to instantiate a new POJO
     */
    public TrailingIndexCSVMapper(Supplier<T> pojoInstantiator) {
        super(pojoInstantiator);

        csvMappingsOfCSVIndices = new HashMap<>();
    }

    /**
     * Adds a CSV index to POJO field mapping as the CSV index being the largest {@link #setMapping(int, BiConsumer,
     * Function)} CSV index + 1.
     *
     * @param <P>                    the type of the POJO field
     * @param fieldSetter            see {@link CSVMapping} constructor doc
     * @param stringToFieldConverter see {@link CSVMapping} constructor doc
     */
    public <P> void addMapping(BiConsumer<T, P> fieldSetter, Function<String, P> stringToFieldConverter) {
        int nextCSVIndex = csvMappingsOfCSVIndices.keySet().stream().max(Integer::compareTo).orElse(-1) + 1;
        setMapping(nextCSVIndex, fieldSetter, stringToFieldConverter);
    }

    /**
     * Sets a CSV index to POJO field mapping.
     *
     * @param <P>                    the type of the POJO field
     * @param csvIndex               the CSV index
     * @param fieldSetter            see {@link CSVMapping} constructor doc
     * @param stringToFieldConverter see {@link CSVMapping} constructor doc
     */
    @SuppressWarnings("OptionalGetWithoutIsPresent") // Optional will always be present here
    public <P> void setMapping(int csvIndex, BiConsumer<T, P> fieldSetter, Function<String, P> stringToFieldConverter) {
        csvMappingsOfCSVIndices.put(csvIndex, new CSVMapping<>(fieldSetter, stringToFieldConverter));
        trailingCSVIndex = csvMappingsOfCSVIndices.keySet().stream().max(Integer::compareTo).get() + 1;
    }

    /**
     * Removes a CSV mapping, if it exists.
     *
     * @param csvIndex the CSV index
     */
    public void removeMapping(int csvIndex) {
        csvMappingsOfCSVIndices.remove(csvIndex);
    }

    /**
     * Sets the trailing CSV mapping that accumulates/maps all CSV values after the being the largest {@link
     * #setMapping(int, BiConsumer, Function)} CSV index + 1.
     *
     * @param <P>                    the type of the POJO field
     * @param fieldSetter            see {@link CSVMapping} constructor doc
     * @param stringToFieldConverter see {@link CSVMapping} constructor doc
     */
    public <P> void setTrailingMapping(BiConsumer<T, P> fieldSetter, Function<String, P> stringToFieldConverter) {
        this.trailingCSVMapping = new CSVMapping<>(fieldSetter, stringToFieldConverter);
    }

    /**
     * {@inheritDoc}
     * <br>
     * Note this will map with the mappings added via {@link #setMapping(int, BiConsumer, Function)} and {@link
     * #setTrailingMapping(BiConsumer, Function)}.
     */
    @Override
    public T map(String[] csv, int offset) {
        T instance = pojoInstantiator.get();

        // Loop through all added 'CSVMapping's and apply them
        for (int csvIndex : csvMappingsOfCSVIndices.keySet()) {
            if (!valueNotWhitespace(csv, csvIndex + offset)) { // Don't map empty CSV values
                continue;
            }

            // apply() could throw a variety of exceptions
            try {
                csvMappingsOfCSVIndices.get(csvIndex).apply(instance, csv[csvIndex + offset]);
            } catch (Exception exception) {
                throw new CSVMappingException(csvIndex, offset, exception);
            }
        }

        // Now apply 'trailingCSVMapping' on trailing CSV values
        if (valueExists(csv, trailingCSVIndex + offset)) {
            StringBuilder trailingCSVBuilder = new StringBuilder();
            for (int csvIndex = trailingCSVIndex + offset; csvIndex < csv.length; csvIndex++) {
                trailingCSVBuilder.append(csv[csvIndex]);

                if (csvIndex < csv.length - 1) { // Don't append comma on last CSV value
                    trailingCSVBuilder.append(",");
                }
            }

            String trailingCSVString = trailingCSVBuilder.toString();
            if (!trailingCSVString.isEmpty()) {
                // apply() could throw a variety of exceptions
                try {
                    trailingCSVMapping.apply(instance, trailingCSVString);
                } catch (Exception exception) {
                    throw new CSVMappingException(trailingCSVIndex, offset, exception);
                }
            }
        }

        return instance;
    }
}
