package net.jacobpeterson.iqfeed4j.util.csv.mapper;

import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static net.jacobpeterson.iqfeed4j.util.csv.CSVUtil.valueExists;
import static net.jacobpeterson.iqfeed4j.util.csv.CSVUtil.valueNotWhitespace;

/**
 * {@inheritDoc}
 * <br>
 * {@link TrailingCSVMapper} mappings are based off of predefined CSV indices, but with the ability to add a CSV mapping
 * that accumulates all CSV values after a certain index into one mapping.
 */
public class TrailingCSVMapper<T> extends CSVMapper<T> {

    protected final HashMap<Integer, MappingFunction<?>> mappingFunctionsOfCSVIndices;
    protected int trailingCSVIndex;
    protected MappingFunction<?> trailingMappingFunction;

    /**
     * Instantiates a new {@link TrailingCSVMapper}.
     *
     * @param pojoInstantiator a {@link Callable} to instantiate a new POJO
     */
    public TrailingCSVMapper(Callable<T> pojoInstantiator) {
        super(pojoInstantiator);

        mappingFunctionsOfCSVIndices = new HashMap<>();
    }

    /**
     * Adds a CSV index to POJO field mapping as the CSV index being the largest {@link #setMapping(int, BiConsumer,
     * Function)} CSV index + 1.
     *
     * @param <P>                    the type of the POJO field
     * @param fieldSetter            see {@link CSVMapper.MappingFunction} constructor doc
     * @param stringToFieldConverter see {@link CSVMapper.MappingFunction} constructor doc
     */
    public <P> void addMapping(BiConsumer<T, P> fieldSetter, Function<String, P> stringToFieldConverter) {
        int nextCSVIndex = mappingFunctionsOfCSVIndices.keySet().stream().max(Integer::compareTo).orElse(-1) + 1;
        setMapping(nextCSVIndex, fieldSetter, stringToFieldConverter);
    }

    /**
     * Sets a CSV index to POJO field mapping.
     *
     * @param <P>                    the type of the POJO field
     * @param csvIndex               the CSV index
     * @param fieldSetter            see {@link CSVMapper.MappingFunction} constructor doc
     * @param stringToFieldConverter see {@link CSVMapper.MappingFunction} constructor doc
     */
    @SuppressWarnings("OptionalGetWithoutIsPresent") // Optional will always be present here
    public <P> void setMapping(int csvIndex, BiConsumer<T, P> fieldSetter, Function<String, P> stringToFieldConverter) {
        mappingFunctionsOfCSVIndices.put(csvIndex, new MappingFunction<P>(fieldSetter, stringToFieldConverter));
        trailingCSVIndex = mappingFunctionsOfCSVIndices.keySet().stream().max(Integer::compareTo).get() + 1;
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
     * Sets the trailing CSV mapping that accumulates/maps all CSV values after the being the largest {@link
     * #setMapping(int, BiConsumer, Function)} CSV index + 1.
     *
     * @param <P>                    the type of the POJO field
     * @param fieldSetter            see {@link CSVMapper.MappingFunction} constructor doc
     * @param stringToFieldConverter see {@link CSVMapper.MappingFunction} constructor doc
     */
    public <P> void setTrailingMapping(BiConsumer<T, P> fieldSetter, Function<String, P> stringToFieldConverter) {
        this.trailingMappingFunction = new MappingFunction<P>(fieldSetter, stringToFieldConverter);
    }

    /**
     * {@inheritDoc}
     * <br>
     * Note this will map with the mappings added via {@link #setMapping(int, BiConsumer, Function)} and {@link
     * #setTrailingMapping(BiConsumer, Function)}.
     */
    @Override
    public T map(String[] csv, int offset) throws Exception {
        T instance = pojoInstantiator.call();

        // Loop through all added 'MappingFunctions' and apply them
        for (int csvIndex : mappingFunctionsOfCSVIndices.keySet()) {
            if (!valueNotWhitespace(csv, csvIndex + offset)) { // Don't map empty CSV values
                continue;
            }

            // apply() could throw a variety of exceptions
            try {
                mappingFunctionsOfCSVIndices.get(csvIndex).apply(instance, csv[csvIndex + offset]);
            } catch (Exception exception) {
                throw new Exception("Error mapping at index " + csvIndex + " with offset " + offset, exception);
            }
        }

        // Now apply 'trailingMappingFunction' on trailing CSV values
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
                    trailingMappingFunction.apply(instance, trailingCSVString);
                } catch (Exception exception) {
                    throw new Exception("Error mapping trailing CSV values at index " +
                            trailingCSVIndex + " with offset " + offset, exception);
                }
            }
        }

        return instance;
    }
}
