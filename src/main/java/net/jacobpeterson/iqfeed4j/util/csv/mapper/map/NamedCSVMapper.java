package net.jacobpeterson.iqfeed4j.util.csv.mapper.map;

import net.jacobpeterson.iqfeed4j.util.csv.mapper.AbstractCSVMapper;
import net.jacobpeterson.iqfeed4j.util.csv.mapper.CSVMapping;
import net.jacobpeterson.iqfeed4j.util.csv.mapper.CSVMappingException;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static net.jacobpeterson.iqfeed4j.util.csv.CSVUtil.valueNotWhitespace;

/**
 * {@inheritDoc}
 * <br>
 * {@link NamedCSVMapper} mappings are based off of named CSV indices.
 */
public class NamedCSVMapper<T> extends AbstractCSVMapper<T> {

    private final HashMap<String, CSVMapping<T, ?>> csvMappingsOfCSVIndexNames;

    /**
     * Instantiates a new {@link NamedCSVMapper}.
     *
     * @param pojoInstantiator a {@link Supplier} to instantiate a new POJO
     */
    public NamedCSVMapper(Supplier<T> pojoInstantiator) {
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
     * @throws CSVMappingException thrown for {@link CSVMappingException}s
     */
    public T map(String[] csv, int offset, Map<String, Integer> csvIndicesOfIndexNames) {
        T instance = pojoInstantiator.get();

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
                throw new CSVMappingException(
                        String.format("Error mapping at index %d with offset %d with index name %s",
                                csvNamedIndex, offset, csvIndexOfIndexName.getKey()), exception);
            }
        }

        return instance;
    }
}
