package net.jacobpeterson.iqfeed4j.util.csv.mapper.list;

import net.jacobpeterson.iqfeed4j.util.csv.mapper.AbstractCSVMapper;
import net.jacobpeterson.iqfeed4j.util.csv.mapper.CSVMapping;
import net.jacobpeterson.iqfeed4j.util.csv.mapper.CSVMappingException;

import java.util.List;
import java.util.function.Supplier;

/**
 * {@inheritDoc}
 * <br>
 * {@link AbstractListCSVMapper} maps a CSV list.
 */
public abstract class AbstractListCSVMapper<T> extends AbstractCSVMapper<T> {

    /**
     * Instantiates a new {@link AbstractListCSVMapper}.
     *
     * @param pojoInstantiator a {@link Supplier} to instantiate a new POJO
     */
    public AbstractListCSVMapper(Supplier<T> pojoInstantiator) {
        super(pojoInstantiator);
    }

    /**
     * Maps the given <code>csv</code> list to a {@link List}.
     *
     * @param csv    the CSV
     * @param offset offset to add to CSV indices when applying {@link CSVMapping}s
     *
     * @return a new {@link List} of mapped POJOs
     *
     * @throws CSVMappingException thrown for {@link CSVMappingException}s
     */
    public abstract List<T> mapToList(String[] csv, int offset);
}
