package net.jacobpeterson.iqfeed4j.util.csv.mapper;

import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * {@link CSVMapping} holds a mapping for a CSV value to POJO field mapping.
 *
 * @param <T> the type of the POJO
 * @param <P> the type of the POJO field
 */
public final class CSVMapping<T, P> {

    private final BiConsumer<T, String> mappingConsumer;

    /**
     * Instantiates a new {@link CSVMapping}.
     *
     * @param fieldSetter            the field setter {@link BiConsumer}. The first argument is the POJO instance and
     *                               the second argument is the converted field to be set in the instance.
     * @param stringToFieldConverter the string to field converter {@link Function}. The return type is the converted
     *                               POJO field instance and the argument is the CSV value {@link String}.
     */
    public CSVMapping(BiConsumer<T, P> fieldSetter, Function<String, P> stringToFieldConverter) {
        mappingConsumer = (instance, value) -> fieldSetter.accept(instance, stringToFieldConverter.apply(value));
    }

    /**
     * Instantiates a new {@link CSVMapping}.
     *
     * @param csvValueConsumer a {@link BiConsumer} that will takes a POJO instance as the first argument, and a CSV
     *                         {@link String} value as the second argument. This is so that fields inside the POJO can
     *                         be directly set according to the passed in CSV {@link String} value.
     */
    public CSVMapping(BiConsumer<T, String> csvValueConsumer) {
        mappingConsumer = csvValueConsumer;
    }

    /**
     * Applies the mapping. Note this could throw a variety of {@link Exception}s.
     *
     * @param instance the POJO instance
     * @param value    the CSV value
     */
    public void apply(T instance, String value) {
        mappingConsumer.accept(instance, value);
    }
}
