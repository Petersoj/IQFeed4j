package net.jacobpeterson.iqfeed4j.util.csv.mapper;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static net.jacobpeterson.iqfeed4j.util.csv.CSVUtil.valueNotWhitespace;

/**
 * {@inheritDoc}
 * <br>
 * {@link NamedCSVMapper} mappings are based off of named CSV indices.
 */
public class NamedCSVMapper<T> extends CSVMapper<T> {

    private final HashMap<String, CSVMapping<T, ?>> csvMappingsOfCSVIndexNames;

    /**
     * Instantiates a new {@link NamedCSVMapper}.
     *
     * @param pojoInstantiator a {@link Callable} to instantiate a new POJO
     */
    public NamedCSVMapper(Callable<T> pojoInstantiator) {
        super(pojoInstantiator);

        csvMappingsOfCSVIndexNames = new HashMap<>();
    }

    /**
     * Sets a CSV index name to POJO field mapping.
     *
     * @param <P>                    the type of the POJO field
     * @param csvIndexName           the CSV index name
     * @param fieldSetter            see {@link CSVMapping} constructor doc
     * @param stringToFieldConverter see {@link CSVMapping} constructor doc
     */
    public <P> void setMapping(String csvIndexName, BiConsumer<T, P> fieldSetter,
            Function<String, P> stringToFieldConverter) {
        csvMappingsOfCSVIndexNames.put(csvIndexName, new CSVMapping<>(fieldSetter, stringToFieldConverter));
    }

    /**
     * Removes a CSV mapping, if it exists.
     *
     * @param csvIndexName the csv index name
     */
    public void removeMapping(String csvIndexName) {
        csvMappingsOfCSVIndexNames.remove(csvIndexName);
    }

    /**
     * Use {@link #map(String[], int, Map)}.
     */
    @Override
    public T map(String[] csv, int offset) throws Exception {
        throw new UnsupportedOperationException();
    }

    /**
     * Maps the given CSV to a POJO.
     *
     * @param csv                    the CSV
     * @param offset                 offset to add to CSV indices when applying {@link CSVMapping}
     * @param csvIndicesOfIndexNames a {@link Map} with they key being the 'csvIndexName's that were added via {@link
     *                               #setMapping(String, BiConsumer, Function)} and the values being which CSV indices
     *                               they correspond to in the given 'csv'.
     *
     * @return a new POJO
     *
     * @throws Exception thrown for a variety of {@link Exception}s
     */
    public T map(String[] csv, int offset, Map<String, Integer> csvIndicesOfIndexNames) throws Exception {
        T instance = pojoInstantiator.call();

        // Loop through all added 'CSVMapping's and apply them with the given 'csvIndicesOfIndexNames' map
        for (Map.Entry<String, Integer> csvIndexOfIndexName : csvIndicesOfIndexNames.entrySet()) {
            CSVMapping<T, ?> csvMapping = csvMappingsOfCSVIndexNames.get(csvIndexOfIndexName.getKey());

            if (csvMapping == null) {
                throw new IllegalArgumentException("The CSV index name " + csvIndexOfIndexName.getKey() +
                        " does not have CSVMapping! Please report this!");
            }

            int csvNamedIndex = csvIndexOfIndexName.getValue();
            if (!valueNotWhitespace(csv, csvNamedIndex + offset)) { // Don't map empty CSV values
                continue;
            }
            try {
                csvMapping.apply(instance, csv[csvNamedIndex + offset]);
            } catch (Exception exception) {
                throw new Exception("Error mapping at index " + csvNamedIndex + " with offset " + offset +
                        " with index name " + csvIndexOfIndexName.getKey(), exception);
            }
        }

        return instance;
    }
}
