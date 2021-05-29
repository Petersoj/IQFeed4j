package net.jacobpeterson.iqfeed4j.util.map;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * {@link MapUtil} contains utility functions for {@link Map}.
 */
public class MapUtil {

    /**
     * Gets keys by the given value for a 'many-to-one' {@link Map} relationship.
     *
     * @param <T>   the key type parameter
     * @param <E>   the value type parameter
     * @param map   the {@link Map}
     * @param value the value to search for
     *
     * @return the keys by value {@link Set} or null if none were found
     *
     * @see <a href="https://stackoverflow.com/a/2904266">Stackoverflow</a>
     */
    public static <T, E> Set<T> getKeysByValue(Map<T, E> map, E value) {
        Set<T> keys = new HashSet<>();

        for (Map.Entry<T, E> entry : map.entrySet()) {
            if (Objects.equals(value, entry.getValue())) {
                keys.add(entry.getKey());
            }
        }

        return keys.isEmpty() ? null : keys;
    }

    /**
     * Gets a key by the given value for a 'one-to-one' {@link Map} relationship.
     *
     * @param <T>   the key type parameter
     * @param <E>   the value type parameter
     * @param map   the {@link Map}
     * @param value the value to search for
     *
     * @return the key
     *
     * @see <a href="https://stackoverflow.com/a/2904266">Stackoverflow</a>
     */
    public static <T, E> T getKeyByValue(Map<T, E> map, E value) {
        for (Map.Entry<T, E> entry : map.entrySet()) {
            if (Objects.equals(value, entry.getValue())) {
                return entry.getKey();
            }
        }

        return null;
    }
}
