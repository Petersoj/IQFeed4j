package net.jacobpeterson.iqfeed4j.util.csv.mapper.index;

import net.jacobpeterson.iqfeed4j.util.csv.mapper.AbstractCSVMapper;
import net.jacobpeterson.iqfeed4j.util.csv.mapper.CSVMapping;
import net.jacobpeterson.iqfeed4j.util.csv.mapper.exception.CSVMappingException;

import java.util.concurrent.Callable;

/**
 * {@inheritDoc}
 * <br>
 * {@link AbstractIndexCSVMapper} mappings are based off of predefined CSV indices.
 */
public abstract class AbstractIndexCSVMapper<T> extends AbstractCSVMapper<T> {

    /**
     * Instantiates a new {@link AbstractIndexCSVMapper}.
     *
     * @param pojoInstantiator a {@link Callable} to instantiate a new POJO
     */
    public AbstractIndexCSVMapper(Callable<T> pojoInstantiator) {
        super(pojoInstantiator);
    }

    /**
     * Maps the given CSV to a POJO.
     *
     * @param csv    the CSV
     * @param offset offset to add to CSV indices when applying {@link CSVMapping}s
     *
     * @return a new POJO
     *
     * @throws CSVMappingException thrown for {@link CSVMappingException}s
     */
    public abstract T map(String[] csv, int offset);
}
