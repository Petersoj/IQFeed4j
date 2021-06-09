package net.jacobpeterson.iqfeed4j.util.csv.mapper.index;

import net.jacobpeterson.iqfeed4j.util.csv.mapper.CSVMapping;
import net.jacobpeterson.iqfeed4j.util.csv.mapper.exception.CSVMappingException;

import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static net.jacobpeterson.iqfeed4j.util.csv.CSVUtil.valueNotWhitespace;

/**
 * {@inheritDoc}
 */
public class IndexCSVMapper<T> extends AbstractIndexCSVMapper<T> {

    protected final HashMap<Integer, CSVMapping<T, ?>> csvMappingsOfCSVIndices;

    /**
     * Instantiates a new {@link IndexCSVMapper}.
     *
     * @param pojoInstantiator a {@link Callable} to instantiate a new POJO
     */
    public IndexCSVMapper(Callable<T> pojoInstantiator) {
        super(pojoInstantiator);

        csvMappingsOfCSVIndices = new HashMap<>();
    }

    /**
     * Adds a CSV index to POJO field mapping as the CSV index being the largest mapped CSV index + 1.
     *
     * @param <P>                    the type of the POJO field
     * @param fieldSetter            see {@link CSVMapping} constructor doc
     * @param stringToFieldConverter see {@link CSVMapping} constructor doc
     */
    public <P> void addMapping(BiConsumer<T, P> fieldSetter, Function<String, P> stringToFieldConverter) {
        setMapping(getNextCSVIndex(), fieldSetter, stringToFieldConverter);
    }

    /**
     * Adds a CSV index to POJO field mapping as the CSV index being the largest mapped CSV index + 1.
     *
     * @param csvValueConsumer see {@link CSVMapping} constructor doc
     */
    public void addMapping(BiConsumer<T, String> csvValueConsumer) {
        setMapping(getNextCSVIndex(), csvValueConsumer);
    }

    /**
     * Adds a CSV index to POJO field mapping as the CSV index being the largest mapped CSV index + 1.
     *
     * @param csvMapping the {@link CSVMapping}
     */
    public void addMapping(CSVMapping<T, ?> csvMapping) {
        setMapping(getNextCSVIndex(), csvMapping);
    }

    /**
     * Gets the largest CSV index in {@link #csvMappingsOfCSVIndices} + 1.
     *
     * @return an int
     */
    private int getNextCSVIndex() {
        return csvMappingsOfCSVIndices.keySet().stream().max(Integer::compareTo).orElse(-1) + 1;
    }

    /**
     * Sets a CSV index to POJO field mapping.
     *
     * @param <P>                    the type of the POJO field
     * @param csvIndex               the CSV index
     * @param fieldSetter            see {@link CSVMapping} constructor doc
     * @param stringToFieldConverter see {@link CSVMapping} constructor doc
     */
    public <P> void setMapping(int csvIndex, BiConsumer<T, P> fieldSetter, Function<String, P> stringToFieldConverter) {
        csvMappingsOfCSVIndices.put(csvIndex, new CSVMapping<>(fieldSetter, stringToFieldConverter));
    }

    /**
     * Sets a CSV index to POJO field mapping.
     *
     * @param <P>              the type parameter
     * @param csvIndex         the CSV index
     * @param csvValueConsumer see {@link CSVMapping} constructor doc
     */
    public <P> void setMapping(int csvIndex, BiConsumer<T, String> csvValueConsumer) {
        csvMappingsOfCSVIndices.put(csvIndex, new CSVMapping<>(csvValueConsumer));
    }

    /**
     * Sets a CSV index to POJO field mapping.
     *
     * @param csvIndex   the CSV index
     * @param csvMapping the {@link CSVMapping}
     */
    public void setMapping(int csvIndex, CSVMapping<T, ?> csvMapping) {
        csvMappingsOfCSVIndices.put(csvIndex, csvMapping);
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
     * {@inheritDoc}
     * <br>
     * Note this will map with the mappings added via {@link #setMapping(int, BiConsumer, Function)}.
     */
    @Override
    public T map(String[] csv, int offset) {
        T instance;
        try {
            instance = pojoInstantiator.call();
        } catch (Exception exception) {
            throw new CSVMappingException("Could not instantiate POJO!", exception);
        }

        // Loop through all added 'CSVMappings' and apply them
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

        return instance;
    }
}
