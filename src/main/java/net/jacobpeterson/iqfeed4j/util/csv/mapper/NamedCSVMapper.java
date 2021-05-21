package net.jacobpeterson.iqfeed4j.util.csv.mapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;

/**
 * {@inheritDoc}
 * <br>
 * {@link NamedCSVMapper} mappings are based off of named CSV indices.
 */
public class NamedCSVMapper<T> extends CSVMapper<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(NamedCSVMapper.class);

    /**
     * Instantiates a new {@link NamedCSVMapper}.
     *
     * @param pojoInstantiator a {@link Callable} to instantiate a new POJO
     */
    public NamedCSVMapper(Callable<T> pojoInstantiator) {
        super(pojoInstantiator);
    }

    @Override
    public T map(String[] csv, int offset) throws Exception {
        // TODO
        return null;
    }
}
