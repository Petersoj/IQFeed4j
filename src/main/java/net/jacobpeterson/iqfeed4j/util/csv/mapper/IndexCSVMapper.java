package net.jacobpeterson.iqfeed4j.util.csv.mapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static net.jacobpeterson.iqfeed4j.util.csv.CSVUtil.valueExists;

/**
 * {@inheritDoc}
 * <br>
 * {@link IndexCSVMapper} mappings are based off of predefined CSV indices.
 */
public class IndexCSVMapper<T> extends CSVMapper<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(IndexCSVMapper.class);

    protected final HashMap<Integer, MappingFunctions<?>> mappingFunctionsOfCSVIndices;

    /**
     * Instantiates a new {@link IndexCSVMapper}.
     *
     * @param pojoInstantiator a {@link Callable} to instantiate a new POJO
     */
    public IndexCSVMapper(Callable<T> pojoInstantiator) {
        super(pojoInstantiator);

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
        mappingFunctionsOfCSVIndices.put(csvIndex, new MappingFunctions<P>(fieldSetter, stringToFieldConverter));
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
     * {@inheritDoc}
     * <br>
     * Note this will map all mappings added via {@link #setMapping(int, BiConsumer, Function)}.
     */
    public T map(String[] csv, int offset) throws Exception {
        T instance = pojoInstantiator.call();

        // Loop through all added 'MappingFunctions' and apply them
        for (int csvIndex : mappingFunctionsOfCSVIndices.keySet()) {
            if (!valueExists(csv, csvIndex + offset)) {
                LOGGER.debug("Mapping at index {} with added offset {} doesn't exist in the given CSV: {}",
                        csvIndex, offset, csv);
                continue;
            }

            // apply() could throw a variety of exceptions
            mappingFunctionsOfCSVIndices.get(csvIndex).apply(instance, csv[csvIndex + offset]);
        }

        return instance;
    }
}
