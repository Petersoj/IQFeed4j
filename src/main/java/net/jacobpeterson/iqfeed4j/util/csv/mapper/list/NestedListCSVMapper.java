package net.jacobpeterson.iqfeed4j.util.csv.mapper.list;

import net.jacobpeterson.iqfeed4j.util.csv.mapper.CSVMapping;
import net.jacobpeterson.iqfeed4j.util.csv.mapper.CSVMappingException;

import java.util.HashMap;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static net.jacobpeterson.iqfeed4j.util.csv.CSVUtil.valueNotWhitespace;

/**
 * {@inheritDoc}
 * <br>
 * {@link NestedListCSVMapper} mappings are based on a nested CSV lists inside a CSV list (e.g. a group of 3 CSV values
 * that repeated in a CSV list).
 */
public class NestedListCSVMapper<T> extends AbstractListCSVMapper<T> {

    protected final Supplier<? extends List<T>> listInstantiator;
    protected final HashMap<Integer, CSVMapping<T, ?>> csvMappingsOfCSVIndices;
    protected final int nestedListLength;

    /**
     * Instantiates a new {@link NestedListCSVMapper}.
     *
     * @param listInstantiator a {@link Supplier} to instantiate a new {@link List}
     * @param pojoInstantiator a {@link Supplier} to instantiate a new POJO
     * @param nestedListLength the nested list length
     */
    public NestedListCSVMapper(Supplier<? extends List<T>> listInstantiator, Supplier<T> pojoInstantiator,
            int nestedListLength) {
        super(pojoInstantiator);

        this.listInstantiator = listInstantiator;
        this.nestedListLength = nestedListLength;

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
    public <P> void setMapping(int csvIndex, BiConsumer<T, P> fieldSetter, Function<String, P> stringToFieldConverter) {
        csvMappingsOfCSVIndices.put(csvIndex, new CSVMapping<>(fieldSetter, stringToFieldConverter));
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
     * Note: this will map to a list of POJOs using the mappings added via {@link #setMapping(int, BiConsumer,
     * Function)}. This will iterate through the list at {@link #nestedListLength} length.
     */
    @Override
    public List<T> mapToList(String[] csv, int offset) {
        List<T> mappedList = listInstantiator.get();

        for (int csvIndex = offset; csvIndex < csv.length; csvIndex += nestedListLength) {
            T instance = pojoInstantiator.get();

            // Loop through all added 'CSVMapping's and apply them
            boolean valueWasMapped = false;
            for (int mappedCSVIndex : csvMappingsOfCSVIndices.keySet()) {
                if (!valueNotWhitespace(csv, csvIndex + mappedCSVIndex)) { // Don't map empty CSV values
                    continue;
                }

                // apply() could throw a variety of exceptions
                try {
                    csvMappingsOfCSVIndices.get(mappedCSVIndex).apply(instance, csv[csvIndex + mappedCSVIndex]);
                    valueWasMapped = true;
                } catch (Exception exception) {
                    throw new CSVMappingException(
                            String.format("Error mapping at index %d with offset %d at mapped CSV index %d",
                                    csvIndex - offset, offset, mappedCSVIndex), exception);
                }
            }

            if (valueWasMapped) {
                mappedList.add(instance);
            }
        }

        return mappedList;
    }
}
